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
package de.uni_mannheim.swt.lasso.service.controller.cluster;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.core.dto.RecordsRequest;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewItem;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewRequest;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewResponse;
import de.uni_mannheim.swt.lasso.service.LassoManager;
import de.uni_mannheim.swt.lasso.service.controller.BaseApi;
import de.uni_mannheim.swt.lasso.service.dto.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * API for LASSO's distributed file service.
 * 
 * @author Marcus Kessel
 *
 */
@RestController
@RequestMapping(value = "/api/v1/lasso/dfs")
@Tag(name = "DFS API")
public class DFSController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(DFSController.class);

    @Autowired
    ClusterEngine clusterEngine;

    @Autowired
    LassoManager lassoManager;

    @Autowired
    private Environment env;

    @Operation(summary = "DFS File Listing", description = "Get listing of all matching files")
    @RequestMapping(value = "/files", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<FileViewResponse> getFiles(
            //@PathVariable("executionId") String executionId, // FIXME currently not evaluated
            @RequestBody FileViewRequest request,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            //
            String path = request.getFilePatterns().get(0);
            List<String> files = clusterEngine.getFileSystem().listFiles(path, 1000); // TODO there is a limit set

            FileViewResponse response = new FileViewResponse();
            FileViewItem root = new FileViewItem();
            root.setText(path);
            root.setValue(path);

            // add children
            files.stream().map(p -> {
                FileViewItem node = new FileViewItem();
                node.setText(p);
                node.setValue(p);
                return node;
            }).forEach(root::addChild);

            response.setRoot(root);

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

    @Operation(summary = "Download file", description = "Get file")
    @RequestMapping(value = "/file", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/octet-stream")
    @ResponseBody
    public StreamingResponseBody getFile(
            //@PathVariable("executionId") String executionId,
            @RequestBody RecordsRequest request,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        String path = request.getFilePatterns().get(0);
        try {

            // return stream
            StreamingResponseBody streamingResponseBody = output -> {
                IOUtils.copy(clusterEngine.getFileSystem().read(path), output);
            };

            //
            String fileName = FilenameUtils.getName(path);

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
                LOG.warn(String.format("Could not get file for '%s'", path ), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get file for '%s'", path ), e);
        }
    }
}
