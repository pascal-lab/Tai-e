package pascal.taie.interp;

import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Maps;

import java.util.Map;

public class Frame {
    private int pc;

    private int lastPc; // Only used for phi selection.

    private final Map<Var, JValue> regs;

    private JValue rets;

    public static final int METHOD_ENTRY = -1; // Should we define a uniform index for entry?

    public Frame(int pc, Map<Var, JValue> regs) {
        this.pc = pc;
        this.lastPc = METHOD_ENTRY;
        this.regs = regs;
    }

    public static Frame mkNewFrame() {
        return new Frame(0, Maps.newMap());
    }

    public static Frame mkNewFrame(Map<Var, JValue> args) {
        return new Frame(0, args);
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int getLastPc() {
        return lastPc;
    }

    public void setLastPc(int lastPc) {
        this.lastPc = lastPc;
    }

    public Map<Var, JValue> getRegs() {
        return regs;
    }

    public JValue getRets() {
        return rets;
    }

    public void setRets(JValue rets) {
        this.rets = rets;
    }

    public void markEnd() {
        this.pc = -1;
    }
}
