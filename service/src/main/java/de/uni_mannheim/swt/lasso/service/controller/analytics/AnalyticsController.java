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
package de.uni_mannheim.swt.lasso.service.controller.analytics;

import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import de.uni_mannheim.swt.lasso.service.controller.BaseApi;
import de.uni_mannheim.swt.lasso.srm.olap.Warehouse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * LASSO's Analytics API.
 * 
 * @author Marcus Kessel
 *
 */
@RestController
@RequestMapping(value = "/publicapi/v1/lasso/analytics")
@Tag(name = "Analytics API")
public class AnalyticsController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(AnalyticsController.class);

    @Autowired
    private Environment env;
    @Autowired
    private LassoConfiguration lassoConfiguration;

    @Autowired
    private ApplicationContext applicationContext;

    @Operation(summary = "Get SRM as Parquet", description = "Get SRM as Parquet")
    @RequestMapping(value = "/srm/{type}/{executionId}.parquet", method = RequestMethod.GET, produces = "application/vnd.apache.parquet")
    @ResponseBody
    public ResponseEntity<Resource> getSrmParquet(
            @PathVariable("type") String type,
            @PathVariable("executionId") String executionId,
            /*@ApiIgnore*/ /*@AuthenticationPrincipal UserDetails userDetails,*/
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        // get user details
        //UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            Resource resource = Warehouse.writeSrmResource(executionId, type);
            //
            String fileName = "srm_" + executionId + "_" + type + ".parquet";

            LOG.info(fileName);

            String contentType = "application/vnd.apache.parquet";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get SRM for '%s'", executionId), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get SRM for '%s'", executionId), e);
        }
    }

    @Operation(summary = "Generate Jupyter notebook", description = "Generate Jupyter notebook")
    @RequestMapping(value = "/srm/{type}/{executionId}.ipynb", method = RequestMethod.GET, produces = "application/x-ipynb+json")
    @ResponseBody
    public ResponseEntity<Resource> getJupyterParquet(
            @PathVariable("type") String type,
            @PathVariable("executionId") String executionId,
            /*@ApiIgnore*/ /*@AuthenticationPrincipal UserDetails userDetails,*/
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        // get user details
        //UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            String parquetFile = executionId + ".parquet";

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/publicapi/v1/lasso/analytics/srm/" + type + "/")
                            .path(parquetFile)
                            .toUriString();

            String notebook = JupyterUtils.createSrmNotebook(fileDownloadUri);

            Resource resource = new ByteArrayResource(notebook.getBytes());

            //
            String fileName = "srm_" + executionId + "_" + type + ".ipynb";

            LOG.info(fileName);
            String contentType = "application/x-ipynb+json";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get SRM for '%s'", executionId), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get SRM for '%s'", executionId), e);
        }
    }
}
