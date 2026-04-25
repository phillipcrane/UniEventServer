package dk.unievent.app.infrastructure.exception;

public class OrganizerKeyNotFoundException extends RuntimeException {
    public OrganizerKeyNotFoundException() {
        super("Organizer key not found");
    }
}
