package com.sourcegraph.langserver.langservice.gradle;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.MessageAggregator;
import com.sourcegraph.utils.LanguageUtils;
import groovyjarjarantlr.RecognitionException;
import groovyjarjarantlr.TokenStreamException;
import groovyjarjarantlr.collections.AST;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.groovy.antlr.GroovySourceAST;
import org.codehaus.groovy.antlr.SourceBuffer;
import org.codehaus.groovy.antlr.UnicodeEscapingReader;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.codehaus.groovy.antlr.parser.GroovyRecognizer;
import org.codehaus.groovy.antlr.treewalker.PreOrderTraversal;
import org.codehaus.groovy.antlr.treewalker.Visitor;
import org.codehaus.groovy.antlr.treewalker.VisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Poor man's Gradle executor. Best-effort collection of project metadata from a Gradle script hierarchy.
 * Fradle "executes" a small subset of the Groovy AST. "Execute" is used loosely here -- Fradle really just uses
 * AST-based heuristics to simulate variable assignments, expression evaluation, and subscript invocation.
 * Fradle also simulates a subset of loop structures without actually looping, so maximum execution time is O(n)
 * in the size of the script(s). Because no code is actually executed, it can run untrusted Gradle scripts.
 * <p>
 * Must be used in conjunction with PreOrderTraversal.
 * <p>
 * All Gradle scripts are run for the sake of their side effects on a shared Gradle project hierarchy. Fradle captures
 * this shared global state in a Globals datastructure, which can be examined after script execution to extract
 * project metadata.
 * <p>
 * Created by beyang on 3/7/17.
 */
public class Fradle extends VisitorAdapter implements Visitor {

    /**
     * Scope name used for top-level scopes. This includes the ancestor scope (which is the root of the scope hierarchy)
     * and scopes for sub-script execution.
     */
    public final static String TOP_LEVEL_SCOPE = "TOP";

    /**
     * runGradle runs the designated script (at @param scriptUri in @param fileProvider), using the passed in globals
     * and scope.
     */
    public static void runGradle(String scriptUri, FileContentProvider fileProvider, Fradle.Globals globals, Fradle.Scope scope, MessageAggregator msgs) throws Exception {
        InputStreamReader isReader = new InputStreamReader(fileProvider.readContent(scriptUri));
        runGradle(scriptUri, fileProvider, globals, scope, isReader, msgs);
    }

    static void runGradle(String scriptUri, FileContentProvider fileProvider, Fradle.Globals globals, Fradle.Scope scope, Reader isReader, MessageAggregator msgs) throws IOException, RecognitionException, TokenStreamException {
        SourceBuffer sourceBuffer = new SourceBuffer();
        ArrayList<String> lines = new BufferedReader(isReader).lines().collect(Collectors.toCollection(ArrayList::new));
        String fileContent = String.join("\n", lines);
        UnicodeEscapingReader unicodeReader = new UnicodeEscapingReader(new StringReader(fileContent), sourceBuffer);
        GroovyLexer lexer = new GroovyLexer(unicodeReader);
        unicodeReader.setLexer(lexer);
        GroovyRecognizer parser = GroovyRecognizer.make(lexer);
        parser.setSourceBuffer(sourceBuffer);
        parser.compilationUnit();
        AST ast = parser.getAST();

        Fradle fradle = new Fradle(scriptUri, fileProvider, lines, globals, scope, msgs);
        PreOrderTraversal traverser = new PreOrderTraversal(fradle);
        traverser.process(ast);
    }

    static Fradle newForTest(Globals globals, Scope scope) {
        return new Fradle("TEST", null, null, globals, scope, new MessageAggregator(null));
    }

    private MessageAggregator messages;

    /**
     * Parameters: The end state / output is a function of these variables.
     */
    private String scriptUri;
    private FileContentProvider fileProvider;

    /**
     * The lines of the .gradle file. Used for error reporting.
     */
    private List<String> lines;

    /**
     * Global state: The state being modified by the script. Modifying this is the purpose of running of the script and the
     * value of globals after script execution will contain Gradle project metadata.
     */
    private Globals globals;

    /**
     * Execution context state.
     */
    private Scope scope;  // provides variable namespacing
    private FQName lastFQName;  // the last fully qualified name to be visited in the AST. Used to name block scopes.
    private List<GroovySourceAST> parents;  // a list of all parents of the currently visited AST node.

    /**
     * Block names which do not modify project configuration so we can therefore ignore
     */
    private static final Set<String> IGNORE_BLOCKS = ImmutableSet.of(
            "bintray" // plugin for publishing artifacts to bintray
    );

    /**
     * List of block scopes that we should ignore. While this is non-empty, then Fradle execution should effectively be a
     * no-op.
     */
    private Stack<String> ignorableBlockStack;

    private Fradle(String scriptUri, FileContentProvider fileProvider, List<String> lines, Globals globals, Scope scope, MessageAggregator messages) {
        if (globals == null) {
            throw new IllegalArgumentException("globals == null");
        }
        if (scope == null) {
            throw new IllegalArgumentException("scope == null");
        }
        this.scriptUri = scriptUri;
        this.fileProvider = fileProvider;
        this.lines = lines;
        this.globals = globals;
        this.scope = scope;
        this.parents = Lists.newArrayList();
        this.ignorableBlockStack = new Stack<>();
        this.messages = messages;
    }

    public FQName getLastFQName() {
        return lastFQName;
    }

    public void setLastFQName(FQName lastFQName) {
        this.lastFQName = lastFQName;
    }

    /**
     * Evaluates an expression across all manifestations of the current scope.
     */
    private List<Object> evalExpr(GroovySourceAST t) throws UnsupportedExpressionSyntax, UnsupportedNameSyntax, NameNotFound {
        ArrayList<Object> val = Lists.newArrayListWithCapacity(scope.currentNumContexts());
        for (int i = 0; i < scope.currentNumContexts(); i++) {
            val.add(evalExprSingleScope(i, t));
        }
        return val;
    }

