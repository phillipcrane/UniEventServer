package dk.unievent.app.infrastructure.exception;

public class OrganizerKeyExpiredException extends RuntimeException {
    public OrganizerKeyExpiredException() {
        super("Organizer key has expired");
    }
}
