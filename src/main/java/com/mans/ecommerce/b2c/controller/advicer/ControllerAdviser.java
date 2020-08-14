package com.mans.ecommerce.b2c.controller.advicer;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mans.ecommerce.b2c.domain.exception.*;
import com.mans.ecommerce.b2c.server.eventListener.entity.ServerErrorEvent;
import com.stripe.exception.StripeException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class ControllerAdviser
{
    private ApplicationEventPublisher publisher;

    ControllerAdviser(ApplicationEventPublisher publisher)
    {
        this.publisher = publisher;
    }

    @ExceptionHandler({ UserAlreadyExistException.class, ConflictException.class })
    public ResponseEntity<Object> handleConflictException(Exception ex)
    {
        String message = ex.getMessage();
        return getResponseMessage(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler({LoginException.class, UnauthorizedException.class})
    public ResponseEntity<Object> handleUnauthorized(Exception ex)
    {
        return getResponseMessage(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({ ResourceNotFoundException.class})
    public ResponseEntity<Object> handleBadRequest(Exception ex)
    {
        return getResponseMessage(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<Object> handleBadRequest(ServerWebInputException ex)
    {
        return getResponseMessage(HttpStatus.BAD_REQUEST, ex.getRootCause().getMessage());
    }

    @ExceptionHandler({ PaymentFailedException.class, StripeException.class })
    public ResponseEntity<Object> handlePaymentError(Exception ex)
    {
        return getResponseMessage(HttpStatus.FAILED_DEPENDENCY, ex.getMessage());
    }

    @ExceptionHandler({ OutOfStockException.class })
    public ResponseEntity<Object> outOfStockException(OutOfStockException ex)
    {
        return getResponseMessage(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({ PartialOutOfStockException.class })
    public ResponseEntity<Object> partialStockException(PartialOutOfStockException ex)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        body.put("cart", ex.getCart());
        return new ResponseEntity<>(body, HttpStatus.PARTIAL_CONTENT);
    }

    @ExceptionHandler({ UncompletedCheckoutException.class })
    public ResponseEntity<Object> uncompletedCheckoutException(UncompletedCheckoutException ex)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        body.put("uncompleted", ex.getUncompleted());
        body.put("checkoutResponse", ex.getCheckoutResponse());
        return new ResponseEntity<>(body, HttpStatus.PARTIAL_CONTENT);
    }

    @ExceptionHandler({ SystemConstraintViolation.class, Exception.class })
    public ResponseEntity<Object> handleSystemConstraint(Exception ex)
    {
        System.err.println(ex.getClass().getName());
        publisher.publishEvent(new ServerErrorEvent(ex));
        return getResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request)
    {
        Map<String, Object> body = new LinkedHashMap<>();

        ex.getBindingResult()
          .getFieldErrors()
          .forEach(fieldError -> {
              String fieldName = fieldError.getField();
              String errorMessage = fieldError.getDefaultMessage();
              body.put(fieldName, errorMessage);
          });

        return new ResponseEntity<>(body, status);
    }

    private ResponseEntity<Object> getResponseMessage(HttpStatus status, String message)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }

}
