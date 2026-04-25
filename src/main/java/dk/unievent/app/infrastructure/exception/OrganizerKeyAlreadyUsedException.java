package dk.unievent.app.infrastructure.exception;

public class OrganizerKeyAlreadyUsedException extends RuntimeException {
    public OrganizerKeyAlreadyUsedException() {
        super("Organizer key has already been used");
    }
}
