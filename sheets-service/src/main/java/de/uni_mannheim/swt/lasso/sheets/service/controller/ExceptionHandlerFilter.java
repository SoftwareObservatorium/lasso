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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.core.dto.ErrorDetails;
import de.uni_mannheim.swt.lasso.sheets.service.config.security.InvalidJwtAuthenticationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * Handle filter exceptions.
 *
 * @author Marcus Kessel
 *
 */
public class ExceptionHandlerFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory
            .getLogger(ExceptionHandlerFilter.class);

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);

        } catch (InvalidJwtAuthenticationException | BadCredentialsException e) {
            // 401
            setErrorResponse(HttpStatus.UNAUTHORIZED, response, e);
        } catch (Throwable e) {
            throw e;
        }
    }

    public void setErrorResponse(HttpStatus status, HttpServletResponse response, Throwable ex){
        response.setStatus(status.value());
        response.setContentType("application/json");

        ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(),
                "", ExceptionUtils.getStackTrace(ex));

        if(LOG.isDebugEnabled()) {
            LOG.debug("Returning with exception => \n{}", ExceptionUtils.getStackTrace(ex));
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(response.getWriter(), errorDetails);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
