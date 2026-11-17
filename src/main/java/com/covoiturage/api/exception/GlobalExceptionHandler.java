package com.covoiturage.api.exception;

import com.covoiturage.api.dto.response.ApiMessageResponse;
import com.covoiturage.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiMessageResponse> handleStatus(ResponseStatusException ex) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(ex.getStatusCode()).headers(ex.getHeaders());
        if (ex.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            response.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").cacheControl(CacheControl.noStore());
        }
        return response.body(new ApiMessageResponse(ex.getReason()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiMessageResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiMessageResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessageResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .orElse("Requête invalide.");
        return ResponseEntity.badRequest().body(new ApiMessageResponse(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiMessageResponse> handleInvalidBody() {
        return ResponseEntity.badRequest().body(new ApiMessageResponse("Corps de requête invalide."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiMessageResponse> handleOther(Exception ex) {
        LOGGER.error("Erreur interne lors du traitement d'une requête.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiMessageResponse("Erreur interne."));
    }
}
