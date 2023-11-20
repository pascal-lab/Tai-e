package pascal.taie.frontend.newfrontend.report;

import pascal.taie.World;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.ArrayList;
import java.util.List;

public class TaieCastingReporter {
    public record TaieCastingInfo(JMethod method, Stmt stmt,
                                         Type leftType, Var var, Type rightType) {
    }

    static {
        World.registerResetCallback(() -> get().castingInfos.clear());
    }

    private static final TaieCastingReporter instance = new TaieCastingReporter();

    private final List<TaieCastingInfo> castingInfos = new ArrayList<>();

    public static TaieCastingReporter get() {
        return instance;
    }

    public void reportCasting(TaieCastingInfo info) {
        castingInfos.add(info);
    }

    public List<TaieCastingInfo> getCastingInfos() {
        return castingInfos;
    }
}

