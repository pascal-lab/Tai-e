package pascal.taie.analysis.bugfinder.detector;

import pascal.taie.analysis.MethodAnalysis;
import pascal.taie.analysis.bugfinder.Severity;
import pascal.taie.analysis.bugfinder.BugInstance;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.util.collection.Sets;

import java.util.Set;

public class DroppedException extends MethodAnalysis {

    public static final String ID = "dropped-exception";

    public DroppedException(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Object analyze(IR ir) {
        Set<BugInstance> bugInstanceSet = Sets.newHybridSet();
        for (ExceptionEntry entry : ir.getExceptionEntries()) {
            String exceptionName = entry.catchType().getName();
            if (exceptionName.equals(ClassNames.CLONE_NOT_SUPPORTED_EXCEPTION)
                    || exceptionName.equals(ClassNames.INTERRUPTED_EXCEPTION)) {
                continue;
            }
            Stmt catchHandler = entry.handler();
            int nextStmt = catchHandler.getIndex() + 1;

            while(nextStmt < ir.getStmts().size() && ir.getStmt(nextStmt) instanceof Nop){
                nextStmt++;
            }

            if(nextStmt < ir.getStmts().size() &&
                (ir.getStmt(nextStmt) instanceof Goto || ir.getStmt(nextStmt) instanceof Return)){
                boolean exitInTryBlock = false;
                for(int i = entry.start().getIndex(); i <= entry.end().getIndex(); ++i){
                    if(ir.getStmt(i) instanceof Return){
                        exitInTryBlock = true;
                        break;
                    }
                }
                Severity severity = Severity.MINOR;
                if(exceptionName.equals(ClassNames.ERROR) || exceptionName.equals(ClassNames.EXCEPTION)
                    || exceptionName.equals(ClassNames.THROWABLE) || exceptionName.equals(ClassNames.RUNTIME_EXCEPTION)){
                    severity = Severity.CRITICAL;
                }
                BugInstance bugInstance = new BugInstance(exitInTryBlock ? "DE_MIGHT_DROP" : "DE_MIGHT_IGNORE", severity)
                        .setClassAndMethod(ir.getMethod())
                        .setSourceLine(catchHandler.getLineNumber());
                bugInstanceSet.add(bugInstance);
            }
        }

        return bugInstanceSet;
    }
}