    /**
     * Evaluates an expression within the manifestation of the current scope specified by scopeIndex.
     */
    Object evalExprSingleScope(int scopeIndex, GroovySourceAST t) throws UnsupportedExpressionSyntax, UnsupportedNameSyntax, NameNotFound {
        switch (t.getType()) {
            case GroovyLexer.EXPR: {
                return evalExprSingleScope(scopeIndex, (GroovySourceAST) t.getFirstChild());
            }
            case GroovyLexer.SLIST: {
                return evalExprSingleScope(scopeIndex, (GroovySourceAST) t.getFirstChild());
            }
            case GroovyLexer.ELIST: {
                List<Object> vals = Lists.newArrayList();
                Map<Object, Object> map = new HashMap<>();
                for (AST e = t.getFirstChild(); e != null; e = e.getNextSibling()) {
                    Object o = evalExprSingleScope(scopeIndex, (GroovySourceAST) e);
                    if (o instanceof Pair) {
                        Pair nvPair = (Pair) o;
                        map.put(nvPair.getKey(), nvPair.getValue());
                    } else {
                        vals.add(o);
                    }
                }
                return map.isEmpty() ? vals : map;
            }
            case GroovyLexer.LABELED_ARG:
                Object aName = evalExprSingleScope(scopeIndex, (GroovySourceAST) t.getFirstChild());
                Object aVal = evalExprSingleScope(scopeIndex, (GroovySourceAST) t.getFirstChild().getNextSibling());
                return Pair.of(aName, aVal);
            case GroovyLexer.STRING_LITERAL: {
                return t.getText();
            }
            case GroovyLexer.STRING_CONSTRUCTOR: {
                List<String> components = Lists.newArrayList();
                for (GroovySourceAST c = (GroovySourceAST) t.getFirstChild(); c != null; c = (GroovySourceAST) c.getNextSibling()) {
                    components.add((String) evalExprSingleScope(scopeIndex, c));
                }
                return String.join("", components);
            }
            case GroovyLexer.LIST_CONSTRUCTOR:
                GroovySourceAST list = (GroovySourceAST) t.getFirstChild();
                if (list == null) {
                    break;
                }
                List<Object> evaluatedList = new ArrayList<>();
                for (AST rawItem = list.getFirstChild(); rawItem != null; rawItem = rawItem.getNextSibling()) {
                    Object evaluatedItem = evalExprSingleScope(scopeIndex, (GroovySourceAST) rawItem);
                    evaluatedList.add(evaluatedItem);
                }
                return evaluatedList;
            case GroovyLexer.MAP_CONSTRUCTOR: {
                GroovySourceAST elist = (GroovySourceAST) t.getFirstChild();
                if (elist == null) {
                    break;
                }
                Map<Object, Object> m = Maps.newHashMap();
                for (AST entry = elist.getFirstChild(); entry != null; entry = entry.getNextSibling()) {
                    if (entry.getType() != GroovyLexer.LABELED_ARG) {
                        break;
                    }
                    AST k = entry.getFirstChild();
                    AST v = k.getNextSibling();
                    Object key = evalExprSingleScope(scopeIndex, (GroovySourceAST) k);
                    Object val = evalExprSingleScope(scopeIndex, (GroovySourceAST) v);
                    m.put(key, val);
                }
                return m;
            }
            case GroovyLexer.IDENT:
            case GroovyLexer.DOT: {
                return evalName(scopeIndex, toName(t), t);
            }
            case GroovyLexer.LITERAL_new: {  // new File(...)
                if ("File".equals(t.getFirstChild().getText())) {
                    AST elist = t.getFirstChild().getNextSibling();
                    List<String> components = Lists.newArrayList();
                    for (AST expr = elist.getFirstChild(); expr != null; expr = expr.getNextSibling()) {
                        components.add((String) evalExprSingleScope(scopeIndex, (GroovySourceAST) expr));
                    }
                    return String.join("/", components);
                }
            }
            case GroovyLexer.METHOD_CALL: {
                // TODO: a more general approach for invoking Project methods might be needed in the future
                // (see list of methods here https://docs.gradle.org/current/dsl/org.gradle.api.Project.html)
                GroovySourceAST recv = (GroovySourceAST) t.getFirstChild();
                if (recv != null && recv.getType() == GroovyLexer.DOT) {
                    FQName name = toName(recv);
                    if ("rootProject.file".equals(name.toString())) {
                        List<Object> args = (List) evalExprSingleScope(scopeIndex, (GroovySourceAST) recv.getNextSibling());
                        List<String> argStrs = args.stream().map(Object::toString).collect(Collectors.toList());
                        String rootProjectDir = scope.getProject("rootProject").get(scopeIndex).getAttribute("projectDir").toString();
                        String filepath = Paths.get(rootProjectDir).resolve(String.join("/", argStrs)).toString();
                        return filepath;
                    }
                } else if (recv.getType() == GroovyLexer.IDENT && "project".equals(recv.getText())) {
                    AST args = recv.getNextSibling();
                    if (args.getNextSibling() != null) { // expect 1 arg
                        break;
                    }
                    Object argVal = evalExprSingleScope(scopeIndex, (GroovySourceAST) args.getFirstChild());
                    String projName = argVal.toString();
                    if (projName.startsWith(":")) {
                        projName = projName.substring(1);
                    }
                    Project matchedProj = null;
                    for (Project proj : globals.getAllProjects()) {
                        if (projName.equals(proj.getAttribute("name"))) {
                            matchedProj = proj;
                            break;
                        }
                    }
                    if (matchedProj == null) {
                        throw new NameNotFound(this, t, "could not find project with name \"" + projName + "\"");
                    }
                    return matchedProj;
                }
            }
            case GroovyLexer.PLUS: {
                AST l = t.getFirstChild();
                AST r = l.getNextSibling();
                Object left = evalExprSingleScope(scopeIndex, (GroovySourceAST) l);
                Object right = evalExprSingleScope(scopeIndex, (GroovySourceAST) r);
                if (left instanceof String || right instanceof String) {
                    return left.toString() + right.toString();
                }
            }
        }
        throw new UnsupportedExpressionSyntax(this, t, null);
    }

