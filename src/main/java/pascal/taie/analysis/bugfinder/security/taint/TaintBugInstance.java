package pascal.taie.analysis.bugfinder.security.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.bugfinder.BugInstance;
import pascal.taie.analysis.bugfinder.BugType;
import pascal.taie.analysis.bugfinder.Severity;
import pascal.taie.analysis.pta.plugin.taint.CallSourcePoint;
import pascal.taie.analysis.pta.plugin.taint.ParamSourcePoint;
import pascal.taie.analysis.pta.plugin.taint.SourcePoint;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.Comparator;
import java.util.Objects;

public class TaintBugInstance extends BugInstance {

    private final JClass sourceJClass;

    private final JMethod sourceJMethod;

    private int line = -1;

    private int index = -1;

    private static final Logger logger = LogManager.getLogger(TaintBugInstance.class);

    public TaintBugInstance(BugType type, Severity severity, JMethod jMethod, SourcePoint sourcePoint){
        super(type, severity, jMethod);
        this.sourceJClass = sourcePoint.getContainer().getDeclaringClass();

        if(sourcePoint instanceof ParamSourcePoint psp) {
            this.sourceJMethod = psp.getContainer();
            this.index = psp.index();
        }
        else if(sourcePoint instanceof CallSourcePoint csp){
            this.sourceJMethod = csp.sourceCall().getMethodRef().resolve();
            this.line = csp.sourceCall().getLineNumber();
        }
        else{
            throw new RuntimeException("TaintBugInstance does not contain this type of SourcePoint");
        }
    }

    public TaintBugInstance setSourceLine(int num) {
        super.setSourceLine(num);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaintBugInstance taintBugInstance)) {
            return false;
        }
        return super.equals(o)
                && Objects.equals(sourceJClass, taintBugInstance.sourceJClass)
                && Objects.equals(sourceJMethod, taintBugInstance.sourceJMethod)
                && line == taintBugInstance.line
                && index == taintBugInstance.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode() - 31, sourceJClass, sourceJMethod, line, index);
    }

    @Override
    public int compareTo(BugInstance o) {
        if(!(o instanceof TaintBugInstance tbi)){
            return super.compareTo(o);
        }
        if(sourceJClass.equals(tbi.sourceJClass)){
            int compareResult = 0;
            if(line >= 0) {
                compareResult = Integer.compare(line, tbi.line);
                return compareResult == 0 ? super.compareTo(o) : compareResult;
            }
            else if(index >= 0){
                compareResult = Integer.compare(index, tbi.index);
                return compareResult == 0 ? super.compareTo(o) : compareResult;
            }
        }
        return sourceJClass.toString().compareTo(tbi.sourceJClass.toString());
    }

    @Override
    public String toString() {
        String position = "null";
        String source = "null";
        if(line >= 0){
            position = "Line: " + String.valueOf(line);
            source = "Source: " + sourceJMethod;
        }
        else if(index >= 0){
            position = "Index: " + String.valueOf(index);
            source = "Container: " + sourceJMethod;
        }
        return String.format("{ SourceClass: %s, %s, %s\n%s }\n",
                sourceJClass, source, position, super.toString());
    }
}
