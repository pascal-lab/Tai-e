package pascal.taie.interp;

public class InterpreterException extends UnsupportedOperationException {
    public InterpreterException(String info) {
        super(info);
    }

    public InterpreterException() {}

    public InterpreterException(Exception e) {
        super(e);
    }
}
