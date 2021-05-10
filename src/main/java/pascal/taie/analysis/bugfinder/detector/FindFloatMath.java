//package pascal.taie.analysis.bugfinder.detector;
//
//import pascal.taie.analysis.bugfinder.Priorities;
//import pascal.taie.analysis.bugfinder.bugreport.BugInstance;
//import pascal.taie.ir.IR;
//import pascal.taie.ir.exp.ArithmeticExp;
//import pascal.taie.ir.exp.BinaryExp;
//import pascal.taie.ir.stmt.Binary;
//import pascal.taie.language.type.PrimitiveType;
//
//public class FindFloatMath extends IRScanningDetector {
//
//    public FindFloatMath(IR ir){
//        super(ir);
//    }
//
//    @Override
//    public void visit(Binary stmt) {
//        super.visit(stmt);
//        BinaryExp exp = stmt.getRValue();
//        if(exp instanceof ArithmeticExp){
//            boolean hasFloat = exp.getValue1().getType().equals(PrimitiveType.FLOAT);
////                    || exp.getValue2().getType().equals(PrimitiveType.FLOAT);
////            boolean hasDouble = exp.getValue1().getType().equals(PrimitiveType.DOUBLE)
////                    || exp.getValue2().getType().equals(PrimitiveType.DOUBLE);
//            ArithmeticExp.Op op = ((ArithmeticExp) exp).getOperator();
//            String methodName = ir.getMethod().getName();
//
//            if(hasFloat && !methodName.contains("float") && !methodName.contains("Float")
//                    && !methodName.contains("FLOAT")){
//                assert exp.getValue2().getType().equals(PrimitiveType.FLOAT);
//
//                if(op.equals(ArithmeticExp.Op.ADD) || op.equals(ArithmeticExp.Op.SUB)){
//                    reportBug(new BugInstance("FL_MATH_USING_FLOAT_PRECISION", Priorities.LOW_PRIORITY)
//                            .addClassAndMethod(ir.getMethod())
//                            .addSourceLine(stmt.getLineNumber()));
//                }
//                else{
//                    assert op.equals(ArithmeticExp.Op.MUL) || op.equals(ArithmeticExp.Op.DIV)
//                            || op.equals(ArithmeticExp.Op.REM);
//                    reportBug(new BugInstance("FL_MATH_USING_FLOAT_PRECISION", Priorities.NORMAL_PRIORITY)
//                            .addClassAndMethod(ir.getMethod())
//                            .addSourceLine(stmt.getLineNumber()));
//                }
//            }
//        }
//    }
//
//}
