package ee.sectorsform.shared;

/** Mapped to HTTP 400 by {@link GlobalExceptionHandler}. */
public abstract class BadRequestException extends DomainException {

    protected BadRequestException(String message) {
        super(message);
    }
}
