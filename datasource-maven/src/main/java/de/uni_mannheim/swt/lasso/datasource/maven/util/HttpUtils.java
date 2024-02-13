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
package de.uni_mannheim.swt.lasso.datasource.maven.util;

import org.apache.commons.lang3.StringUtils;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 *
 * @author Marcus Kessel
 */
public class HttpUtils {

    /**
     * Workaround for BASIC auth with SolrJ.
     *
     * @param solrUser
     * @param solrPass
     * @return
     */
    public static HttpClient createHttpClient(String solrUser, String solrPass) {
        if(StringUtils.isNotBlank(solrUser)) {
            // do basic auth
            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(solrUser, solrPass);
            provider.setCredentials(AuthScope.ANY, credentials);

            HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider)
                    .addInterceptorFirst(new HttpRequestInterceptor() {

                        @Override
                        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                            //
                            byte[] credentials = Base64.getEncoder()
                                    .encode((solrUser + ":" + solrPass).getBytes(StandardCharsets.UTF_8));
                            request.setHeader("Authorization", "Basic " + new String(credentials, StandardCharsets.UTF_8));
                        }
                    }).build();
            return client;
        } else {
            return HttpClientBuilder.create().build();
        }
    }
}