    /**
     * Converts an AST node to the fully qualified name (dot-separated) it represents.
     */
    public FQName toName(GroovySourceAST t) throws UnsupportedNameSyntax {
        switch (t.getType()) {
            case GroovyLexer.IDENT: {
                return new FQName(t.getText());
            }
            case GroovyLexer.DOT: {
                List<FQName> components = Lists.newArrayList();
                for (GroovySourceAST c = (GroovySourceAST) t.getFirstChild(); c != null; c = (GroovySourceAST) c.getNextSibling()) {
                    components.add(toName(c));
                }
                return FQName.fromConcat(components);
            }
            default:
                throw new UnsupportedNameSyntax(this, t, null);
        }
    }

    /**
     * Returns the fully qualified attribute name that the argument name refers to within the current scope.
     * By default, this is just the argument name itself, but in some cases, the name resolves to an attribute name
     * that incorporates some part of the scope as a prefix.
     *
     * For instance, inside the following block,
     *
     *   sourceSets {
     *       main {
     *           java {
     *               srcDirs = ["src"]
     *           }
     *       }
     *   }
     *
     * the name `srcDirs` refers to the attribute name `main.java.srcDirs`.
     */
    private FQName getAttributeName(FQName name) {
        List<String> scopeComponents = null;
        for (int i = 0; i < scope.names.size(); i++) {
            if ("sourceSets".equals(scope.names.get(i))) {
                scopeComponents = scope.names.subList(i+1, scope.names.size());
                break;
            }
        }
        if (scopeComponents == null) {
            return name;
        }
        ArrayList<String> attribute = Lists.newArrayList(scopeComponents);
        attribute.addAll(name.name);
        return new FQName(attribute);
    }

    /**
     * Assigns a value to a LHS expression.
     */
    private void assignLHS(GroovySourceAST lhs, List<Object> value) throws UnsupportedExpressionSyntax, UnsupportedNameSyntax, NameNotFound {
        GroovySourceAST firstChild = (GroovySourceAST) lhs.getFirstChild();
        if (lhs.getType() == GroovyLexer.DOT && firstChild != null && firstChild.getType() == GroovyLexer.METHOD_CALL && firstChild.getNextSibling() != null) {
            // method call + name case: `project(":my-project").ext.name = ...`
            assignToExprName(firstChild, (GroovySourceAST)firstChild.getNextSibling(), value);
            return;
        }

        // simple name case: `ext.name = ...`
        FQName name = toName(lhs);
        assignName(name, value);
    }

    /**
     * Assigns a value to a LHS expression of the form ${expr}.${field_name}. @expr is the AST node for the expression
     * and @name is the AST node for the name.
     */
    private void assignToExprName(GroovySourceAST expr, GroovySourceAST name, List<Object> value) throws UnsupportedExpressionSyntax, UnsupportedNameSyntax, NameNotFound {
        List<Object> assignable = evalExpr(expr);
        if (assignable.size() != value.size()) {
            throw new IllegalArgumentException("assignable.size() != value.size()");
        }

        FQName fqName = toName(name);
        for (int i = 0; i < assignable.size(); i++) {
            Object a = assignable.get(i);
            Object v = value.get(i);
            if (a instanceof Project) {
                ((Project) a).setAttribute(fqName.toString(), v);
            }
        }
    }

    /**
     * Assigns a value to a name within the current scope. Determines whether to assign a local or global variable
     * and makes the assignment in the appropriate scope.
     */
    private void assignName(FQName name, List<Object> value) throws UnsupportedNameSyntax {
        if (!ignorableBlockStack.isEmpty()) {
            return;
        }
        if ("srcDirs".equals(name.last())) {
            FQName sourceSetProp = getAttributeName(name);
            if (sourceSetProp == null) {
                return;
            }

            List<Project> projects = scope.getProject("project");
            for (int i = 0; i < projects.size(); ++i) {
                projects.get(i).setAttribute(sourceSetProp.toString(), value.get(i));
            }
            return;
        }

        // Handle any global assignments. Prefer names prefixed with "ext", "project", "rootProject". If the assignment
        // doesn't match any of those, figure out if the immediately containing scope warrants property assignment.
        String projectIdent = null;
        String propertyName = null;
        if ("ext".equals(name.car())) {
            projectIdent = "project";
            propertyName = name.cdr();
        } else if ("project".equals(name.car())) {
            projectIdent = "project";
            propertyName = name.cdr();
        } else if ("rootProject".equals(name.car())) {
            projectIdent = "rootProject";
            propertyName = name.cdr();
        } else {
            projectIdent = getEffectiveProjectIdentFromCurrentScope();
            propertyName = name.toString();
        }
        if (projectIdent != null && propertyName != null) {
            scope.setOnProject(projectIdent, propertyName, value);
        }

        // Add name to local scope for a little redundancy
        scope.set(name.toString(), value);
    }

    private static final ImmutableSet<String> PROJECT_PROPERTY_SCOPES =
            ImmutableSet.of(TOP_LEVEL_SCOPE, "ext", "subprojects", "allprojects", "rootProject.children.each", "mavenDeployer");

    /**
     * Are we currently in a scope where variable assignments should be treated as property assignments on a project?
     * If so, which project? This is a whitelist approach.
     */
    private String getEffectiveProjectIdentFromCurrentScope() {
        String currentScopeName = scope.names.get(scope.names.size() - 1);
        if (PROJECT_PROPERTY_SCOPES.contains(currentScopeName)) {
            return "project";
        }
        return null;
    }

