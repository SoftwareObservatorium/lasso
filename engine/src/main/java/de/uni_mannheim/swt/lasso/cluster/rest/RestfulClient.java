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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Candidate search client including textual as well as test-driven search
 * support.
 * 
 * Currently, only Basic Auth implemented.
 * 
 * @author Marcus Kessel
 *
 */
public abstract class RestfulClient {

    private static final Logger LOG = LoggerFactory
            .getLogger(RestfulClient.class);

    private final String baseURI;
    private final Auth auth;

    /**
     * Springs RESTful client for testing our RESTful service
     */
    private RestTemplate restTemplate;

    /**
     * Constructor
     * 
     * @param baseURI
     *            Base URI
     * @param auth
     *            {@link Auth} authentication
     */
    public RestfulClient(String baseURI, Auth auth) {
        Validate.notBlank(baseURI, "Base URI cannot be blank");

        this.baseURI = StringUtils.removeEnd(baseURI, "/");
        this.auth = auth;

        // init resttemplate with Basic Auth header
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
        interceptors.add(new ClientHttpRequestInterceptor() {

            @Override
            public ClientHttpResponse intercept(HttpRequest request,
                                                byte[] body, ClientHttpRequestExecution execution)
                    throws IOException {
                HttpRequest wrapper = new HttpRequestWrapper(request);

                String auth = String.format("%s:%s", getAuth().getUser(), getAuth().getPassword());
                byte[] encodedAuth = Base64.encodeBase64(
                        auth.getBytes(StandardCharsets.US_ASCII));

                wrapper.getHeaders().set("Authorization", String.format("Basic %s", new String(encodedAuth)));
                return execution.execute(wrapper, body);
            }
        });

        this.restTemplate = new RestTemplate();
        this.restTemplate.setInterceptors(interceptors);
    }

    /**
     * @param path
     *            resource path
     * @return full URI
     */
    protected String getURI(String path) {
        return baseURI + path;
    }

    /**
     * @param responseEntity
     *            {@link ResponseEntity} instance
     * @throws IOException
     *             if status not within 2XX range
     */
    protected void checkStatus(ResponseEntity<?> responseEntity)
            throws IOException {
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new IOException("HTTP response status: "
                    + responseEntity.getStatusCode().getReasonPhrase());
        }
    }

    /**
     * @return the restTemplate
     */
    protected RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public Auth getAuth() {
        return auth;
    }
}
