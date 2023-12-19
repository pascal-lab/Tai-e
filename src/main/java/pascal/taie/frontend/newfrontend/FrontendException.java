package pascal.taie.frontend.newfrontend;

/**
 * Represents the errors raised during constructing program information using new frontend.
 */
public class FrontendException extends RuntimeException {

    public FrontendException(String msg) {
        super(msg);
    }

    FrontendException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