    /**
     * Returns the value of the variable specified by name.
     */
    private Object evalName(int scopeIndex, FQName name, GroovySourceAST nameNode) throws NameNotFound {
        if ("project".equals(name.car())) {
            Project proj = scope.getProject("project").get(scopeIndex);
            if (proj.getAttribute(name.cdr()) != null) {
                return proj.getAttribute(name.cdr());
            }
        } else if ("rootProject".equals(name.car())) {
            Project rootProj = scope.getProject("rootProject").get(scopeIndex);
            if (rootProj.getAttribute(name.cdr()) != null) {
                return rootProj.getAttribute(name.cdr());
            }
        } else {
            Project proj = scope.getProject("project").get(scopeIndex);
            if (proj.getAttribute(name.toString()) != null) {
                return proj.getAttribute(name.toString());
            }
            if (proj.getAttribute(name.car()) instanceof Map) {
                return ((Map) proj.getAttribute(name.car())).get(name.cdr());
            }
        }
        // See if there's a matching property
        Project proj = scope.getProject("project").get(scopeIndex);
        if (proj.getProperty(name.toString()) != null) {
            return proj.getProperty(name.toString());
        }

        // Try to find name as variable somewhere in scope
        Object localVal = scope.get(scopeIndex, name.toString());
        if (localVal == null) {
            throw new NameNotFound(this, nameNode, "name \"" + name.toString() + "\" not found in scope");
        }
        return localVal;
    }

    /**
     * These visitor methods detect AST patterns and make appropriate updates to state (globals and scope).
     * <p>
     * NOTE: if any additional visitor methods are overridden, make sure that `super.visit*()` is called in the
     * appropriate location so that `visitDefault` is still invoked. This is necessary to ensure that the value of
     * `parents` is always correct.
     */
    @Override
    public void visitDefault(GroovySourceAST t, int visit) {
        if (visit == Visitor.OPENING_VISIT) {
            parents.add(t);
        } else if (visit == Visitor.CLOSING_VISIT) {
            parents.remove(parents.size() - 1);
        }
        super.visitDefault(t, visit);
    }

    /**
     * Handles visiting an identifier. Note that handlers here should be mutually exclusive with handlers in visitMethod.
     */
    @Override
    public void visitIdent(GroovySourceAST t, int visit) {
        super.visitIdent(t, visit);

        if (visit == Visitor.CLOSING_VISIT) {
            try {
                setLastFQName(toName(t));
            } catch (UnsupportedNameSyntax e) {
                debug("Not setting last fully qualified name", e);
            }
        }
    }

    @Override
    public void visitDot(GroovySourceAST t, int visit) {
        super.visitDot(t, visit);
        if (visit == Visitor.CLOSING_VISIT) {
            try {
                // Remember last fully qualified name, so we can name block scopes.
                if (t.getNextSibling() != null && t.getNextSibling().getType() == GroovyLexer.CLOSABLE_BLOCK) {
                    boolean isIdent = false;
                    for (AST leftmost = t.getFirstChild(); leftmost != null; leftmost = leftmost.getFirstChild()) {
                        if (leftmost.getType() == GroovyLexer.METHOD_CALL) {
                            isIdent = false;
                            break;
                        }
                        if (leftmost.getType() == GroovyLexer.IDENT) {
                            isIdent = true;
                            break;
                        }
                    }
                    if (isIdent) {
                        setLastFQName(toName(t));
                    }
                }
            } catch (Exception e) {
                debug("Not setting last fully qualified name", e);
            }
        }
    }

    @Override
    public void visitClosedBlock(GroovySourceAST t, int visit) {
        // rootProject.children.each|subprojects { project -> ... }
        String name = getLastFQName() != null ? getLastFQName().toString() : "";
        if (visit == Visitor.OPENING_VISIT) {
            if (IGNORE_BLOCKS.contains(name)) {
                ignorableBlockStack.add(name);
            }

            if ("rootProject.children.each".equals(name) || "subprojects".equals(name)) {
                scope.push(name, globals.subProjects.values().stream().map(p -> {
                    Map<String, Object> m = Maps.newHashMap();
                    m.put("project", p);
                    return m;
                }).collect(Collectors.toList()));
            } else if ("allprojects".equals(name)) {  // all projects { ... }
                scope.push(name, globals.getAllProjects().stream().map(p -> {
                    Map<String, Object> m = Maps.newHashMap();
                    m.put("project", p);
                    return m;
                }).collect(Collectors.toList()));
            } else {  // $ident { ... }
                scope.push(name, Lists.newArrayList(Maps.newHashMap()));
            }
        } else if (visit == Visitor.CLOSING_VISIT) {
            scope.pop();

            if (IGNORE_BLOCKS.contains(name) && !ignorableBlockStack.isEmpty() && ignorableBlockStack.peek().equals(name)) {
                ignorableBlockStack.pop();
            }
        }
        super.visitClosedBlock(t, visit);
    }

    @Override
    public void visitVariableDef(GroovySourceAST t, int visit) {
        if (visit == Visitor.OPENING_VISIT) {
            try {
                // ($MODIFIER)? $TYPE $VAR = ...
                GroovySourceAST name = (GroovySourceAST) t.getFirstChild();
                if (name != null && name.getType() == GroovyLexer.MODIFIERS) {
                    name = (GroovySourceAST) name.getNextSibling();
                }
                if (name != null && name.getType() == GroovyLexer.TYPE) {
                    name = (GroovySourceAST) name.getNextSibling();
                }
                if (name != null && name.getNextSibling() != null && name.getNextSibling().getType() == GroovyLexer.ASSIGN && name.getNextSibling().getFirstChild() != null) {
                    List<Object> value = evalExpr((GroovySourceAST) name.getNextSibling().getFirstChild());
                    assignName(toName(name), value);
                } else {
                    throw new UnsupportedExpressionSyntax(this, t, "unrecognized variable definition syntax");
                }
            } catch (Exception e) {
                debug("Ignored local var", e);
            }
        }
        super.visitVariableDef(t, visit);
    }

