package sa.pta.statement;

public interface Statement {

    enum Kind {
        ALLOCATION,
        ASSIGN,
        INSTANCE_LOAD,
        INSTANCE_STORE,
        CALL,
    }

    Kind getKind();
}
