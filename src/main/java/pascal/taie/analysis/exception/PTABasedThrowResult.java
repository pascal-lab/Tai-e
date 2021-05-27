package pascal.taie.analysis.exception;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.exception.JMethodExceptionResult;
import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;
import java.util.Map;

import static pascal.taie.util.collection.MapUtils.newHybridMap;
import static pascal.taie.util.collection.SetUtils.newHybridSet;

public class PTABasedThrowResult {

    private final Map<JMethod, JMethodExceptionResult> csMethodResultMap;

    public PTABasedThrowResult(){
        csMethodResultMap=newHybridMap();
    }

    public void addJMethodAndExceptionResult(
            JMethod jMethod,
            JMethodExceptionResult jMethodExceptionResult){
        csMethodResultMap.put(jMethod,jMethodExceptionResult);
    }

    public JMethodExceptionResult getExceptionResult(JMethod jMethod){
        return csMethodResultMap.computeIfAbsent(
                jMethod,
                key->new JMethodExceptionResult(jMethod));
    }

    public Collection<Obj> mayThrowExplicitly(IR ir){
        JMethod jMethod=ir.getMethod();
        JMethodExceptionResult jMethodExceptionResult= csMethodResultMap.get(jMethod);
        if(jMethodExceptionResult==null){
            return newHybridSet();
        }
        else{
            return jMethodExceptionResult.getThrownExplicitExceptions();
        }
    }

    public Collection<Obj> mayThrowExplicitly(IR ir, Stmt stmt){
        JMethod jMethod=ir.getMethod();
        JMethodExceptionResult jMethodExceptionResult= csMethodResultMap.get(jMethod);
        if(jMethodExceptionResult==null){
            return newHybridSet();
        }
        else{
            return jMethodExceptionResult.mayThrow(stmt);
        }
    }
}