    @Override
    public void visitAssign(GroovySourceAST t, int visit) {
        if (visit == Visitor.OPENING_VISIT) {
            // $fully.qualfied.name|$ident = $rhs
            GroovySourceAST lhs = (GroovySourceAST) t.getFirstChild();
            GroovySourceAST rhs = (GroovySourceAST) lhs.getNextSibling();
            try {
                List<Object> value = evalExpr(rhs);
                assignLHS(lhs, value);
            } catch (Exception e) {
                debug("Ignored assignment to LHS: ", e);
            }
        }
        super.visitAssign(t, visit);
    }

    /**
     * Handles visiting a method call. Note that handlers here should be mutually exclusive with handlers in visitIdent.
     */
    @Override
    public void visitMethodCall(GroovySourceAST t, int visit) {
        if (visit == Visitor.OPENING_VISIT && t.getFirstChild() != null) {
            try {
                GroovySourceAST recvNode = (GroovySourceAST) t.getFirstChild();
                FQName recv = toName(recvNode);
                if ("srcDir".equals(recv.last())) {
                    handleSrcDir(t);
                } else if (ATTR_METHODS.contains(recv.toString())) {
                    handleAttrSetMethodCall(recvNode);
                } else {
                    switch (recv.toString()) {
                        case "include": // include '$subproject'
                            handleInclude(recvNode);
                            break;
                        case "repositories": {
                            handleRepositories(recvNode);
                            break;
                        }
                        case "dependencies": {
                            handleDependencies(recvNode);
                            break;
                        }
                        case "apply": {
                            handleApply(recvNode);
                            break;
                        }
                        case "mavenBom": {
                            handleMavenBom(recvNode);
                            break;
                        }
                    }
                }
            } catch (UnsupportedNameSyntax e) {
                debug("Ignored method (unrecognized receiver)", e);
            }
        }
        super.visitMethodCall(t, visit);
    }

    /**
     * Pretty print helper
     */
    private String nodeToString(GroovySourceAST t) {
        ArrayList<String> printed = Lists.newArrayList();
        nodeToStringHelper(0, t.getLine(), printed, t);
        return "[" + scriptUri + ", line " + t.getLine() + ", col " + t.getColumn() + "]: " + String.join(" ", printed);
    }

    private void nodeToStringHelper(int depth, int rootline, List<String> printed, GroovySourceAST t) {
        if (t == null) {
            return;
        }

        if (t.getLine() == rootline && t.getFirstChild() == null) {
            printed.add(t.getText());
        }
        for (AST n = t.getFirstChild(); n != null; n = n.getNextSibling()) {
            nodeToStringHelper(depth + 1, rootline, printed, (GroovySourceAST)n);
        }
    }

    private void handleApply(GroovySourceAST t) {
        AST right = t.getNextSibling();
        if (right == null || right.getType() != GroovyLexer.ELIST) {
            return;
        }
        AST colon = right.getFirstChild();
        if (colon.getType() != GroovyLexer.LABELED_ARG || !":".equals(colon.getText())) {
            return;
        }
        AST directive = colon.getFirstChild();
        if (directive.getType() != GroovyLexer.STRING_LITERAL) {
            return;
        }
        if ("from".equals(directive.getText())) {
            handleApplyFrom(t);
        } else if ("plugin".equals(directive.getText())) {
            handleApplyPlugin(t);
        }
    }

    private void handleApplyPlugin(GroovySourceAST t) {
        AST right = t.getNextSibling();
        if (right == null || right.getType() != GroovyLexer.ELIST) {
            return;
        }
        AST colon = right.getFirstChild();
        if (colon.getType() != GroovyLexer.LABELED_ARG || !":".equals(colon.getText())) {
            return;
        }
        AST directive = colon.getFirstChild();
        if (directive.getType() != GroovyLexer.STRING_LITERAL || !"plugin".equals(directive.getText())) {
            return;
        }
        AST plugin = directive.getNextSibling();
        if (plugin == null || plugin.getType() != GroovyLexer.STRING_LITERAL) {
            return;
        }
        String pluginName = plugin.getText().toString();

        List<Project> project = scope.getProject("project");
        for (int i = 0; i < project.size(); i++) {
            Project proj = project.get(i);
            proj.plugins.add(pluginName);
        }
    }

    public void handleSrcDir(GroovySourceAST t) {
        if (t.getType() != GroovyLexer.METHOD_CALL) {
            return;
        }
        try {
            FQName recv = toName((GroovySourceAST) t.getFirstChild());
            if ("srcDir".equals(recv.last())) {
                FQName sourceSetProp = getAttributeName(recv);
                if (sourceSetProp != null) {
                    List<Project> proj = scope.getProject("project");
                    List<Object> val = evalExpr((GroovySourceAST) t.getFirstChild().getNextSibling());
                    for (int i = 0; i < proj.size(); i++) {
                        proj.get(i).setAttribute(sourceSetProp.toString(), val.get(i));
                    }
                }
            }
        } catch (UnsupportedNameSyntax | UnsupportedExpressionSyntax | NameNotFound e) {
            warn("Ignored `srcDir` method (unrecognized receiver)", e);
        }
    }

