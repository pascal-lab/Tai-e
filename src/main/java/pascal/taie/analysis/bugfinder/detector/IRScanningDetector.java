//package pascal.taie.analysis.bugfinder.detector;
//
//import pascal.taie.analysis.IntraproceduralAnalysis;
//import pascal.taie.analysis.bugfinder.Priorities;
//import pascal.taie.analysis.bugfinder.bugreport.BugInstance;
//import pascal.taie.analysis.bugfinder.bugreport.BugReporter;
//import pascal.taie.analysis.bugfinder.bugreport.PrintingBugReporter;
//import pascal.taie.config.AnalysisConfig;
//import pascal.taie.ir.IR;
//import pascal.taie.ir.stmt.StmtVisitor;
//
//import java.util.HashSet;
//
//public abstract class IRScanningDetector implements StmtVisitor{
//
//     protected final IR ir;
//
//     private final HashSet<BugInstance> seenAlready = new HashSet<>();
//
//     protected IRScanningDetector(IR ir) {
//          this.ir = ir;
//     }
//
//     protected void reportBug(BugInstance bugInstance){
//          seenAlready.add(bugInstance);
//     }
//
//     public HashSet<BugInstance> getResult(){ return seenAlready; }
//}
