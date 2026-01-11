package pascal.taie.frontend.java.ir.typing;

enum EdgeKind {
    // v1 <- v2
    VAR_VAR,

    // v1[i] <- v2
    VAR_ARRAY,

    // v2 <- v1[i]
    ARRAY_VAR

}
