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
package de.uni_mannheim.swt.lasso.cluster.worker.standalone.controller;

import de.uni_mannheim.swt.lasso.cluster.worker.standalone.LassoManager;
import de.uni_mannheim.swt.lasso.cluster.worker.standalone.dto.UserInfo;
import de.uni_mannheim.swt.lasso.core.dto.*;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewRequest;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Lasso Worker API.
 * 
 * @author Marcus Kessel
 *
 */
@RestController
@RequestMapping(value = "/api/v1/lasso/worker")
@Tag(name = "Lasso Worker API")
public class LassoApiController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(LassoApiController.class);

    @Autowired
    LassoManager lassoManager;

    @Operation(summary = "LSL Records Listing", description = "Get listing of all matching records")
    @RequestMapping(value = "/scripts/{executionId}/records", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @ResponseBody
    @Deprecated
    public ResponseEntity<RecordsResponse> getRecords(
            @PathVariable("executionId") String executionId,
            @RequestBody RecordsRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            //
            RecordsResponse recordsResponse = lassoManager.getRecords(request, executionId, userInfo);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning Records response to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(recordsResponse));
            }

            // 200
            return ResponseEntity.ok(recordsResponse);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get Records response"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get Records response"), e);
        }
    }

    @Operation(summary = "LSL Records as ZIP", description = "Get ZIP file of all matching records")
    @RequestMapping(value = "/scripts/{executionId}/records/download", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/octet-stream")
    @ResponseBody
    public StreamingResponseBody streamRecords(
            @PathVariable("executionId") String executionId,
            @RequestBody RecordsRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            // return ZIP stream
            StreamingResponseBody streamingResponseBody = lassoManager.streamRecords(request, executionId, userInfo);

            httpServletResponse.addHeader("Content-disposition", String.format("attachment;filename=records_%s.zip", executionId));
            httpServletResponse.setContentType("application/octet-stream");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning LSL records to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(streamingResponseBody.getClass()));
            }

            // 200
            return streamingResponseBody;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get records as zip for '%s'", executionId ), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get records as zip for '%s'", executionId ), e);
        }
    }

    @Operation(summary = "Download file", description = "Get file")
    @RequestMapping(value = "/scripts/{executionId}/records/file", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/octet-stream")
    @ResponseBody
    public StreamingResponseBody getCSVFile(
            @PathVariable("executionId") String executionId,
            @RequestBody RecordsRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            // return ZIP stream
            StreamingResponseBody streamingResponseBody = lassoManager.streamCSV(request, executionId, userInfo);

            //
            String fileName = FilenameUtils.getName(request.getFilePatterns().get(0));

            LOG.info(fileName);

            httpServletResponse.addHeader("Content-disposition", String.format("attachment;filename=", fileName));
            httpServletResponse.setContentType("application/octet-stream");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning file to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(streamingResponseBody.getClass()));
            }

            // 200
            return streamingResponseBody;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get file for '%s'", executionId ), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get file for '%s'", executionId ), e);
        }
    }

    @Operation(summary = "Workspace File Listing", description = "Get listing of all matching files")
    @RequestMapping(value = "/scripts/{executionId}/files", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<FileViewResponse> getFiles(
            @PathVariable("executionId") String executionId,
            @RequestBody FileViewRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            //
            FileViewResponse response = lassoManager.getFiles(request, executionId, userInfo);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning Files response to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(response));
            }

            // 200
            return ResponseEntity.ok(response);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get Records response"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get Records response"), e);
        }
    }
}
