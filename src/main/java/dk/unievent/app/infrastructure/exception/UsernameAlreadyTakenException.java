package dk.unievent.app.infrastructure.exception;

public class UsernameAlreadyTakenException extends RuntimeException {
    public UsernameAlreadyTakenException() {
        super("Username is already taken");
    }
}
