package ee.sectorsform.shared;

/** Mapped to HTTP 403 by {@link GlobalExceptionHandler}. */
public abstract class ForbiddenException extends DomainException {

    protected ForbiddenException(String message) {
        super(message);
    }
}
