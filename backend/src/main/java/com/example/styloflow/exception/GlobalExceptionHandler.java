package com.example.styloflow.exception;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.hibernate.query.PathException;
import org.hibernate.query.sqm.PathElementException;
import org.hibernate.query.sqm.UnknownPathException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps every error to a ProblemDetail with the titles of the API contract:
 * "Invalid data" (400 + {@code errors}), "Invalid request", "Unauthorized", "Forbidden", "Business suspended",
 * "Not found", "Conflict" and "Business rule" (422).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    static final String INVALID_DATA = "Invalid data";
    private static final String INVALID_DATA_DETAIL = "Check the highlighted fields";
    private static final String INVALID_VALUE = "invalid value";
    private static final String INVALID_SORT_DETAIL = "Unknown sort property";

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException ex) {
        return problem(ex.getStatus(), ex.getTitle(), ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflict", "The operation conflicts with existing data");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", "The record was modified by another request, retry");
    }

    /** {@code @PreAuthorize} denials (AuthorizationDeniedException is a subclass). */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to perform this action");
    }

    /** {@code ?sort=<unknown property>} on a derived query or {@code findAll(Pageable)}. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleUnknownProperty(PropertyReferenceException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", INVALID_SORT_DETAIL);
    }

    /**
     * {@code ?sort=<unknown property>} on a {@code @Query}: Spring wraps Hibernate's path error. {@code @Query} paths are
     * validated at startup, so at runtime a path error comes from the client's sort; anything else stays a 500.
     */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ProblemDetail handleInvalidDataAccessApiUsage(InvalidDataAccessApiUsageException ex) {
        return isUnknownPath(ex)
                ? problem(HttpStatus.BAD_REQUEST, "Invalid request", INVALID_SORT_DETAIL)
                : handleUnexpected(ex);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "An unexpected error occurred");
    }

    /** {@code @Valid @RequestBody} and {@code @Valid @ModelAttribute} (multipart forms), including type mismatches. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        var errors = new LinkedHashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), message(e)));
        ex.getBindingResult().getGlobalErrors().forEach(e -> errors.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        return invalidData(errors);
    }

    /** Constraints on {@code @RequestParam} / {@code @PathVariable}, or mixed with {@code @Valid} arguments. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers,
                                                                            HttpStatusCode status, WebRequest request) {
        var errors = new LinkedHashMap<String, String>();
        ex.getParameterValidationResults().forEach(result -> {
            if (result instanceof ParameterErrors pe) {
                pe.getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), message(e)));
            } else {
                var name = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(e -> errors.putIfAbsent(name, e.getDefaultMessage()));
            }
        });
        return invalidData(errors);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpHeaders headers,
                                                                          HttpStatusCode status, WebRequest request) {
        return invalidData(Map.of("image", "must be at most 5 MB"));
    }

    /** Normalizes the titles of the framework errors (bad JSON, wrong param type, unknown route...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        var response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail pd && !INVALID_DATA.equals(pd.getTitle())) {
            pd.setTitle(switch (statusCode.value()) {
                case 400 -> "Invalid request";
                case 404 -> "Not found";
                default -> pd.getTitle();
            });
        }
        return response;
    }

    private ResponseEntity<Object> invalidData(Map<String, String> errors) {
        var pd = problem(HttpStatus.BAD_REQUEST, INVALID_DATA, INVALID_DATA_DETAIL);
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    /** Hibernate path error in the cause chain, or only in the message when the cause was not kept. */
    private static boolean isUnknownPath(Throwable ex) {
        for (var t = ex; t != null; t = t.getCause()) {
            if (t instanceof PathException || t instanceof PathElementException) return true;
        }
        var msg = ex.getMessage();
        return msg != null && (msg.contains(UnknownPathException.class.getName()) || msg.contains(PathElementException.class.getName()));
    }

    private static String message(FieldError error) {
        return error.isBindingFailure() ? INVALID_VALUE : error.getDefaultMessage();
    }

    private static ProblemDetail problem(HttpStatusCode status, String title, String detail) {
        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
