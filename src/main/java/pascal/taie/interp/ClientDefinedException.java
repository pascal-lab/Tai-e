package pascal.taie.interp;

public class ClientDefinedException extends RuntimeException {
    JObject inner;

    ClientDefinedException(JObject inner) {
        this.inner = inner;
    }
}
