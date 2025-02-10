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

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.sheets.service.cut.CodeGeneration;
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
import java.util.Arrays;
import java.util.List;

/**
 * CUT API.
 * 
 * @author Marcus Kessel
 *
 */
@RestController
@RequestMapping(value = "/api/v1/cut")
@Tag(name = "Cut Core API")
public class CutController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(CutController.class);

    @Autowired
    CodeSearch codeSearch;
    @Autowired
    CodeGeneration codeGeneration;

    /**
     *
     *
     * @param request
     *            {@link ClassUnderTestSpec} instance
     * @param httpServletRequest
     *            {@link HttpServletRequest} instance
     * @return {@link ResponseEntity} having a status and in case of success a
     *         {@link InterfaceSpecificationResponse} body set
     */
    @Operation(summary = "Code Search", description = "Code Search")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    public ResponseEntity<CodeSearchResponse> search(
            @RequestBody CodeSearchRequest request,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        if (LOG.isInfoEnabled()) {
            LOG.info("Received code search request from '{}':\n{}",
                    userInfo.getRemoteIpAddress(),
                    ToStringBuilder
                            .reflectionToString(request));
        }

        // do something
        try {
            // response
            // retrieved classes
            List<ClassUnderTest> classesUnderTest = codeSearch.queryForClassesDirectly(request.getInterfaceSpecification(), request.getLimit(), "class"); // retrieve classes

            List<ClassUnderTestSpec> cuts = classesUnderTest.stream().map(cut -> {
                ClassUnderTestSpec classUnderTestSpec = new ClassUnderTestSpec();
                classUnderTestSpec.setId(cut.getId());
                classUnderTestSpec.setClassName(cut.getClassName());
                classUnderTestSpec.setArtifacts(Arrays.asList(cut.getImplementation().getCode().toUri()));
                classUnderTestSpec.setCodeUnit(cut.getImplementation().getCode());

                return classUnderTestSpec;
            }).toList();

            CodeSearchResponse response = new CodeSearchResponse();
            response.setClassResults(cuts);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning code search response to '{}':\n{}",
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
                                "code search failed for '%s':\n %s",
                                userInfo.getRemoteIpAddress(),
                                ToStringBuilder
                                        .reflectionToString(request)),
                        e);
            }

            throw new RuntimeException("code search failed for '"
                    + userInfo.getRemoteIpAddress()
                    + "' "
                    + ToStringBuilder
                    .reflectionToString(request),
                    e);
        }
    }

    /**
     *
     *
     * @param request
     *            {@link ClassUnderTestSpec} instance
     * @param httpServletRequest
     *            {@link HttpServletRequest} instance
     * @return {@link ResponseEntity} having a status and in case of success a
     *         {@link InterfaceSpecificationResponse} body set
     */
    @Operation(summary = "Code Generation", description = "Code Generation")
    @RequestMapping(value = "/generate", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    public ResponseEntity<CodeGenerationResponse> generate(
            @RequestBody CodeGenerationRequest request,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        if (LOG.isInfoEnabled()) {
            LOG.info("Received code generation request from '{}':\n{}",
                    userInfo.getRemoteIpAddress(),
                    ToStringBuilder
                            .reflectionToString(request));
        }

        // do something
        try {
            // response
            List<ClassUnderTestSpec> cuts = codeGeneration.promptOllama(request);

            CodeGenerationResponse response = new CodeGenerationResponse();
            response.setClassResults(cuts);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning code generation response to '{}':\n{}",
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
                                "code generation failed for '%s':\n %s",
                                userInfo.getRemoteIpAddress(),
                                ToStringBuilder
                                        .reflectionToString(request)),
                        e);
            }

            throw new RuntimeException("code generation failed for '"
                    + userInfo.getRemoteIpAddress()
                    + "' "
                    + ToStringBuilder
                    .reflectionToString(request),
                    e);
        }
    }
}
