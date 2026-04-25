package dk.unievent.app.infrastructure.exception;

public class InvalidConfirmationTokenException extends RuntimeException {
    public InvalidConfirmationTokenException() {
        super("Invalid or expired confirmation token");
    }
}
