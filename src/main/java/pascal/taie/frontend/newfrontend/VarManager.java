package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.ParameterNode;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class VarManager {

    public static final String PARAMETER_PREFIX = "@";
    public static final String LOCAL_PREFIX = "%";
    public static final String TEMP_PREFIX = "$";

    public static final String STACK_PREFIX = "#";

    public static final String THIS = "this";

    public static final String NULL_LITERAL = "null";

    private final JMethod method;

    private final @Nullable List<LocalVariableNode> localVariableTable;

    private int counter;

    private int tempCounter;

    private final Map<Integer, Var> local2Var;

    private final List<Var> params;

    private final List<Var> vars;

    private final Set<Var> retVars;

    private final @Nullable Var thisVar;

    private @Nullable Var zeroLiteral;

    private @Nullable Var nullLiteral;

    public VarManager(JMethod method,
                      @Nullable List<ParameterNode> params,
                      @Nullable List<LocalVariableNode> localVariableTable) {
        this.method = method;
        this.localVariableTable = localVariableTable;
        this.local2Var = Maps.newMap();
        this.params = new ArrayList<>();
        this.vars = new ArrayList<>();
        this.retVars = new HashSet<>();

        int nowIdx = method.isStatic() ? 0 : 1;
        if (params == null) {
            for (int i = nowIdx; i < method.getParamCount() + nowIdx; ++i) {
                Var v = newParameter(i);
                this.params.add(v);
                local2Var.put(i, v);
            }
        } else {
            for (var i : params) {
                Var v = newVar(i.name);
                this.params.add(v);
                local2Var.put(nowIdx, v);
            }
        }

        if (method.isStatic()) {
            thisVar = null;
        } else {
            Var t = newVar(THIS);
            thisVar = t;
            local2Var.put(0, t);
        }
    }

    public Var getTempVar() {
        Var v = newVar(TEMP_PREFIX + tempCounter++);
        this.vars.add(v);
        return v;
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

    /**
     * Get the TIR var for a <code>this</code> variable, parameter or local variable
     * @param i index of variable in bytecode
     * @return the corresponding TIR variable
     */
    public Var getLocal(int i) {
        return local2Var.computeIfAbsent(i, t -> {
            Var v = newVar(getLocalName(i, getLocalName(i)));
            // Note: if reach here, this variable must be a local variable
            this.vars.add(v);
            return v;
        });
    }

    public String getLocalName(int i, @Nullable String name) {
        if (name == null) {
            return LOCAL_PREFIX + i;
        } else {
            return name;
        }
    }

    public void addReturnVar(Var v) {
        this.retVars.add(v);
    }

    public Var getZeroLiteral() {
        if (zeroLiteral == null) {
            zeroLiteral = newVar("*intliteral0");
        }
        return zeroLiteral;
    }

    public Var getNullLiteral() {
        if (nullLiteral == null) {
            nullLiteral = newVar(NULL_LITERAL);
        }
        return nullLiteral;
    }

    private LocalVariableNode searchLocal(int index) {
        for (LocalVariableNode node : localVariableTable) {
            if (node.index == index) {
                return node;
            }
        }
        throw new IllegalArgumentException();
    }

    private @Nullable Type getLocalType(int i) {
        if (localVariableTable == null) {
            return null;
        } else {
            String sig = searchLocal(i).signature;
            return BuildContext.get().fromAsmType(sig);
        }
    }

    private @Nullable String getLocalName(int i) {
        if (localVariableTable == null || localVariableTable.size() == 0) {
            return null;
        } else {
            return searchLocal(i).name;
        }
    }

    private Var newParameter(int index) {
        return newVar(PARAMETER_PREFIX + index);
    }
    private Var newVar(String name) {
        return new Var(method, name, null, counter++);
    }
}
