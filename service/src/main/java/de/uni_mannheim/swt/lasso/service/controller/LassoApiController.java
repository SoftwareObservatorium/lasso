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
package de.uni_mannheim.swt.lasso.service.controller;

import de.uni_mannheim.swt.lasso.core.dto.*;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewRequest;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewResponse;
import de.uni_mannheim.swt.lasso.engine.dag.model.LGraph;
import de.uni_mannheim.swt.lasso.service.LassoManager;
import de.uni_mannheim.swt.lasso.service.dto.ScriptInfo;
import de.uni_mannheim.swt.lasso.service.dto.UserInfo;
import de.uni_mannheim.swt.lasso.service.persistence.ScriptJobRepository;
import de.uni_mannheim.swt.lasso.service.persistence.User;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Lasso API.
 * 
 * @author Marcus Kessel
 *
 */
@RestController
@RequestMapping(value = "/api/v1/lasso")
@Tag(name = "LASSO Core API")
public class LassoApiController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(LassoApiController.class);

    @Autowired
    LassoManager lassoManager;

    @Autowired
    ScriptJobRepository scriptJobRepository;

    /**
     * Execute LSL based on given {@link LSLRequest}
     * 
     * @param request
     *            {@link LSLRequest} instance
     * @param httpServletRequest
     *            {@link HttpServletRequest} instance
     * @return {@link ResponseEntity} having a status and in case of success a
     *         {@link LSLResponse} body set
     */
    @Operation(summary = "Execute LSL Script", description = "Execute LSL Script")
    @RequestMapping(value = "/execute", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    public ResponseEntity<LSLResponse> execute(
            @RequestBody LSLRequest request,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        if (LOG.isInfoEnabled()) {
            LOG.info("Received LSL execution request from '{}':\n{}",
                    userInfo.getRemoteIpAddress(),
                    ToStringBuilder
                            .reflectionToString(request));
        }

        // do something
        try {
            // response
            LSLResponse response = lassoManager.execute(request, userInfo);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning LSL execution response to '{}':\n{}",
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
                        "LSL execution failed for '%s':\n %s",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(request)),
                e);
            }

            throw new RuntimeException("LSL execution failed for '"
                    + userInfo.getRemoteIpAddress()
                    + "' "
                    + ToStringBuilder
                    .reflectionToString(request),
                    e);
        }
    }

    /**
     * Extract graph from script (by evaluation)
     *
     * @param request
     *            {@link LSLRequest} instance
     * @param httpServletRequest
     *            {@link HttpServletRequest} instance
     * @return {@link ResponseEntity} having a status and in case of success a
     *         {@link LSLResponse} body set
     */
    @Operation(summary = "Transform LSL Script", description = "Transform LSL Script into Graph")
    @RequestMapping(value = "/graph", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    public ResponseEntity<LGraph> graph(
            @RequestBody LSLRequest request,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        if (LOG.isInfoEnabled()) {
            LOG.info("Received LSL graph request from '{}':\n{}",
                    userInfo.getRemoteIpAddress(),
                    ToStringBuilder
                            .reflectionToString(request));
        }

        // do something
        try {
            // response
            LGraph response = lassoManager.graph(request, userInfo);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning LSL graph response to '{}':\n{}",
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
                                "LSL graph failed for '%s':\n %s",
                                userInfo.getRemoteIpAddress(),
                                ToStringBuilder
                                        .reflectionToString(request)),
                        e);
            }

            throw new RuntimeException("LSL graph failed for '"
                    + userInfo.getRemoteIpAddress()
                    + "' "
                    + ToStringBuilder
                    .reflectionToString(request),
                    e);
        }
    }

    @Operation(summary = "LSL Execution Result", description = "Get LSL Script Execution Result")
    @RequestMapping(value = "/scripts/{executionId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity<ExecutionResult> executionResult(
            @PathVariable("executionId") String executionId,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        if (LOG.isInfoEnabled()) {
            LOG.info("Received LSL execution result request from '{}':\n{}",
                    userInfo.getRemoteIpAddress(),
                    ToStringBuilder
                            .reflectionToString(executionId));
        }

        // do something
        try {
            // response
            ExecutionResult response = lassoManager.getExecutionResult(executionId, userInfo);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning LSL execution result response to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(response));
            }

            // return 200
            return ResponseEntity.ok(response);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get execution result for '%s'", executionId ), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get execution result for '%s'", executionId ), e);
        }
    }

    @Operation(summary = "LSL Execution Status", description = "Get LSL Script Execution Status")
    @RequestMapping(value = "/scripts/{executionId}/status", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<ExecutionStatus> getExecutionStatus(
            @PathVariable("executionId") String executionId,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            // query for execution status
            ExecutionStatus executionStatus = lassoManager.getExecutionStatus(executionId, userInfo);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning LSL execution status to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(executionStatus));
            }

            // 200
            return ResponseEntity.ok(executionStatus);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get execution status for '%s'", executionId ), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get execution status for '%s'", executionId ), e);
        }
    }

    @Operation(summary = "LSL Records Listing", description = "Get listing of all matching records")
    @RequestMapping(value = "/scripts/{executionId}/records", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @ResponseBody
    @Deprecated
    public ResponseEntity<RecordsResponse> getRecords(
            @PathVariable("executionId") String executionId,
            @RequestBody RecordsRequest request,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
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
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
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
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
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
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
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

    @Operation(summary = "LSL Info", description = "Get LSL Info about Available Actions etc.")
    @RequestMapping(value = "/info", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<LSLInfoResponse> getLSLInfo(
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            //
            LSLInfoResponse lslInfoResponse = lassoManager.getLSLInfo(userInfo);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning LSL info response to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(lslInfoResponse));
            }

            // 200
            return ResponseEntity.ok(lslInfoResponse);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get LSL info"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get LSL info"), e);
        }
    }

    @Operation(summary = "Scripts", description = "Get Scripts")
    @RequestMapping(value = "/scripts", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<List<ScriptInfo>> getScripts(
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            // by owner
            User owner = (User) userDetails;
            // TODO add paging in UI add parameters page + size
            Pageable pageable = PageRequest.of(0, 1000, Sort.by("start").descending());

            List<ScriptInfo> scriptInfos = scriptJobRepository.findAllByOwner(owner, pageable).stream().map(s -> {
                ScriptInfo scriptInfo = new ScriptInfo();
                scriptInfo.setExecutionId(s.getExecutionId());
                scriptInfo.setName(s.getName());
                scriptInfo.setOwner(s.getOwner().getUsername());
                scriptInfo.setStatus(s.getStatus().name());
                scriptInfo.setStart(s.getStart());
                scriptInfo.setEnd(s.getEnd());
                scriptInfo.setContent(s.getContent());

                return scriptInfo;
            }).collect(Collectors.toList());

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning script infos response to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(scriptInfos));
            }

            // 200
            return ResponseEntity.ok(scriptInfos);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get script infos"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get script infos"), e);
        }
    }

    @Operation(summary = "Shared Scripts", description = "Get Shared Scripts")
    @RequestMapping(value = "/scripts/shared", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<List<ScriptInfo>> getSharedScripts(
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            // by owner
            User owner = (User) userDetails;
            // TODO add paging in UI add parameters page + size
            Pageable pageable = PageRequest.of(0, 1000, Sort.by("start").descending());

            List<ScriptInfo> scriptInfos = scriptJobRepository.findAllByShared(true, pageable).stream().map(s -> {
                ScriptInfo scriptInfo = new ScriptInfo();
                scriptInfo.setExecutionId(s.getExecutionId());
                scriptInfo.setName(s.getName());
                scriptInfo.setOwner(s.getOwner().getUsername());
                scriptInfo.setStatus(s.getStatus().name());
                scriptInfo.setStart(s.getStart());
                scriptInfo.setEnd(s.getEnd());
                scriptInfo.setContent(s.getContent());

                return scriptInfo;
            }).collect(Collectors.toList());

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning script infos response to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(scriptInfos));
            }

            // 200
            return ResponseEntity.ok(scriptInfos);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get script infos"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get script infos"), e);
        }
    }
}
