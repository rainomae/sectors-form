package ee.sectorsform.shared;

/** Mapped to HTTP 404 by {@link GlobalExceptionHandler}. */
public abstract class NotFoundException extends DomainException {

    protected NotFoundException(String message) {
        super(message);
    }
}
