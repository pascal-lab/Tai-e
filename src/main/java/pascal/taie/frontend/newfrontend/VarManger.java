package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.ParameterNode;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

class VarManger {

    public static final String LOCAL_PREFIX = "%";
    public static final String TEMP_PREFIX = "$";

    public static final String STACK_PREFIX = "#";

    private JMethod method;

    private int counter;

    private int tempCounter;

    private Map<Integer, Var> local2Var;

    private List<Var> params;

    private List<Var> vars;

    private Set<Var> retVars;

    private @Nullable Var thisVar;

    public VarManger(JMethod method, List<ParameterNode> params) {
        this.method = method;
        this.local2Var = Maps.newMap();

        if (method.isStatic()) {
            thisVar = null;
        }
    }

    public Var getTempVar() {
        return newVar(TEMP_PREFIX + tempCounter++, null);
    }

    public @Nullable Var getThisVar() {
        return thisVar;
    }

    public List<Var> getParams() {
        return params;
    }

    public List<Var> getVars() {
        return vars;
    }

    public Set<Var> getRetVars() {
        return retVars;
    }

    public Var getLocal(int i, @Nullable String name, @Nullable Type type) {
        return local2Var.computeIfAbsent(i, t -> newVar(getLocalName(i, name), type));
    }

    public String getLocalName(int i, @Nullable String name) {
        if (name == null) {
            return LOCAL_PREFIX + i;
        } else {
            return name;
        }
    }

    private Var newVar(String name, @Nullable Type type) {
        return new Var(method, name, type, counter++);
    }
}
