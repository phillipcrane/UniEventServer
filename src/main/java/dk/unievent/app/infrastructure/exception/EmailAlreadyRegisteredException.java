package dk.unievent.app.infrastructure.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("Email is already registered");
    }
}
