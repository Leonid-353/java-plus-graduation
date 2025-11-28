package ru.practicum.service.exception;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(final NotFoundException e) {
        return new ErrorResponse("NOT_FOUND", "The required object was not found.", e.getMessage());
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(RuntimeException e) {
        return new ErrorResponse("CONFLICT", "Integrity constraint has been violated.", e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(ValidationException e) {
        return new ErrorResponse("BAD_REQUEST", "Incorrectly made request.", e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleForbidden(ForbiddenException e) {
        return new ErrorResponse("FORBIDDEN", "For the requested operation the conditions are not met.",
                e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().stream() //извлекаем список всех ошибок
                .map(error -> {
                    String field = ((FieldError) error).getField(); //получаем имя поля, которое не прошло валидацию
                    String message = error.getDefaultMessage(); //получаем сообщение об ошибке
                    Object rejectedValue = ((FieldError) error).getRejectedValue(); //получаем значение не прошедшее валидацию
                    return "Field: " + field + ". Error: " + message + ". Value: " + rejectedValue; //формируем строку с подробным описанием ошибки
                })
                .findFirst()//из всего потока берем первое (если оно есть)
                .orElse("Validation failed without specific error message."); //если по какой-то причине нет подробностей возвращаем дефолтное сообщение

        return new ErrorResponse("BAD_REQUEST", "Incorrectly made request.", errorMessage); //возвращаем объект с информацией об ошибке
    }

}
