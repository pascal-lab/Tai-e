package pascal.taie.analysis.bugfinder.bugreport;

import pascal.taie.analysis.bugfinder.Priorities;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

//TODO: refactor it with more precise context information.
public class BugInstance {

    private final String type;

    private Priorities priority;
    // use JMethod to provide more precise context information temporarily
    private JClass jClass;
    private JMethod jMethod;
    private JField jField;
    private int sourceLineNumber;
//    private final ArrayList<BugAnnotation> annotationList;

    public BugInstance(@Nonnull String type, Priorities priority){
        this.type = type.intern();
        this.priority = priority;
//        annotationList = new ArrayList<>(4);
        sourceLineNumber = -1;
        jClass = null;
        jField = null;
        jMethod = null;
    }

    public Priorities getPriority() { return priority;}

    public String getType() { return type;}

    private String getString(Object o){
        if(o == null){
            return "";
        }
        return o.toString();
    }

    /**
     * For now, get the location of this BugInstance.
     */
    public String getDescription() {
        // TODO: add more detailed description on it.
        return getString(jClass) + " " + getString(jMethod) + " " + getString(jField);
    }

    public String getDescriptionWithType(){
        return getType() + " in " + getDescription();
    }

    public BugInstance addClassAndMethod(JMethod method){
        addMethod(method);
        addClass(method.getDeclaringClass());
        return this;
    }

//    public BugInstance addClass(String className){
//        return this;
//    }
//
    public BugInstance addClass(JClass clazz){
        jClass = clazz;
        return this;
    }

    public BugInstance addMethod(JMethod method){
        jMethod = method;
        return this;
    }

    public BugInstance addField(JField field){
        jField = field;
        return this;
    }

    public BugInstance addSourceLine(int lineNumber){
        sourceLineNumber = lineNumber;
        return this;
    }

    public int getPrimarySourceLineNumber(){
        return sourceLineNumber;
    }
//    public BugInstance add(@Nonnull BugAnnotation bugAnnotation){
//        requireNonNull(bugAnnotation, "missing bug annotation");
//
//        return this;
//    }
}
