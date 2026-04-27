package com.tictactoe.session.exception;

import com.tictactoe.session.dto.ApiErrorResponse;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(SessionNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(SessionNotFoundException exception, ServerWebExchange exchange) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage(), exchange);
	}

	@ExceptionHandler(SessionConflictException.class)
	ResponseEntity<ApiErrorResponse> handleConflict(SessionConflictException exception, ServerWebExchange exchange) {
		return error(HttpStatus.CONFLICT, exception.getMessage(), exchange);
	}

	@ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
	ResponseEntity<ApiErrorResponse> handleConcurrentUpdate(ServerWebExchange exchange) {
		return error(HttpStatus.CONFLICT, "Session was updated concurrently; retry the request", exchange);
	}

	@ExceptionHandler(EngineCommunicationException.class)
	ResponseEntity<ApiErrorResponse> handleEngineCommunication(EngineCommunicationException exception,
															ServerWebExchange exchange) {
		log.error("Engine communication failed while handling {} {}",
				exchange.getRequest().getMethod(),
				exchange.getRequest().getPath().value(),
				exception);
		return error(HttpStatus.BAD_GATEWAY, exception.getMessage(), exchange);
	}

	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception, ServerWebExchange exchange) {
		HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
		String message = switch (status) {
			case METHOD_NOT_ALLOWED -> "Method is not supported for this endpoint";
			case NOT_FOUND -> "No endpoint found for %s %s".formatted(
					exchange.getRequest().getMethod(),
					exchange.getRequest().getPath().value()
			);
			default -> exception.getReason();
		};
		return error(status, message, exchange);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, ServerWebExchange exchange) {
		log.error("Unexpected error while handling {} {}",
				exchange.getRequest().getMethod(),
				exchange.getRequest().getPath().value(),
				exception);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", exchange);
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, ServerWebExchange exchange) {
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(
						Instant.now(),
						status.value(),
						status.getReasonPhrase(),
						message,
						exchange.getRequest().getPath().value()
				));
	}
}
