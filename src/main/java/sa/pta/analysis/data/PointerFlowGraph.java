package sa.pta.analysis.data;

public interface PointerFlowGraph {

    class Edge {

        enum Kind {
            LOCAL_ASSIGN,
            INTERPROCEDRUAL_ASSIGN,
            INSTANCE_FIELD_LOAD,
            INSTANCE_FIELD_STORE,
            STATIC_FIELD_LOAD,
            STATIC_FIELD_STORE,
            // array, reflection, ...
        }

        private Pointer from;

        private Pointer to;
    }
}
