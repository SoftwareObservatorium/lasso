/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.cluster.rest;

import de.uni_mannheim.swt.lasso.core.dto.*;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewItem;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewRequest;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewResponse;
import org.apache.commons.collections4.CollectionUtils;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Integration tests for LASSO's worker RESTful interface.
 * 
 * @author Marcus Kessel
 *
 * @see LassoWorkerClient
 */
public class LassoWorkerClientIntegrationTest {

    private static final Logger LOG = LoggerFactory
            .getLogger(LassoWorkerClientIntegrationTest.class);

    // dev: base URI
    private String baseURI = "http://localhost:9988/";

    // new authorization approach
    private String user = "ladmin";
    private String pass = "dj290djfj93f0jd3";

    private static Auth auth(String user, String pass) {
        Auth auth = new Auth();
        auth.setUser(user);
        auth.setPassword(pass);

        return auth;
    }

    @Test
    public void test_getFiles() throws IOException, ClientException, InterruptedException {
        //
        LassoWorkerClient lassoClient = new LassoWorkerClient(baseURI, auth(user, pass));

        FileViewRequest request = new FileViewRequest();
        request.setFilePatterns(Arrays.asList("*.csv", "**/*.java", "**/*.xml"));

        String executionId = "91e27b6a-fe58-4980-8a2b-13528983ff5a";

        FileViewResponse response = lassoClient.getFiles(executionId, request);

        FileViewItem root = response.getRoot();

        debugTree(root, 0);
    }

    void debugTree(FileViewItem item, int depth) {
        System.out.println(StringUtils.repeat(' ', depth) + item.getText() + " => " + item.getValue());

        if (CollectionUtils.isNotEmpty(item.getChildren())) {
            item.getChildren().forEach(c -> debugTree(c, depth + 1));
        }
    }

    @Test
    public void test_downloadRecords() throws IOException, ClientException, InterruptedException {
        //
        LassoWorkerClient lassoClient = new LassoWorkerClient(baseURI, auth(user, pass));

        // -- execute
        RecordsRequest recordsRequest = new RecordsRequest();
        // download all CSV records
        recordsRequest.setFilePatterns(Arrays.asList("ADAPTERREPORT_jacocoRef.csv"));

        // e.g., download all surefire reports generated: "**/TEST-*.xml"

        String executionId = "91e27b6a-fe58-4980-8a2b-13528983ff5a";

        File toFile = new File("/tmp/" + System.currentTimeMillis() + ".zip");
        lassoClient.downloadFile(executionId, recordsRequest, toFile);

        assertTrue(toFile.exists());
    }
}