    /**
     * Execute sub-scripts on `apply`.
     */
    private void handleApplyFrom(GroovySourceAST t) {
        AST right = t.getNextSibling();
        if (right == null || right.getType() != GroovyLexer.ELIST) {
            return;
        }
        AST colon = right.getFirstChild();
        if (colon.getType() != GroovyLexer.LABELED_ARG || !":".equals(colon.getText())) {
            return;
        }
        AST directive = colon.getFirstChild();
        if (directive.getType() != GroovyLexer.STRING_LITERAL || !"from".equals(directive.getText())) {
            return;
        }
        GroovySourceAST scriptNameNode = (GroovySourceAST) directive.getNextSibling();
        try {
            List<Object> scriptName = evalExpr(scriptNameNode);
            List<Scope> subScopes = scope.extractSubScriptScopes();
            if (scriptName.size() != subScopes.size()) {
                throw new IllegalStateException("scriptName.size() != subScopes.size()");
            }
            Path basePath = LanguageUtils.uriToPath(scriptUri).getParent();
            for (int p = 0; p < scriptName.size(); p++) {
                String subscriptUri = LanguageUtils.pathToUri(basePath.resolve(scriptName.get(p).toString()).toString());
                Fradle.runGradle(subscriptUri, fileProvider, globals, subScopes.get(p), messages);
            }
        } catch (Exception e) {
            warn("Ignored sub-script", e);
        }
    }

    /**
     * Record repositories
     */
    private void handleRepositories(GroovySourceAST t) {
        String projIdent = getEffectiveProjectIdentFromCurrentScope();
        if (projIdent == null) {
            return;
        }
        AST block = t.getNextSibling();
        if (block == null) {
            return;
        }
        GroovySourceAST implicitParams = (GroovySourceAST)block.getFirstChild();
        if (implicitParams == null || implicitParams.getType() != GroovyLexer.IMPLICIT_PARAMETERS) {
            return ;
        }
        for (AST elem = implicitParams.getNextSibling(); elem != null; elem = elem.getNextSibling()) {
            AST methodCall = elem.getFirstChild();
            if (methodCall == null || methodCall.getType() != GroovyLexer.METHOD_CALL) {
                continue;
            }
            AST methodName = methodCall.getFirstChild();
            if (methodName == null || methodName.getType() != GroovyLexer.IDENT) {
                continue;
            }
            List<String> url = getMavenURL((GroovySourceAST) methodName);
            if (url != null) {
                List<Project> project = scope.getProject(projIdent);
                if (project.size() != url.size()) {
                    throw new IllegalStateException("project.size() != url.size()");
                }
                for (int p = 0; p < project.size(); p++) {
                    if (url.get(p) != null && url.get(p).startsWith("http")) {
                        project.get(p).addRepository(url.get(p));
                    }
                }
            }
        }
    }

