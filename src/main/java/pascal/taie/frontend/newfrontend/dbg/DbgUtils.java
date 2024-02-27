package pascal.taie.frontend.newfrontend.dbg;

import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.frontend.newfrontend.AsmIRBuilder;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;

import java.util.List;
import java.util.Optional;

public class DbgUtils {
    public static List<Stmt> findDef(AsmIRBuilder builder, Var v) {
        return builder.getAllStmts().stream()
                .map(stmt -> filterDef(stmt, v))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private static Optional<Stmt> filterDef(Stmt stmt, Var v) {
        Optional<LValue> def = stmt.getDef();
        if (def.isPresent() && def.get().equals(v)) {
            return Optional.of(stmt);
        }
        return Optional.empty();
    }

    public static void dumpTIR(String className) {
        JClass klass = World.get().getClassHierarchy().getClass(className);
        if (klass == null) {
            System.out.println("Class not found: " + className);
            return;
        }
        IRDumper dumper = new IRDumper(AnalysisConfig.of(IRDumper.ID));
        dumper.analyze(klass);
    }
}
