package works.bosk.defang.api;

public class NotEntitledException extends RuntimeException {
    public NotEntitledException(String message) {
        super(message);
    }

    public NotEntitledException(String message, Throwable cause) {
        super(message, cause);
    }
}
