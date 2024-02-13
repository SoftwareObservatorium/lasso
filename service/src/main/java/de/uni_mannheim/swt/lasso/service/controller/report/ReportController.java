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
import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import de.uni_mannheim.swt.lasso.core.dto.ReportRequest;

import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.service.controller.BaseApi;
import de.uni_mannheim.swt.lasso.service.dto.UserInfo;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.List;

/**
 * Lasso Report API.
 *
 * @author Marcus Kessel
 */
@RestController
@RequestMapping(value = "/api/v1/lasso/report")
//@Api("Report API")
public class ReportController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(ReportController.class);

    @Autowired
    ClusterEngine clusterEngine;

    @Autowired
    private Environment env;

    ObjectMapper objectMapper = new ObjectMapper();

    //@ApiOperation(value = "Get cached report", notes = "Get cached report")
    @RequestMapping(value = "/{executionId}/{reportId}/{actionId}/{abstractionId}/{implementationId}/{dataSourceId}/{permId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public StreamingResponseBody getCachedReport(
            @PathVariable("executionId") String executionId,
            @PathVariable("reportId") String reportId,
            @PathVariable("actionId") String actionId,
            @PathVariable("abstractionId") String abstractionId,
            @PathVariable("implementationId") String implementationId,
            @PathVariable("dataSourceId") String dataSourceId,
            @PathVariable("permId") int permId,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            // construct key
            ReportKey reportKey = ReportKey.of(actionId, abstractionId, implementationId, dataSourceId, permId);
            // determine report class
            Class<? extends LassoReport> reportClazz = clusterEngine.getReportRepository().toLassoReportClass(executionId, reportId);
            // get cached report
            Object lassoReport = clusterEngine.getReportRepository().get(executionId, reportKey, reportClazz);

            //
            Object report = lassoReport;
            StreamingResponseBody response = outputStream -> {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, report);
            };

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning getCachedReport to '{}'",
                        userInfo.getRemoteIpAddress());
            }

            // 200
            return response;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not getCachedReport for '%s'", executionId ), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not getCachedReport for '%s'", executionId ), e);
        }
    }

    //@ApiOperation(value = "Get report tables", notes = "Get all report tables")
    @RequestMapping(value = "/{executionId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<List<String>> getReportTables(
            @PathVariable("executionId") String executionId,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            //
            List<String> tables = clusterEngine.getReportRepository().getTables(executionId);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning getReportTables to '{}'",
                        userInfo.getRemoteIpAddress());
            }

            // 200
            return ResponseEntity.ok(tables);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not getReportTables for '%s'", executionId ), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not getReportTables for '%s'", executionId ), e);
        }
    }

    //@ApiOperation(value = "Query report", notes = "Query report for given execution id")
    @RequestMapping(value = "/{executionId}", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public StreamingResponseBody queryReport(
            @RequestBody ReportRequest reportRequest,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("executionId") String executionId,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {
            StreamingResponseBody response = outputStream -> {
                try {
                    String tableName = StringUtils.substringBetween(reportRequest.getSql() + " ", "from ", " ");
                    tableName = StringUtils.trimToEmpty(tableName);

                    if (LOG.isInfoEnabled()) {
                        LOG.info("SQL query for table '{}' => '{}'", tableName, reportRequest.getSql());
                    }

                    if(StringUtils.containsIgnoreCase(tableName, "srm")) {
                        selectSRM(reportRequest.getSql(), outputStream);
                    } else {
                        select(executionId, tableName, reportRequest.getSql(), outputStream);
                    }

//                    JdbcTemplate jdbcTemplate = clusterEngine.getJDBC().getJdbcTemplate();
//                    jdbcTemplate.query(reportRequest.getSql(), new StreamingJsonResultSetExtractor(outputStream));
                } catch (SQLException e) {
                    throw new IOException(e);
                }
            };

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning getReport response to '{}'",
                        userInfo.getRemoteIpAddress());
            }

            // 200
            return response;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get report response"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get report response"), e);
        }
    }

    private void select(String executionId, String table, String sql, OutputStream out) throws SQLException {
        JdbcTemplate jdbcTemplate = clusterEngine.getReportRepository().getJdbcTemplate(executionId, table);

        jdbcTemplate.query(sql, new StreamingJsonResultSetExtractor(out));
    }

    private void selectSRM(String sql, OutputStream out) throws SQLException {
        JdbcTemplate jdbcTemplate = clusterEngine.getClusterSRMRepository().getJdbcTemplate();

        jdbcTemplate.query(sql, new StreamingJsonResultSetExtractor(out));
    }

    /**
     * taken from here: https://www.javacodegeeks.com/2018/09/streaming-jdbc-resultset-json.html
     */
    public class StreamingJsonResultSetExtractor implements ResultSetExtractor<Void> {

        private final OutputStream os;

        public StreamingJsonResultSetExtractor(final OutputStream os) {
            this.os = os;
        }

        @Override
        public Void extractData(final ResultSet rs) {
            try (JsonGenerator jg = objectMapper.getFactory().createGenerator(
                    os, JsonEncoding.UTF8)) {
                writeResultSetToJson(rs, jg);
                jg.flush();
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    private static void writeResultSetToJson(final ResultSet rs,
                                             final JsonGenerator jg)
            throws SQLException, IOException {
        final ResultSetMetaData rsmd = rs.getMetaData();
        final int columnCount = rsmd.getColumnCount();
        jg.writeStartArray();
        while (rs.next()) {
            jg.writeStartObject();
            for (int i = 1; i <= columnCount; i++) {
                jg.writeObjectField(rsmd.getColumnName(i), rs.getObject(i));
            }
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }
}
