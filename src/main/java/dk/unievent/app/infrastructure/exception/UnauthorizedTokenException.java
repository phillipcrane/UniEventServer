package dk.unievent.app.infrastructure.exception;

public class UnauthorizedTokenException extends RuntimeException {

    public UnauthorizedTokenException(String message) {
        super(message);
    }
}