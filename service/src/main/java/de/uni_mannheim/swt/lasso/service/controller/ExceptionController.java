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

import de.uni_mannheim.swt.lasso.core.dto.ErrorDetails;
import de.uni_mannheim.swt.lasso.service.app.config.security.InvalidJwtAuthenticationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;

/**
 * Default exception handling for controllers.
 *
 * @author Marcus Kessel
 */
@ControllerAdvice
@RestController
public class ExceptionController extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory
            .getLogger(ExceptionController.class);

    @ExceptionHandler(Throwable.class)
    public final ResponseEntity<ErrorDetails> handleAllExceptions(Throwable ex, WebRequest request) {
        // unauthorized?
        if(ex instanceof InvalidJwtAuthenticationException || ex instanceof BadCredentialsException) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Returning unauthorized => \n{}", ExceptionUtils.getStackTrace(ex));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(),
                request.getDescription(false), ExceptionUtils.getStackTrace(ex));

        if(LOG.isDebugEnabled()) {
            LOG.debug("Returning with exception => \n{}", ExceptionUtils.getStackTrace(ex));
        }

        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

//    @ExceptionHandler(SomeOtherException.class)
//    public final ResponseEntity<ErrorDetails> handleUserNotFoundException(SomeOtherException ex, WebRequest request) {
//        ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(),
//                request.getDescription(false));
//        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
//    }

}
