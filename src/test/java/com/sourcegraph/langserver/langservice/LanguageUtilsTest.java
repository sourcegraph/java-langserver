package com.sourcegraph.langserver.langservice;

import com.google.common.collect.ImmutableMap;
import com.sourcegraph.utils.LanguageUtils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Created by beyang on 8/4/17.
 */
public class LanguageUtilsTest {

    @Test
    public void testUriToPath() {
        ImmutableMap<String, Path> uriToPath = ImmutableMap.<String, Path>builder()
                .put("file:///foo/bar", Paths.get("/foo/bar"))
                .put("file:///", Paths.get("/"))
                .put("file:///foo", Paths.get("/foo"))
                .build();

        for (Map.Entry<String, Path> e : uriToPath.entrySet()) {
            String uri = e.getKey();
            Path path = e.getValue();

            Path gotPath = LanguageUtils.uriToPath(uri);
            Assert.assertEquals("URI " + uri, gotPath, path);

            String gotUri = LanguageUtils.pathToUri(path.toString());
            Assert.assertEquals("Path " + path.toString(), gotUri, uri);
        }
    }
}
