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
package de.uni_mannheim.swt.lasso.analyzer.config;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.uni_mannheim.swt.lasso.analyzer.index.CompilationUnitRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Solr Configuration
 * 
 * @author Marcus Kessel
 *
 */
@Configuration
public class SolrConfig {

    @Value("${batch.solr.url}")
    private String solrUrl;

    @Value("${batch.solr.user}")
    private String solrUser;

    @Value("${batch.solr.pass}")
    private String solrPass;

    @Value("${batch.solr.core.candidates}")
    private String solrCandidatesCore;

    @Bean
    public SolrClient solrClient() {
        // do basic auth
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(solrUser, solrPass);
        provider.setCredentials(AuthScope.ANY, credentials);

        HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider)
                .addInterceptorFirst(new HttpRequestInterceptor() {

                    @Override
                    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                        //
                        byte[] credentials = Base64
                                .encodeBase64((solrUser + ":" + solrPass).getBytes(StandardCharsets.UTF_8));
                        request.setHeader("Authorization", "Basic " + new String(credentials, StandardCharsets.UTF_8));
                    }
                }).build();

        HttpSolrClient solrServer = new HttpSolrClient.Builder(solrUrl + "/" + solrCandidatesCore)
                .withHttpClient(client).build();

        return solrServer;
    }

    @Bean
    public CompilationUnitRepository compilationUnitRepository(
            SolrClient solrClient) {
        return new CompilationUnitRepository(solrClient);
    }
}
