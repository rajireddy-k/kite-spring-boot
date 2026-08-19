package com.example.kite.exception;

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessage> handleRuntimeException(RuntimeException ex) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatusCode("500");
        errorMessage.setDescription(ex.getMessage());
        return ResponseEntity.status(500).body(errorMessage);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorMessage> handleValidationException(ValidationException ex) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatusCode("401");
        errorMessage.setDescription(ex.getMessage());
        return ResponseEntity.status(401).body(errorMessage);
    }

    @ExceptionHandler(KiteClientException.class)
    public ResponseEntity<ErrorMessage> handleKiteException(KiteClientException ex) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatusCode("501");
        errorMessage.setDescription(ex.getMessage());
        return ResponseEntity.status(501).body(errorMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessage> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatusCode("404");
        errorMessage.setDescription(ex.getMessage());
        return ResponseEntity.status(404).body(errorMessage);
    }

}
