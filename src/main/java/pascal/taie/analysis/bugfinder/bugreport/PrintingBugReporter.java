//package pascal.taie.analysis.bugfinder.bugreport;
//
//import java.util.HashSet;
//
//public class PrintingBugReporter extends TextBugReporter{
//    private final HashSet<BugInstance> seenAlready = new HashSet<>();
//
//    // FIXME: may not assist multithreading
//    public void doReportBug(BugInstance bugInstance){
//        if(seenAlready.add(bugInstance)){
//            printBug(bugInstance);
//        }
//    }
//}
