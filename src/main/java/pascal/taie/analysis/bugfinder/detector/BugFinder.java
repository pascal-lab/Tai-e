//package pascal.taie.analysis.bugfinder.detector;
//
//import pascal.taie.analysis.IntraproceduralAnalysis;
//import pascal.taie.analysis.bugfinder.Priorities;
//import pascal.taie.config.AnalysisConfig;
//import pascal.taie.ir.IR;
//
//import java.lang.reflect.Constructor;
//
//public abstract class BugFinder extends IntraproceduralAnalysis {
//
//    private Constructor detectorConstructor;
//
//    protected BugFinder(AnalysisConfig config) throws ClassNotFoundException{
//        super(config);
//        Class detectorClass = Class.forName(config.getId());
//        try{
//            this.detectorConstructor = detectorClass.getConstructor(IR.class);
//        }
//        catch (NoSuchMethodException e){
//            // should never happen
//            e.printStackTrace();
//        }
//    }
//
//    public Object analyze(IR ir) {
//        IRScanningDetector detector;
//        try{
//            detector = (IRScanningDetector) detectorConstructor.newInstance(ir);
//        }
//        catch (Exception e){
//            // should never happen
//            e.printStackTrace();
//            return null;
//        }
//        ir.getStmts().forEach(s -> s.accept(detector));
//        return detector.getResult();
//    }
//}
