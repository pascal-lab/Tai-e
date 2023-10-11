package pascal.taie.frontend.newfrontend;

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

