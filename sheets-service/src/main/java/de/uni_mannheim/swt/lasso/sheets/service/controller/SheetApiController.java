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
import de.uni_mannheim.swt.lasso.sheets.service.dto.SheetRequest;
import de.uni_mannheim.swt.lasso.sheets.service.dto.SheetResponse;
import de.uni_mannheim.swt.lasso.sheets.service.dto.UserInfo;
import de.uni_mannheim.swt.lasso.sheets.service.persistence.SheetJobRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

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
     * Execute LSL based on given {@link SheetRequest}
     * 
     * @param request
     *            {@link SheetRequest} instance
     * @param httpServletRequest
     *            {@link HttpServletRequest} instance
     * @return {@link ResponseEntity} having a status and in case of success a
     *         {@link SheetResponse} body set
     */
    @Operation(summary = "Execute LSL Script", description = "Execute Sheet")
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
}
