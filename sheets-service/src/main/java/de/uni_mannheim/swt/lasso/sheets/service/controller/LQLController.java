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
package de.uni_mannheim.swt.lasso.sheets.service.controller;

import de.uni_mannheim.swt.lasso.sheets.service.cut.InterfaceGeneration;
import de.uni_mannheim.swt.lasso.sheets.service.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * LQL API.
 * 
 * @author Marcus Kessel
 *
 */
@RestController
@RequestMapping(value = "/api/v1/lql")
@Tag(name = "LQL Core API")
public class LQLController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(LQLController.class);

    @Autowired
    InterfaceGeneration interfaceGeneration;

    /**
     *
     *
     * @param request
     *            {@link LQLGenerationRequest} instance
     * @param httpServletRequest
     *            {@link HttpServletRequest} instance
     * @return {@link ResponseEntity} having a status and in case of success a
     *         {@link LQLGenerationResponse} body set
     */
    @Operation(summary = "Code Generation", description = "Code Generation")
    @RequestMapping(value = "/generate", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    public ResponseEntity<LQLGenerationResponse> generate(
            @RequestBody LQLGenerationRequest request,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        if (LOG.isInfoEnabled()) {
            LOG.info("Received lql generation request from '{}':\n{}",
                    userInfo.getRemoteIpAddress(),
                    ToStringBuilder
                            .reflectionToString(request));
        }

        // do something
        try {
            // response
            String lql = interfaceGeneration.promptOllama(request);

            LQLGenerationResponse response = new LQLGenerationResponse();
            response.setLql(lql);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning lql generation response to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(response));
            }

            // return 200
            return ResponseEntity.ok(response);
        } catch (Throwable e) {
            // warn
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format(
                                "lql generation failed for '%s':\n %s",
                                userInfo.getRemoteIpAddress(),
                                ToStringBuilder
                                        .reflectionToString(request)),
                        e);
            }

            throw new RuntimeException("lql generation failed for '"
                    + userInfo.getRemoteIpAddress()
                    + "' "
                    + ToStringBuilder
                    .reflectionToString(request),
                    e);
        }
    }
}
