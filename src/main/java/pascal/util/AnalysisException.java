package pascal.util;

public class AnalysisException extends RuntimeException {

    /**
     * Constructs a new exception.
     */
    public AnalysisException() {
    }

    /**
     * Constructs a new exception.
     */
    public AnalysisException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new exception.
     */
    public AnalysisException(Throwable t) {
        super(t);
    }

    /**
     * Constructs a new exception.
     */
    public AnalysisException(String msg, Throwable t) {
        super(msg, t);
    }
}
