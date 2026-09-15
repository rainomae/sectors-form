package ee.sectorsform.shared;

/** Mapped to HTTP 409 by {@link GlobalExceptionHandler}. */
public abstract class ConflictException extends DomainException {

    protected ConflictException(String message) {
        super(message);
    }
}
