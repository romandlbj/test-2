package com.yiling.stocktake.controller;

import com.yiling.stocktake.model.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.NoSuchElementException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError badRequest(IllegalArgumentException ex) {
        return new ApiError("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError notFound(NoSuchElementException ex) {
        return new ApiError("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<ApiError> responseStatus(ResponseStatusException ex) {
        String code = ex.getStatus() == HttpStatus.UNAUTHORIZED ? "UNAUTHORIZED" : ex.getStatus() == HttpStatus.FORBIDDEN ? "FORBIDDEN" : "REQUEST_REJECTED";
        return org.springframework.http.ResponseEntity.status(ex.getStatus()).body(new ApiError(code, ex.getReason()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError malformedRequest(HttpMessageNotReadableException ex) {
        return new ApiError("BAD_REQUEST", "请求参数格式不正确");
    }

    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidParameter(Exception ex) {
        return new ApiError("BAD_REQUEST", "请求参数格式不正确，请检查日期和分页参数");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError internalError(Exception ex) {
        return new ApiError("INTERNAL_ERROR", "服务暂时不可用，请稍后重试");
    }
}
