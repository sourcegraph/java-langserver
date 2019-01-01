/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.sourcegraph.langserver.langservice.files;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author beyang
 *
 */
public class HTTPUtil {
    public static InputStream httpGet(String url) {
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            // TODO(beyang): pass in token
            conn.setRequestProperty("Authorization", "token 223144ec7fd26f7bd6ed46521bf2957262216fa3");
            conn.setRequestProperty("Accept", "application/zip");

            return new BufferedInputStream(conn.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}