package com.sourcegraph.langserver;

import com.sourcegraph.langserver.langservice.*;
import com.sourcegraph.langserver.langservice.gradle.FradleTest;
import com.sourcegraph.langserver.langservice.gradle.GradleServiceTest;
import com.sourcegraph.langserver.langservice.gradle.GradleUtilTest;
import com.sourcegraph.langserver.langservice.maven.EffectivePomTest;
import com.sourcegraph.lsp.ControllerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ControllerTest.class,
        ResourceFileProviderTest.class,
        GradleServiceTest.class,
        GradleUtilTest.class,
        FradleTest.class,
        LanguageUtilsTest.class,
        EffectivePomTest.class
}) // Note that Categories is a kind of Suite
public class AllTestSuite {
}
