package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.JumpStmt;
import pascal.taie.ir.stmt.LookupSwitch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.TableSwitch;
import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StmtManager {
    List<Stmt> constRegion;

    List<Stmt> normalRegion;

    List<Stmt> stmts;

    int constRegionSize;

    final Map<String, Stmt> blockMap;

    final List<String> assocList;

    final Map<JumpStmt, String> gotoMap;

    final Map<SwitchStmt, List<String>> switchMap;

    final Map<SwitchStmt, String> switchDefaultMap;

    public StmtManager() {
        constRegion = new LinkedList<>();
        normalRegion = new LinkedList<>();
        blockMap = Maps.newMap();
        assocList = new LinkedList<>();
        gotoMap = Maps.newMap();
        switchDefaultMap = Maps.newMap();
        switchMap = Maps.newMap();
    }

    public void addConst(Stmt stmt) {
        stmt.setIndex(constRegion.size());
        constRegion.add(stmt);
    }


    public int getTop() {
        return normalRegion.size();
    }

    public void assocLabel(String label) {
        assocList.add(label);
    }

    public void addStmt(int lineNo, Stmt stmt) {
        stmt.setLineNumber(lineNo);
        stmt.setIndex(getTop());
        if (assocList.size() != 0) {
            for (var i : assocList) {
                blockMap.put(i, stmt);
            }
        }
        normalRegion.add(stmt);
        assocList.clear();
    }

    void addPatchList(JumpStmt stmt, String label) {
        gotoMap.put(stmt, label);
    }

    public void addGoto(int lineno, String label) {
        var go = new Goto();
        var top = normalRegion.size();
        addStmt(lineno, go);
        addPatchList(go, label);
    }


    public void addIf(int lineno, ConditionExp exp, String label) {
        var go = new If(exp);
        var top = normalRegion.size();
        addStmt(lineno, go);
        addPatchList(go, label);
    }

    public void addTableSwitch(int lineno, Var v, int lowIdx, int highIdx, List<String> label, String defaultLabel) {
        SwitchStmt stmt = new TableSwitch(v, lowIdx, highIdx);
        addStmt(lineno, stmt);
        switchMap.put(stmt, label);
        switchDefaultMap.put(stmt, defaultLabel);
    }

    public void addLookupSwitch(int lineno, Var v, List<Integer> values, List<String> label, String defaultLabel) {
        var top = normalRegion.size();
        SwitchStmt stmt = new LookupSwitch(v, values);
        addStmt(lineno, stmt);
        switchMap.put(stmt, label);
        switchDefaultMap.put(stmt, defaultLabel);
    }

    /**
     * Use union-find set to resolve all goto statements
     */

    public void resolveGoto() {
        gotoMap.forEach((i, l) -> {
            if (i instanceof Goto g) {
                g.setTarget(blockMap.get(l));
            } else if (i instanceof If _if)  {
                _if.setTarget(blockMap.get(l));
            }
        });

        switchMap.forEach((i, l) -> i.setTargets(l.stream().map(blockMap::get).toList()));

        switchDefaultMap.forEach((i, l) -> i.setDefaultTarget(blockMap.get(l)));
    }

    public void resolveAll() {
        resolveGoto();
        int constSize = constRegion.size();
        constRegionSize = constSize;
        int normalSize = normalRegion.size();
        int allSize = constSize + normalSize;
        stmts = new ArrayList<>(allSize);
        stmts.addAll(constRegion);
        int i = 0;
        for (Stmt nowStmt : normalRegion) {
            nowStmt.setIndex(i + constSize);
            stmts.add(nowStmt);
            i++;
        }
        constRegion.clear();
        normalRegion.clear();
    }

    public Stmt getStmt(int i) {
        return stmts.get(i + constRegionSize);
    }

    public List<Stmt> getStmts() {
        return stmts;
    }
}
