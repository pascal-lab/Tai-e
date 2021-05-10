//package pascal.taie.analysis.bugfinder.bugreport;
//
//import pascal.taie.analysis.bugfinder.Priorities;
//
//import javax.annotation.Nonnull;
//import java.io.OutputStreamWriter;
//import java.io.PrintWriter;
//import java.nio.charset.StandardCharsets;
//
//public abstract class TextBugReporter implements BugReporter{
//
//    private int priorityThreshold;
//
//    protected PrintWriter outputStream = new PrintWriter(
//            new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true
//    );
//
//    public TextBugReporter(){
//        this.priorityThreshold = Priorities.LOW_PRIORITY;
//    }
//
//    public TextBugReporter(int priorityThreshold){
//        this.priorityThreshold = priorityThreshold;
//    }
//
//    public void setPriorityThreshold(int priorityThreshold) { this.priorityThreshold = priorityThreshold; }
//
//    public void setOutputStream(PrintWriter printWriter) { this.outputStream = printWriter; }
//
//    protected void printBug(BugInstance bugInstance){
//        // TODO: finish priorities
//        switch (bugInstance.getPriority()){
//            case Priorities.LOW_PRIORITY:
//                outputStream.print("L :");
//                break;
//            case Priorities.NORMAL_PRIORITY:
//                outputStream.print("M ");
//                break;
//            case Priorities.HIGH_PRIORITY:
//                outputStream.print("H ");
//                break;
//            default:
//                assert false;
//        }
//        outputStream.print(bugInstance.getDescriptionWithType() + "line number: "
//                + bugInstance.getPrimarySourceLineNumber());
//    }
//    @Override
//    public void reportBug(@Nonnull BugInstance bugInstance){
//        if(bugInstance.getPriority() <= priorityThreshold){
//            doReportBug(bugInstance);
//        }
//    }
//
//    protected abstract void doReportBug(BugInstance bugInstance);
//}
