package sa.callgraph;

public interface Edge<CallSite, Method> {

    enum Kind {
        VIRTUAL,
        SPECIAL,
        STATIC,
        OTHER,
    }

    Kind getKind();

    CallSite getCaller();

    Method getCallee();
}
