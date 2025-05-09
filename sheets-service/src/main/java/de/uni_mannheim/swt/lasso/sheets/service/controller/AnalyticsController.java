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

import de.uni_mannheim.swt.lasso.sheets.service.srh.InMemorySRH;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;

/**
 * LASSO's Analytics API.
 * 
 * @author Marcus Kessel
 *
 */
@RestController
@RequestMapping(value = "/api/v1/srm")
@Tag(name = "SRM API")
public class AnalyticsController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(AnalyticsController.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    InMemorySRH inMemorySRH;

    @Operation(summary = "Get SRM as Parquet", description = "Get SRM as Parquet")
    @RequestMapping(value = "/{executionId}.parquet", method = RequestMethod.GET, produces = "application/vnd.apache.parquet")
    @ResponseBody
    public ResponseEntity<Resource> getSrmParquet(
            @PathVariable("executionId") String executionId,
            /*@ApiIgnore*/ /*@AuthenticationPrincipal UserDetails userDetails,*/
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        // get user details
        //UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            Path path = inMemorySRH.toParquet(executionId);
            Resource resource = new UrlResource(path.toUri());
            //
            String fileName = "srm_" + executionId + ".parquet";

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
}
