package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.stmt.Stmt;

import java.util.List;

public interface IBasicBlock {

    int getIndex();

    List<Stmt> getStmts();

    void setStmt(Stmt stmt, int pos);

    /**
     * Insert statements at the beginning of the block
     * @param stmts the statements to be inserted
     */
    void insertStmts(List<Stmt> stmts);

}
