package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Var;

import java.util.ArrayList;
import java.util.List;

/**
 * Def-use information
 */
public class DUInfo {
    List<List<IBasicBlock>> defBlocks;

    public DUInfo(int varCount) {
        defBlocks = new ArrayList<>(varCount);
        for (int i = 0; i < varCount; i++) {
            defBlocks.add(new ArrayList<>());
        }
    }

    public void addDefBlock(Var v, IBasicBlock b) {
        int i = v.getIndex();
        assert i < defBlocks.size();
        defBlocks.get(i).add(b);
    }

    public List<IBasicBlock> getDefBlock(Var v) {
        return defBlocks.get(v.getIndex());
    }
}
