package pascal.taie.analysis.graph.cfg;

import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.MultiMap;

import java.util.List;
import java.util.Map;

public class ExtraEdgeAppender {
    public static void append(CFG<Stmt> cfg, MultiMap<Stmt, Stmt> mapping) {
        mapping.forEach((k, v) -> {
            ((StmtCFG)cfg).addEdge(new CFGEdge<>(CFGEdge.Kind.UNCAUGHT_EXCEPTION, k, v));
        });
    }
}
