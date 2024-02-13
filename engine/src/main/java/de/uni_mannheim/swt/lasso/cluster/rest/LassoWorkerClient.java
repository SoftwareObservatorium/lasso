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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.core.dto.*;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewRequest;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Lasso Worker client.
 * 
 * @author Marcus Kessel
 *
 */
public class LassoWorkerClient extends RestfulClient {

    private static final Logger LOG = LoggerFactory
            .getLogger(LassoWorkerClient.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor
     *
     * @param baseURI
     *            Base URI
     *            @param auth {@link Auth} authentication
     */
    public LassoWorkerClient(String baseURI, Auth auth) {
        super(baseURI, auth);
    }

    public RecordsResponse getRecords(String executionId, RecordsRequest request) throws IOException, ClientException {
        try {
            ResponseEntity<RecordsResponse> responseEntity = getRestTemplate()
                    .postForEntity(getURI("/api/v1/lasso/worker/scripts/{executionId}/records"), request,
                            RecordsResponse.class, executionId);

            // check status
            checkStatus(responseEntity);

            return responseEntity.getBody();
        } catch (HttpServerErrorException e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn(String.format("executionStatus failed =>\n %s", e.getResponseBodyAsString()));
            }

            throw new ClientException(objectMapper.readValue(e.getResponseBodyAsString(), ErrorDetails.class));
        }
    }

    public FileViewResponse getFiles(String executionId, FileViewRequest request) throws IOException, ClientException {
        try {
            ResponseEntity<FileViewResponse> responseEntity = getRestTemplate()
                    .postForEntity(getURI("/api/v1/lasso/worker/scripts/{executionId}/files"), request,
                            FileViewResponse.class, executionId);

            // check status
            checkStatus(responseEntity);

            return responseEntity.getBody();
        } catch (HttpServerErrorException e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn(String.format("executionStatus failed =>\n %s", e.getResponseBodyAsString()));
            }

            throw new ClientException(objectMapper.readValue(e.getResponseBodyAsString(), ErrorDetails.class));
        }
    }

    /**
     * Download records.
     *
     * @param executionId
     * @param request
     * @param toFile
     * @throws IOException
     * @throws ClientException
     */
    public void downloadAsZIP(String executionId, RecordsRequest request, File toFile) throws IOException, ClientException {
        try {
            // search
            ResponseEntity<byte[]> responseEntity = getRestTemplate()
                    .postForEntity(getURI("/api/v1/lasso/worker/scripts/{executionId}/records/download"), request,
                            byte[].class, executionId);

            Files.write(toFile.toPath(), responseEntity.getBody());
        } catch (HttpServerErrorException e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn(String.format("execute failed =>\n %s", e.getResponseBodyAsString()));
            }

            throw new ClientException(objectMapper.readValue(e.getResponseBodyAsString(), ErrorDetails.class));
        }
    }

    public void downloadFile(String executionId, RecordsRequest request, File toFile) throws IOException, ClientException {
        try {
            // search
            ResponseEntity<byte[]> responseEntity = getRestTemplate()
                    .postForEntity(getURI("/api/v1/lasso/worker/scripts/{executionId}/records/file"), request,
                            byte[].class, executionId);

            Files.write(toFile.toPath(), responseEntity.getBody());
        } catch (HttpServerErrorException e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn(String.format("execute failed =>\n %s", e.getResponseBodyAsString()));
            }

            throw new ClientException(objectMapper.readValue(e.getResponseBodyAsString(), ErrorDetails.class));
        }
    }
}
