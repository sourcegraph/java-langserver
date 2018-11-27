package com.sourcegraph.langserver.langservice.maven;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.sourcegraph.langserver.langservice.files.FileSystemFileProvider;
import com.sourcegraph.langserver.langservice.workspace.TestMessenger;
import com.sourcegraph.lsp.MessageAggregator;
import com.sourcegraph.lsp.domain.result.WorkspaceConfigurationServersResult;
import junit.framework.Assert;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.io.DefaultModelWriter;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class EffectivePomTest {

    @Test
    public void testEffectivePomResolution() throws Exception {
        // TODO: use different mvn cache for test (and clear it before each run)

        URL testDataRoot = Resources.getResource("_EffectivePomTestData");
        File[] testCaseDirs = new File(testDataRoot.getFile()).listFiles();
        List<String> testCaseDirnames = new ArrayList<>();
        for (File f : testCaseDirs) {
            if (f.isDirectory()) {
                testCaseDirnames.add(FilenameUtils.getBaseName(f.getName()));
            }
        }

        List<WorkspaceConfigurationServersResult.Server> servers = new ArrayList<>();
        WorkspaceConfigurationServersResult.Server privServer = new WorkspaceConfigurationServersResult.Server();
        privServer.setId("my-maven-repo");
        privServer.setUsername("myMavenRepo");
        privServer.setPassword("asdf");
        servers.add(privServer);

        for (String testCaseName : testCaseDirnames) {
            Path root = Paths.get(Resources.getResource("_EffectivePomTestData/" + testCaseName).toURI());
            FileSystemFileProvider files = new FileSystemFileProvider(root);
            MessageAggregator msgs = new MessageAggregator(new TestMessenger());


            List<String> pomUris = Lists.newArrayList("file:///pom.xml");
            MavenWorkspaceModelResolver mavenWorkspaceModelResolver = MavenWorkspaceModelResolver.newResolver(pomUris, files, msgs);

            String expEffectivePomXml = IOUtil.toString(files.readContent("file:///effective-pom.xml"));

            MavenWorkspaceModelResolver.PomInfo pomInfo = mavenWorkspaceModelResolver.getPomInfos().values().iterator().next();
            EffectivePom effectivePom = EffectivePom.createAndResolve("/", pomInfo.rawModel, mavenWorkspaceModelResolver,
                    new ArrayList<>(),
                    msgs,
                    servers);

            StringWriter stringWriter = new StringWriter();
            new DefaultModelWriter().write(stringWriter, null, effectivePom.getEffectiveModel());
            String effectivePomXml = stringWriter.toString();

            Assert.assertEquals(expEffectivePomXml, effectivePomXml);
        }
    }
}
