package pascal.taie.frontend.newfrontend;

/**
 * Represents the errors raised during constructing program information from bytecode in ASM form.
 */
class AsmFrontendException extends RuntimeException {

    AsmFrontendException(String msg) {
        super(msg);
    }

    AsmFrontendException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
