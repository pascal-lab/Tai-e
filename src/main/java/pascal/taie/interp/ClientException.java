package pascal.taie.interp;

public class ClientException extends RuntimeException {
    final Exception internal;

    public ClientException(Exception e) {
        this.internal = e;
    }

    @Override
    public String toString() {
        return "Client code throw uncaught exception: " + internal;
    }
}
