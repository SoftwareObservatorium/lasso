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

import de.uni_mannheim.swt.lasso.sheets.service.SheetsManager;
import de.uni_mannheim.swt.lasso.sheets.service.dto.*;
import de.uni_mannheim.swt.lasso.sheets.service.persistence.SheetJobRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Sheet API.
 * 
 * @author Marcus Kessel
 *
 */
@RestController
@RequestMapping(value = "/api/v1/sheet")
@Tag(name = "Sheet Core API")
public class SheetApiController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(SheetApiController.class);

    @Autowired
    SheetsManager sheetsManager;

    @Autowired
    SheetJobRepository sheetJobRepository;

    /**
     * Execute Sheet based on given {@link SheetRequest}
     * 
     * @param request
     *            {@link SheetRequest} instance
     * @param httpServletRequest
     *            {@link HttpServletRequest} instance
     * @return {@link ResponseEntity} having a status and in case of success a
     *         {@link SheetResponse} body set
     */
    @Operation(summary = "Execute Sheet", description = "Execute Sheet")
    @RequestMapping(value = "/execute", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    public ResponseEntity<SheetResponse> execute(
            @RequestBody SheetRequest request,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        if (LOG.isInfoEnabled()) {
            LOG.info("Received sheet execution request from '{}':\n{}",
                    userInfo.getRemoteIpAddress(),
                    ToStringBuilder
                            .reflectionToString(request));
        }

        // do something
        try {
            // response
            SheetResponse response = sheetsManager.execute(request, userInfo);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning sheet execution response to '{}':\n{}",
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
                        "sheet execution failed for '%s':\n %s",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(request)),
                e);
            }

            throw new RuntimeException("sheet execution failed for '"
                    + userInfo.getRemoteIpAddress()
                    + "' "
                    + ToStringBuilder
                    .reflectionToString(request),
                    e);
        }
    }

    /**
     * Get interface specification in LQL for a CUT based on given {@link ClassUnderTestSpec}
     *
     * @param request
     *            {@link ClassUnderTestSpec} instance
     * @param httpServletRequest
     *            {@link HttpServletRequest} instance
     * @return {@link ResponseEntity} having a status and in case of success a
     *         {@link InterfaceSpecificationResponse} body set
     */
    @Operation(summary = "Get interface specification in LQL", description = "Get interface specification in LQL")
    @RequestMapping(value = "/lql", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    public ResponseEntity<InterfaceSpecificationResponse> toLql(
            @RequestBody ClassUnderTestSpec request,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        if (LOG.isInfoEnabled()) {
            LOG.info("Received lql request from '{}':\n{}",
                    userInfo.getRemoteIpAddress(),
                    ToStringBuilder
                            .reflectionToString(request));
        }

        // do something
        try {
            // response
            InterfaceSpecificationResponse response = sheetsManager.toLql(request, userInfo);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning lql response to '{}':\n{}",
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
                                "lql failed for '%s':\n %s",
                                userInfo.getRemoteIpAddress(),
                                ToStringBuilder
                                        .reflectionToString(request)),
                        e);
            }

            throw new RuntimeException("lql failed for '"
                    + userInfo.getRemoteIpAddress()
                    + "' "
                    + ToStringBuilder
                    .reflectionToString(request),
                    e);
        }
    }
}
