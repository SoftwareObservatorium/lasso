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
package de.uni_mannheim.swt.lasso.gai.openai;

import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Marcus Kessel
 */
public class OpenAiClient {

    private String apiUrl;
    private String apiKey;

    private RestTemplate restTemplate;

    public OpenAiClient(String apiUrl, String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;

        this.restTemplate = createRestTemplate(apiKey);
    }

    public static RestTemplate createRestTemplate(String apiKey) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("Authorization", "Bearer " + apiKey);
            return execution.execute(request, body);
        });

        return restTemplate;
    }

    public CompletionResponse complete(CompletionRequest request) {
        return restTemplate.postForObject(apiUrl, request, CompletionResponse.class);
    }
}
