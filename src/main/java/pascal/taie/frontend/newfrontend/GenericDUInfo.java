package pascal.taie.frontend.newfrontend;

import java.util.List;

/**
 * Def-use information
 * DON FORGET PARAMS (a special kind of def)
 */
public interface GenericDUInfo<Block extends IBasicBlock> {

    enum OccurType {
        USE,
        DEF,
        PARAM
    }

    interface DUVisitor {
        void visit(int index, OccurType type, int v);
    }

    List<Block> getDefBlock(int v);

    int getMaxDuIndex();

    void visit(Block block, DUVisitor visitor);

    Block getBlock(int index);

    int getParamSize();
}
