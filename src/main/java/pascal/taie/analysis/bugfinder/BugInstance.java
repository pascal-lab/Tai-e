package pascal.taie.analysis.bugfinder;

import pascal.taie.ir.IR;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;

import javax.annotation.Nonnull;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

//TODO: refactor it with more precise context information.
public class BugInstance {

    private final String type;

    private Severity severity;

    private JClass jClass;

    private JMethod jMethod;


    private int sourceLineStart = -1, sourceLineEnd = -2;
//    private final ArrayList<BugAnnotation> annotationList;

    public BugInstance(@Nonnull String type, Severity severity) {
        this.type = type.intern();
        this.severity = severity;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getType() {
        return type;
    }

    private String getString(Object o) {
        return o == null ? "empty" :o.toString();
    }

    @Override
    public String toString() {
        String sourcelineRange = "empty";
        if(sourceLineStart >= 0){
            sourcelineRange = sourceLineStart == sourceLineEnd ? String.valueOf(sourceLineStart) :
                sourceLineStart + "---" + sourceLineEnd;
        }
        return String.format("Class: %s, Method: %s, LineNumber: %s. \nbug type: %s, severity: %s",
            getString(jClass), getString(jMethod), sourcelineRange, type, severity
            );
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof BugInstance bugInstance)) return false;

        return type.equals(bugInstance.type) && jClass == bugInstance.jClass
            && jMethod == bugInstance.jMethod && sourceLineStart == bugInstance.sourceLineStart
            && sourceLineEnd == bugInstance.sourceLineEnd;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(jClass);
        result = 31 * result + Objects.hashCode(jMethod);
        result = 31 * result + sourceLineStart;
        result = 31 * result + sourceLineEnd;
        return result;
    }

    public static BugInstance newBugInstance(String type, Severity severity, JMethod method, int lineNum){
        return new BugInstance(type, severity).setClassAndMethod(method).setSourceLine(lineNum);
    }

    public BugInstance setClassAndMethod(JMethod method) {
        setMethod(method);
        setClass(method.getDeclaringClass());
        return this;
    }

    public BugInstance setClass(JClass clazz) {
        jClass = clazz;
        return this;
    }

    public BugInstance setMethod(JMethod method) {
        jMethod = method;
        return this;
    }

    public BugInstance setSourceLine(int num) {
        sourceLineStart = sourceLineEnd = num;
        return this;
    }

    public BugInstance setSourceLine(int start, int end) {
        sourceLineStart = start;
        sourceLineEnd = end;
        return this;
    }

    public int getSourceLineStart() {
        return sourceLineStart;
    }

    public int getSourceLineEnd() {
        return sourceLineEnd;
    }

}
