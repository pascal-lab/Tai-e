package pascal.taie.interp;

import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Maps;

import java.util.Map;

public class Frame {
    private int pc;

    private final Map<Var, JValue> regs;

    private JValue rets;

    public Frame(int pc, Map<Var, JValue> regs) {
        this.pc = pc;
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
