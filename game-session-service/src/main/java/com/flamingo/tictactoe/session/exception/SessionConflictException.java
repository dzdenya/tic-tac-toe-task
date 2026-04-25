package com.flamingo.tictactoe.session.exception;

public class SessionConflictException extends RuntimeException {

	public SessionConflictException(String message) {
		super(message);
	}
}
