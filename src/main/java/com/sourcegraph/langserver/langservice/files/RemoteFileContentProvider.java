package com.sourcegraph.langserver.langservice.files;

import com.sourcegraph.lsp.FileContentProvider;
import com.sourcegraph.lsp.domain.structures.TextDocumentIdentifier;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * RemoteFileContentProvider serves file contents from the Sourcegraph raw file API.
 */
public class RemoteFileContentProvider implements FileContentProvider {

    /**
     * remoteRootURI is the remote root URI, e.g.,
     * "https://${TOKEN}@sourcegraph.com/github.com/apache/commons-io@4daab02fb7d967a39eb15fe33f0d5350fc548a98/-/raw/"
     */
    private URL remoteRootURI;

    private File cacheContainer;

    public RemoteFileContentProvider(String remoteRootURI, File cacheContainer) throws IllegalArgumentException {
        try {
            this.remoteRootURI = new URL(remoteRootURI);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        this.cacheContainer = cacheContainer;

        // TODO: make async
        fetchTree(remoteRootURI, uriToCachePath(remoteRootURI));
    }

//    // TODO(beyang): change? (deals with old-style URIs)
//    @Override
//    public InputStream readContent(String uri) throws Exception {
//        if (!uri.startsWith("file://")) {
//            throw new Exception("bad URI");
//        }
//        String remoteURI = remoteRootURI + uri.substring("file://".length());
//        return new FileInputStream(uriToCachePath(remoteURI));
//    }

    // TODO: this should be the new readContent, if the URIs are switched over.
    @Override
    public InputStream readContent(String uri) throws Exception {
        URL parsedURI = new URL(uri);
        if (!parsedURI.getHost().equals(remoteRootURI.getHost())) {
            throw new IllegalArgumentException("requested URI was not a sub-URI");
        }
        if (!(parsedURI.getPath().equals(remoteRootURI.getPath()) || parsedURI.getPath().startsWith(remoteRootURI.getPath()))) {
            throw new IllegalArgumentException("requested URI was not a sub-URI");
        }
        String path = uriToCachePath(uri);
        return new FileInputStream(path);
    }

//    // TODO(beyang): change? (deals with old-style URIs)
//    @Override
//    public List<TextDocumentIdentifier> listFilesRecursively(String baseUri) throws Exception {
//        if (!(baseUri.startsWith("file://"))) {
//            throw new Exception("bad URI");
//        }
//        String remoteURI = remoteRootURI + baseUri.substring("file://".length());
//        return Files.walk(Paths.get(uriToCachePath(remoteURI)))
//                .filter(Files::isRegularFile)
//                .map(u -> new TextDocumentIdentifier().withUri(cachePathToLegacyUri(u)))
//                .collect(Collectors.toList());
//    }

    @Override
    public List<TextDocumentIdentifier> listFilesRecursively(String baseUri) throws Exception {
        String cachePath = uriToCachePath(baseUri);
        return Files.walk(Paths.get(cachePath))
                .filter(Files::isRegularFile)
                .map(u -> new TextDocumentIdentifier().withUri(cachePathToUri(u.toString())))
                .collect(Collectors.toList());
    }

    private String uriToCachePath(String uri) {
        if (uri == null) {
            return null;
        }
        try {
            URL url = new URL(uri);
            //			return Paths.get(cacheRootDir(), url.getProtocol(), url.getHost(), url.getPath().replace("/", File.separator)).toString();
            return Paths.get(cacheRootDir(), url.getProtocol(), url.getHost(), url.getPath().replace("/", File.separator)).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String cachePathToUri(String cachePath) {
        try {
            String rootPath = uriToCachePath(remoteRootURI.toString());
            Path rel = Paths.get(rootPath).relativize(Paths.get(cachePath));
            return new URL(remoteRootURI, rel.toString()).toString();
        } catch (Exception e) {
            // TODO(beyang)
            throw new RuntimeException(e);
        }
    }

    private String localToRemoteUri(String local) {
        if (local == null) {
            return null;
        }
        String path = StringUtils.replace(StringUtils.removeStart(local, "file://"), "/", File.separator);
        Path subpath = Paths.get(StringUtils.removeStart(StringUtils.removeStart(path, cacheRootDir()), File.separator));
        if (subpath.getNameCount() < 2) {
            throw new RuntimeException("TODO");
        }
        String protocol = subpath.getName(0).toString();
        String remainder = StringUtils.removeStart(subpath.toString(), protocol + File.separator);
        return protocol + "://" + StringUtils.replace(remainder, File.separator, "/");
    }

    private String remoteToLocalUri(String remote) {
        if (remote == null) {
            return null;
        }
        return "file://" + uriToCachePath(remote).replace(File.separator, "/");
    }

    private String cacheTmpDir() {
        return Paths.get(cacheContainer.toString(), "tmp").toString();
    }

    private String cacheRootDir() {
        return Paths.get(cacheContainer.toString(), "root").toString();
    }

    private String cachePathToLegacyUri(Path path) {
        String cacheRoot = uriToCachePath(remoteRootURI.toString());
        return "file:///" + Paths.get(cacheRoot).relativize(path).toString();
    }

    private void fetchTree(String remoteUri, String localPath) {
        // TODO(beyang): try-with-resources block (so things are closed properly)
        try {
            if (Files.exists(Paths.get(localPath))) {
                return;
            }

            InputStream respBody = HTTPUtil.httpGet(remoteUri);
            new File(cacheTmpDir()).mkdirs();
            Path tmpDir = Files.createTempDirectory(Paths.get(cacheTmpDir()), Paths.get(localPath).getFileName().toString());
            // TODO(beyang): delete tmpDir if still exists

            ZipInputStream zipIn = new ZipInputStream(respBody);
            byte[] buffer = new byte[1024];
            for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
                if (entry.isDirectory()) {
                    new File(tmpDir + File.separator + entry.getName()).mkdirs();
                    continue;
                }
                File newFile = new File(tmpDir + File.separator + entry.getName());
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zipIn.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }

            new File(new File(localPath).getParent()).mkdirs();
            Files.move(tmpDir, Paths.get(localPath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