    /**
     * Handle:
     * maven {
     *     url "..."
     * }
     */
    private List<String> getMavenURL(GroovySourceAST t) {
        if (t == null || t.getType() != GroovyLexer.IDENT) {
            return null;
        }
        if (!"maven".equals(t.getText())) {
            return null;
        }
        try {
            AST block = t.getNextSibling();
            AST implicitParams = block.getFirstChild();
            for (AST expr = implicitParams.getNextSibling(); expr != null; expr = expr.getNextSibling()) {
                AST meth = expr.getFirstChild();
                AST ident = meth.getFirstChild();
                if (ident.getType() == GroovyLexer.IDENT && "url".equals(ident.getText())) {
                    AST elist = ident.getNextSibling();
                    List<Object> val = evalExpr((GroovySourceAST)elist);
                    return val.stream().map(v -> {
                        if (v instanceof List) {
                            return String.join("", (List)v);
                        }
                        return v.toString();
                    }).collect(Collectors.toCollection(ArrayList::new));
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Record dependencies added by the `dependencies` directive.
     */
    private void handleDependencies(GroovySourceAST t) {
        if (t.getType() != GroovyLexer.IDENT) {
            throw new IllegalArgumentException("dependencies node must by of type IDENT, was " + t.getType());
        }
        AST blockMaybe = t.getNextSibling();
        if (blockMaybe == null) {
            return;
        }
        if (blockMaybe.getNumberOfChildren() < 2) {
            return;
        }
        List<Project> project = scope.getProject(null);
        for (int p = 0; p < project.size(); p++) {
            Project proj = project.get(p);
            for (AST depExpr = blockMaybe.getFirstChild().getNextSibling(); depExpr != null; depExpr = depExpr.getNextSibling()) {
                AST directive = depExpr.getFirstChild();
                if (directive != null && directive.getType() == GroovyLexer.METHOD_CALL) {
                    String directiveName = directive.getFirstChild().getText();
                    if ("compileOnly".equals(directiveName)) {
                        // See https://blog.gradle.org/introducing-compile-only-dependencies
                        directiveName = "provided";
                    }
                    GroovySourceAST elist = (GroovySourceAST) directive.getFirstChild().getNextSibling();
                    if (elist.getType() == GroovyLexer.ELIST) {
                        try {
                            Object dependency = evalExprSingleScope(p, elist);
                            if (dependency instanceof Map) {
                                switch (directiveName) {
                                    case "compile":
                                    case "testCompile":
                                    case "provided":
                                    case "androidTestCompile":
                                        proj.addDependency(directiveName, toArtifactStringOrProject(dependency));
                                }
                            } else {
                                List listVal = (List) dependency;
                                for (Object v : listVal) {
                                    switch (directiveName) {
                                        case "compile":
                                        case "testCompile":
                                        case "provided":
                                        case "androidTestCompile":
                                            proj.addDependency(directiveName, toArtifactStringOrProject(v));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            switch (directiveName) {
                                case "compile":
                                case "testCompile":
                                case "provided":
                                case "androidTestCompile":
                                    error("Ignored dependency", e);
                                    break;
                                default:
                                    debug("Ignored dependency statement", e);
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static Object toArtifactStringOrProject(Object v) {
        if (v instanceof Project) {
            return  v;
        } else if (v instanceof Map) {
            Map m = (Map) v;
            String group = String.valueOf(m.get("group"));
            String name = String.valueOf(m.get("name"));
            String version = String.valueOf(m.get("version"));
            return group + ':' + name + ':' + version;
        }
        return v.toString();
    }

    /**
     * Record subprojects added by `include` directive in settings.gradle.
     */
    private void handleInclude(GroovySourceAST t) {
        if (!scriptUri.endsWith("settings.gradle")) {
            return;
        }
        for (AST e = t.getNextSibling().getFirstChild(); e != null; e = e.getNextSibling()) {
            if (e.getType() == GroovyLexer.STRING_LITERAL) {
                Project subProject = new Project();
                String projName = e.getText();
                if (projName != null && projName.startsWith(":")) {
                    projName = projName.substring(1);
                }
                String projDir = projName;
                if (projDir != null) {
                    projDir = projDir.replace(':', '/');
                }
                String projId = projDir;
                if (projId != null) {
                    // dealing with the case "include 'foo:bar'"
                    int pos = projId.lastIndexOf('/');
                    if (pos > 0) {
                        projId = projId.substring(pos + 1);
                    }
                }
                subProject.setAttribute("name", projId);
                subProject.setAttribute("projectDir", projDir);
                subProject.setAttribute("buildFileName", "build.gradle");
                globals.subProjects.put(projName, subProject);
            }
        }
    }

    /**
     * dependencyManagement {
     *   imports {
     *     mavenPom "..."
     *   }
     * }
     */
    private void handleMavenBom(GroovySourceAST t) {
        if (!(scope.names.size() >= 2 && "imports".equals(scope.names.get(scope.names.size() - 1)) && "dependencyManagement".equals(scope.names.get(scope.names.size() - 2)))) {
            return;
        }
        AST elist = t.getNextSibling();
        if (elist == null) {
            return;
        }
        AST arg = elist.getFirstChild();
        if (arg == null) {
            return;
        }
        try {
            List<Object> dep = evalExpr((GroovySourceAST) arg);
            List<Project> project = scope.getProject("project");
            if (project.size() != dep.size()) {
                throw new IllegalStateException("project.size() != dep.size()");
            }
            for (int p = 0; p < project.size(); p++) {
                Project proj = project.get(p);
                String depVal = dep.get(p).toString();
                proj.depManagement.add(depVal);
            }
        } catch (Exception e) {
            error("Ignored dependency management dependency", e);
        }
    }

    /**
     * (group|version) "$GROUP"
     */
    private void handleAttrSetMethodCall(GroovySourceAST t) {
        String attr = t.getText();
        if (t.getType() != GroovyLexer.IDENT || !ATTR_METHODS.contains(attr)) {
            return;
        }
        AST args = t.getNextSibling();
        if (args == null || args.getType() != GroovyLexer.ELIST) {
            return;
        }
        if (args.getNextSibling() != null) { // single arg
            return;
        }
        AST arg = args.getFirstChild();
        if (arg == null) {
            return;
        }
        try {
            List<Object> argVal = evalExpr((GroovySourceAST) arg);
            List<Project> project = scope.getProject("project");
            if (argVal.size() != project.size()) {
                throw new IllegalStateException("argVal.size() != project.size()");
            }
            for (int p = 0; p < project.size(); p++) {
                project.get(p).setAttribute(attr, argVal.get(p).toString());
            }
        } catch (Exception e) {
            warn("Ignored setting attribute", e);
        }
    }

    private static final Set<String> ATTR_METHODS = ImmutableSet.of("group", "version");

    /**
     * Globals encapsulates global objects (i.e., the project hierarchy) shared across all Gradle scripts for a given
     * Settings instance (defined by settings.gradle)
     */
    public static class Globals {
        public Project rootProject = new Project();
        public Map<String, Project> subProjects = Maps.newHashMap();

        public Globals(String rootPath) {
            rootProject.setAttribute("projectDir", "");
            rootProject.setAttribute("buildFileName", "build.gradle");
            rootProject.setAttribute("rootDir", rootPath);
        }

        public List<Project> getAllProjects() {
            List<Project> allProjects = Lists.newArrayListWithCapacity(subProjects.size() + 1);
            allProjects.add(rootProject);
            allProjects.addAll(subProjects.values());
            return allProjects;
        }
    }

    public static class ScopeNode {
        public ScopeNode parent;
        public Map<String, Object> vars;

        public ScopeNode(ScopeNode parent, Map<String, Object> vars) {
            this.parent = parent;
            this.vars = vars;
        }
    }

    public static class FQName {
        private List<String> name;

        public FQName(String name) {
            this.name = Lists.newArrayList(name);
        }

        public FQName(List<String> name) {
            this.name = name;
        }

        public static FQName fromConcat(List<FQName> names) {
            List<String> components = Lists.newArrayList();
            for (FQName name : names) {
                components.addAll(name.name);
            }
            return new FQName(components);
        }

        public static FQName from(String fqName) {
            return new FQName(Lists.newArrayList(fqName.split("\\.")));
        }

        public String toString() {
            return String.join(".", name);
        }

        public String car() {
            return name.size() > 0 ? name.get(0) : "";
        }

        public String cdr() {
            return String.join(".", name.subList(1, name.size()));
        }

        public String last() {
            return name.get(name.size() - 1);
        }
    }

    /**
     * Scope encapsulates the variable namespace in the Fradle execution environment.
     */
    public static class Scope {
        public List<List<ScopeNode>> scopes;
        public List<String> names;

        private Scope(List<List<ScopeNode>> scopes, List<String> names) {
            this.scopes = scopes;
            this.names = names;
        }

        public static Scope of(Globals globals) {
            return of(ImmutableMap.of(
                    "settingsDir", "",
                    "rootProject", globals.rootProject,
                    "project", globals.rootProject
            ));
        }

        private static Scope of(Map<String, Object> initialVars) {
            List<List<ScopeNode>> scopes = Lists.<List<ScopeNode>>newArrayList(
                    Lists.newArrayList(new ScopeNode(null, new HashMap<>(initialVars)))
            );
            List<String> names = Lists.newArrayList(TOP_LEVEL_SCOPE);
            return new Scope(scopes, names);
        }

        private List<ScopeNode> currentLayer() {
            return scopes.get(scopes.size() - 1);
        }

        public int currentNumContexts() {
            return this.currentLayer().size();
        }

        /**
         * extractSubScriptScopes extracts scopes that can be used when invoking sub-scripts (via `apply`)
         */
        public List<Scope> extractSubScriptScopes() {
            List<Scope> subScriptScopes = Lists.newArrayListWithCapacity(currentNumContexts());
            for (ScopeNode base : currentLayer()) {
                List<List<ScopeNode>> chain = Lists.newArrayList();
                for (ScopeNode n = base; n != null; n = n.parent) {
                    chain.add(Lists.newArrayList(n));
                }
                subScriptScopes.add(new Scope(Lists.reverse(chain), Lists.newArrayList(names)));
            }
            return subScriptScopes;
        }

        public void push(String name, List<Map<String, Object>> contexts) {
            // Hard limit on number of contexts (avoid DoS by loops)
            int newLayerSize = currentLayer().size() * contexts.size();
            if (newLayerSize > 10_000) {
                throw new RuntimeException(String.format("Aborting Gradle execution, because too many contexts (%d)", newLayerSize));
            }

            ArrayList<ScopeNode> newLayer = Lists.newArrayList();
            for (ScopeNode current : currentLayer()) {
                for (Map<String, Object> context : contexts) {
                    newLayer.add(new ScopeNode(current, Maps.newHashMap(context)));
                }
            }
            scopes.add(newLayer);
            names.add(name);
        }

        public void pop() {
            names.remove(names.size() - 1);
            scopes.remove(scopes.size() - 1);
        }

        public void set(String name, List<Object> val) {
            if (currentLayer().size() != val.size()) {
                throw new IllegalArgumentException("val.size() != currentLayer().size()");
            }
            for (int i = 0; i < currentLayer().size(); i++) {
                set(i, name, val.get(i));
            }
        }

        private void set(int i, String name, Object val) {
            currentLayer().get(i).vars.put(name, val);
        }

        public List<Object> get(String name) {
            ArrayList<Object> vals = Lists.newArrayListWithCapacity(currentLayer().size());
            for (int i = 0; i < currentLayer().size(); i++) {
                vals.add(get(i, name));
            }
            return vals;
        }

        public void setOnProject(String projectIdent, String attr, List<Object> val) {
            List<Project> project = getProject(projectIdent);
            if (project.size() != val.size()) {
                throw new IllegalArgumentException("project.size() != val.size()");
            }
            for (int p = 0; p < val.size(); p++) {
                project.get(p).setAttribute(attr, val.get(p));
            }
        }

        public List<Project> getProject(String projectIdent) {
            if (projectIdent == null) {
                projectIdent = "project";
            }
            List<Object> project = this.get(projectIdent);
            return project.stream().map(p -> {
                if (!(p instanceof Project)) {
                    throw new IllegalArgumentException("non-project is set as \"project\" in scope");
                }
                return (Project) p;
            }).collect(Collectors.toList());
        }

        private Object get(int i, String name) {
            for (ScopeNode node = currentLayer().get(i); node != null; node = node.parent) {
                if (node.vars.containsKey(name)) {
                    return node.vars.get(name);
                }
            }
            return null;
        }
    }

    /**
     * Error logging: Fradle logic should use the debug and error methods rather than invoke this.log methods directly.
     */
    private static Logger log = LoggerFactory.getLogger(Fradle.class);

    private static void debug(String consequence, Exception e) {
        log.debug(printErrMsgToString(consequence, e));
    }

    private void error(String consequence, Exception e) {
        messages.error(printErrMsgToString(consequence, e));
    }

    private void warn(String consequence, Exception e) {
        messages.warn(printErrMsgToString(consequence, e));
    }

    static String printErrMsgToString(String consequence, Exception e) {
        String msg = consequence + ": \t" + e.getMessage();
        if (e.getStackTrace() != null && e.getStackTrace().length >= 1) {
            StackTraceElement s = e.getStackTrace()[0];
            msg += "\t(See " + s.getFileName() + "@" + s.getMethodName() + ":L" + s.getLineNumber() + ")";
        }
        return msg;
    }

    private static String errMsg(Fradle f, GroovySourceAST t, String reason) {
        int l = t.getLine() - 1;
        String line = "<error: could not fetch line>";
        if (f.lines != null && l < f.lines.size() && l >= 0) {
            line = f.lines.get(l);
        }
        String err = String.format("Interpreter error at %s:L%d - `%s`", f.scriptUri, l, line.trim());
        if (reason != null) {
            err += ". Reason: " + reason + ".";
        }
        return err;
    }

    /**
     * Fradle doesn't understand that syntax for declaring / referencing a name.
     */
    private static class UnsupportedNameSyntax extends Exception {
        public UnsupportedNameSyntax(Fradle f, GroovySourceAST t, String reason) {
            super(errMsg(f, t, reason));
        }
    }

    /**
     * Fradle doesn't understand that expression syntax.
     */
    private static class UnsupportedExpressionSyntax extends Exception {
        public UnsupportedExpressionSyntax(Fradle f, GroovySourceAST t, String reason) { super(errMsg(f, t, reason)); }
    }

    /**
     * We didn't find the name in the present scope or globals.
     */
    private static class NameNotFound extends Exception {
        public NameNotFound(Fradle f, GroovySourceAST t, String reason) {
            super(errMsg(f, t, reason));
        }
    }
}
