package pascal.taie.analysis.pta.plugin;

import pascal.taie.analysis.pta.core.solver.PointerAnalysis;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

public class InvokedynamicPlugin implements Plugin{

    private PointerAnalysis pta;

    // Lambdas can be processed in LambdasPlugin
    private final boolean processLambdas = false;

    private Var indyResult;

    @Override
    public void initialize() {

    }

    @Override
    public void handleNewMethod(JMethod method) {
        System.out.println("hello handleNewMethod");
        extractInvokeDynamics(method.getIR()).forEach(invoke -> {
            indyResult = invoke.getResult();
            InvokeDynamic indy = (InvokeDynamic) invoke.getInvokeExp();
            String bsmName = indy.getBootstrapMethodRef().getName();
            if (processLambdas
                    || ( !processLambdas && !"metafactory".equals(bsmName) && !"altMetafactory".equals(bsmName))){

            }
            JClass indyLookupClass = indy.getCallSite().getMethod().getDeclaringClass();
//            indyLookupClass.getDeclaredMethod();
//            indyLookupClass.getDeclaredMethod();

        });
    }

    private static Stream<Invoke> extractInvokeDynamics(IR ir) {
        return ir.getStmts()
                .stream()
                .filter(s -> s instanceof Invoke)
                .map(s -> (Invoke) s)
                .filter(s -> s.getInvokeExp() instanceof InvokeDynamic);
    }

}
