package de.uni_mannheim.swt.lasso.sheets.service.controller;

import de.uni_mannheim.swt.lasso.core.dto.ErrorDetails;
import de.uni_mannheim.swt.lasso.sheets.service.config.security.InvalidJwtAuthenticationException;
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
