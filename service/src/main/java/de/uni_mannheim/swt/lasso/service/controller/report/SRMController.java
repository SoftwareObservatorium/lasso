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
package de.uni_mannheim.swt.lasso.service.controller.report;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;

import de.uni_mannheim.swt.lasso.service.controller.BaseApi;
import de.uni_mannheim.swt.lasso.service.dto.UserInfo;
import de.uni_mannheim.swt.lasso.srm.SRMManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import joinery.DataFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lasso SRM API.
 *
 * @author Marcus Kessel
 */
@RestController
@RequestMapping(value = "/api/v1/lasso/srm")
@Tag(name = "LASSO SRM API")
public class SRMController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(SRMController.class);

    @Autowired
    ClusterEngine clusterEngine;

    @Autowired
    private Environment env;

    ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "Get Actuation Sheets", description = "Get Actuation Sheets")
    @RequestMapping(value = "/{executionId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public StreamingResponseBody getActuationSheets(
            @PathVariable("executionId") String executionId,
            @RequestParam(value = "type", required = false) String type,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            StreamingResponseBody response = os -> {
                try {
                    //
                    SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository());
                    // FIXME arena id (let's assume arena "execute" for now)
                    DataFrame df = srmManager.getActuationSheets(executionId, "execute", type, null);

                    try (JsonGenerator jg = objectMapper.getFactory().createGenerator(
                            os, JsonEncoding.UTF8)) {
                        writeDataFrameToJson(df, jg);
                        jg.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                }
            };

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning getActuationSheets response to '{}'",
                        userInfo.getRemoteIpAddress());
            }

            // 200
            return response;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get getActuationSheets response"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get getActuationSheets response"), e);
        }
    }

    @Operation(summary = "Get Actuation Sheets for System", description = "Get Actuation Sheets for given system")
    @RequestMapping(value = "/{executionId}/{systemId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public StreamingResponseBody getActuationSheetsForSystem(
            @PathVariable("executionId") String executionId,
            @PathVariable("systemId") String systemId,
            @RequestParam(value = "type", required = false) String type,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            StreamingResponseBody response = os -> {
                try {
                    //
                    SRMManager srmManager = new SRMManager(clusterEngine.getClusterSRMRepository());
                    // FIXME arena id (let's assume arena "execute" for now)
                    DataFrame df = srmManager.getActuationSheetsForSystem(executionId, "execute", systemId, type);

                    try (JsonGenerator jg = objectMapper.getFactory().createGenerator(
                            os, JsonEncoding.UTF8)) {
                        writeDataFrameToJson(df, jg);
                        jg.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                }
            };

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning getActuationSheets response to '{}'",
                        userInfo.getRemoteIpAddress());
            }

            // 200
            return response;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get getActuationSheets response"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get getActuationSheets response"), e);
        }
    }

    private static void writeDataFrameToJson(final DataFrame df,
                                             final JsonGenerator jg)
            throws IOException {
        jg.writeStartArray();
        Iterator<List> it = df.iterator();
        while (it.hasNext()) {
            List row = it.next();
            jg.writeStartObject();
            for (int i = 0; i < row.size(); i++) {
                jg.writeObjectField((String) new ArrayList<>(df.columns()).get(i), row.get(i));
            }
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }
}
