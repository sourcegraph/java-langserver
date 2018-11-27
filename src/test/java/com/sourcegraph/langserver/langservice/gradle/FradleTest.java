package com.sourcegraph.langserver.langservice.gradle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sourcegraph.lsp.MessageAggregator;
import org.codehaus.groovy.antlr.GroovySourceAST;
import org.codehaus.groovy.antlr.SourceBuffer;
import org.codehaus.groovy.antlr.UnicodeEscapingReader;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.codehaus.groovy.antlr.parser.GroovyRecognizer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertTrue;

/**
 * Created by beyang on 4/2/17.
 */
public class FradleTest {

    private static Logger log = LoggerFactory.getLogger(FradleTest.class);

    private static class ExprTestCase {
        Fradle.Scope scope;
        Fradle.Globals globals;
        String expr;
        Object expVal;

        public ExprTestCase(Fradle.Scope scope, Fradle.Globals globals, String expr, Object expVal) {
            this.globals = globals == null ? new Fradle.Globals("/") : globals;
            this.scope = scope == null ? Fradle.Scope.of(this.globals) : scope;
            this.expr = expr;
            this.expVal = expVal;
        }
    }

    @Test
    public void testFradleExpressions() {
        // Test convenience case state
        Fradle.Globals foobarGlobals = new Fradle.Globals("/");
        foobarGlobals.rootProject.setAttribute("name", "foobar");

        ImmutableList<ExprTestCase> cases = ImmutableList.of(
                new ExprTestCase(null, null, "'foobar'", "foobar"),
                new ExprTestCase(null, foobarGlobals, "project(':foobar')", foobarGlobals.rootProject),
                new ExprTestCase(null, null, "new File('foo/bar')", "foo/bar"),
                new ExprTestCase(null, null, "'foo' + 'bar'", "foobar")
        );
        for (ExprTestCase c : cases) {
            SourceBuffer sourceBuffer = new SourceBuffer();
            UnicodeEscapingReader u = new UnicodeEscapingReader(new StringReader(c.expr), sourceBuffer);
            GroovyLexer lex = new GroovyLexer(u);
            u.setLexer(lex);
            GroovyRecognizer parser = GroovyRecognizer.make(lex);
            parser.setSourceBuffer(sourceBuffer);
            try {
                parser.compilationUnit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            GroovySourceAST ast = (GroovySourceAST)parser.getAST();
            Fradle f = Fradle.newForTest(c.globals, c.scope);
            try {
                Object val = f.evalExprSingleScope(0, ast);
                assertTrue(String.format("Evaluating expression `%s`: %s != %s", c.expr, c.expVal, val), Objects.deepEquals(val, c.expVal));
            } catch (Exception e) {
                assertTrue(String.format("Error evaluating expression `%s`\n%s", c.expr, Fradle.printErrMsgToString("Test error", e)), false);
            }
        }
    }

    private static class StatementTestCase {
        Fradle.Scope scope;
        Fradle.Globals globals;
        String code;
        Runnable check;

        public StatementTestCase(Fradle.Scope scope, Fradle.Globals globals, String code, Runnable check) {
            this.scope = scope;
            this.globals = globals;
            this.code = code;
            this.check = check;
        }
    }

    @Test
    public void testFradleAssignments() {
        // Test cases
        List<StatementTestCase> cases = Lists.newArrayList();
        {
            Fradle.Globals globals = new Fradle.Globals("/");
            Fradle.Scope scope = Fradle.Scope.of(globals);
            String code = "group = 'foo'";
            cases.add(new StatementTestCase(scope, globals, code, () -> {
                assertAttribute(globals.rootProject, "group", "foo", code);
            }));
        }
        {
            Fradle.Globals globals = new Fradle.Globals("/");
            Fradle.Scope scope = Fradle.Scope.of(globals);
            String code = "group = 'the-right-group'\ntask buildTinkerSdk(type: Copy, dependsOn: [build]) {\n\tgroup = 'the-wrong-group'\n}";
            cases.add(new StatementTestCase(scope, globals, code, () -> {
                assertAttribute(globals.rootProject, "group", "the-right-group", code);
            }));
        }
        {
            Fradle.Globals globals = new Fradle.Globals("/");
            globals.subProjects.put("subproj", new Project());
            Fradle.Scope scope = Fradle.Scope.of(globals);
            String code = "subprojects {\n\tgroup = 'foo'\n}";
            cases.add(new StatementTestCase(scope, globals, code, () -> {
                assertAttribute(globals.rootProject, "group", null, code);
            }));
        }

        // Test logic
        for (StatementTestCase c : cases) {
            try {
                Fradle.runGradle("TEST", null, c.globals, c.scope, new StringReader(c.code), new MessageAggregator(null));
            } catch (Exception e) {
                assertTrue(String.format("Error evaluating code (%s):\n%s", Fradle.printErrMsgToString("Test error", e), c.code), false);
            }
            c.check.run();
        }
    }

    private static void assertAttribute(Project proj, String attr, String val, String code) {
        assertTrue(String.format("`%s` != `%s`, statement was:\n\n%s", proj.getAttribute(attr), val, code),
                Objects.deepEquals(proj.getAttribute(attr), val));
    }
}
