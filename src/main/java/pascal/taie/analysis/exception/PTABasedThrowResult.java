package pascal.taie.analysis.exception;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.exception.MethodExceptionResult;
import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;
import java.util.Map;

import static pascal.taie.util.collection.MapUtils.newHybridMap;
import static pascal.taie.util.collection.SetUtils.newHybridSet;

public class PTABasedThrowResult {

    private final Map<JMethod, MethodExceptionResult> csMethodResultMap;

    public PTABasedThrowResult(){
        csMethodResultMap=newHybridMap();
    }

    public void addJMethodAndExceptionResult(
            JMethod jMethod,
            MethodExceptionResult methodExceptionResult){
        csMethodResultMap.put(jMethod, methodExceptionResult);
    }

    public MethodExceptionResult getExceptionResult(JMethod jMethod){
        return csMethodResultMap.computeIfAbsent(
                jMethod,
                key->new MethodExceptionResult(jMethod));
    }

    public Collection<Obj> mayThrowExplicitly(IR ir){
        JMethod jMethod=ir.getMethod();
        MethodExceptionResult methodExceptionResult = csMethodResultMap.get(jMethod);
        if(methodExceptionResult ==null){
            return newHybridSet();
        }
        else{
            return methodExceptionResult.getThrownExplicitExceptions();
        }
    }

    public Collection<Obj> mayThrowExplicitly(IR ir, Stmt stmt){
        JMethod jMethod=ir.getMethod();
        MethodExceptionResult methodExceptionResult = csMethodResultMap.get(jMethod);
        if(methodExceptionResult ==null){
            return newHybridSet();
        }
        else{
            return methodExceptionResult.mayThrow(stmt);
        }
    }
}
