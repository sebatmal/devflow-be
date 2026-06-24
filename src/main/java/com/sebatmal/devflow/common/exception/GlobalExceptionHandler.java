package com.sebatmal.devflow.common.exception;

import com.sebatmal.devflow.common.response.APIErrorResponse;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DevflowException.class)
    public ResponseEntity<APIErrorResponse> handleDevflowException(final DevflowException exception) {
        final FailMessage fail = exception.getFailMessage();
        log.warn("[DevflowException] code={}, message={}", fail.getCode(), fail.getMessage());
        return APIErrorResponse.of(fail.getHttpStatus(), fail.getCode(), fail.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIErrorResponse> handleValidation(final MethodArgumentNotValidException exception) {
        final String message = exception.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("\n"));
        final FailMessage fail = FailMessage.BAD_REQUEST_BODY_VALID;
        return APIErrorResponse.of(fail.getHttpStatus(), fail.getCode(), message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<APIErrorResponse> handleMissingParam(final MissingServletRequestParameterException exception) {
        final FailMessage fail = FailMessage.BAD_REQUEST_MISSING_PARAM;
        return APIErrorResponse.of(fail.getHttpStatus(), fail.getCode(), "누락된 파라미터: " + exception.getParameterName());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIErrorResponse> handleGeneral(final Exception exception) {
        final FailMessage fail = FailMessage.INTERNAL_SERVER_ERROR;
        log.error("[Unhandled] {}", exception.getMessage(), exception);
        return APIErrorResponse.of(fail.getHttpStatus(), fail.getCode(), fail.getMessage());
    }
}
