package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.ParameterNode;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Triple;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final InsnList insnList;

    private int counter;

    private final Map<Triple<Integer, Integer, Integer>, Var> local2Var; // (slot, [start, end)) -> Var

    private final List<Var> params;

    private final List<Var> vars;

    private final Set<Var> retVars;

    private final @Nullable Var thisVar;

    private @Nullable Var zeroLiteral;

    private @Nullable Var nullLiteral;

    public VarManager(JMethod method,
                      @Nullable List<ParameterNode> params,
                      @Nullable List<LocalVariableNode> localVariableTable,
                      InsnList insnList) {
        this.method = method;
        this.localVariableTable = localVariableTable;
        this.insnList = insnList;
        this.local2Var = Maps.newMap();
        this.params = new ArrayList<>();
        this.vars = new ArrayList<>();
        this.retVars = new HashSet<>();

        // Test insnList.size to examine whether the method is not concrete.
        // Checking JMethod's modifiers may be a more elegant way.
        int lastIndex = insnList.size() == 0 ? 0 : insnList.indexOf(insnList.getLast());

        int nowIdx = method.isStatic() ? 0 : 1;
        if (params == null) {
            for (int i = nowIdx; i < method.getParamCount() + nowIdx; ++i) {
                Var v = newParameter(i);
                this.params.add(v);
                local2Var.put(new Triple<>(i, 0, lastIndex + 1), v);
            }
        } else {
            for (var i : params) {
                Var v = newVar(i.name);
                this.params.add(v);
                local2Var.put(new Triple<>(nowIdx, 0, lastIndex + 1), v);
                nowIdx++;
            }
        }

        if (method.isStatic()) {
            thisVar = null;
        } else {
            Var t = newVar(THIS);
            thisVar = t;
            local2Var.put(new Triple<>(0, 0, lastIndex + 1), t);
        }
    }

    public Var getTempVar() {
        Var v = newVar(TEMP_PREFIX + "v" + counter);
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
     * @param slot index of variable in bytecode
     * @return the corresponding TIR variable
     */
    public Var getLocal(int slot, AbstractInsnNode insnNode) {
        int asmIndex = insnList.indexOf(insnNode);
        Pair<Integer, Integer> query = new Pair<>(slot, asmIndex);
        var opt = local2Var.keySet().stream().filter(k -> match(query, k)).findAny();
        if (opt.isPresent()) {
            return local2Var.get(opt.get());
        } else {
            Var v = newVar(getLocalName(slot, getLocalName(slot, asmIndex)));
            // Note: if reach here, this variable must be a local variable
            this.vars.add(v);

            int lastIndex = insnList.indexOf(insnList.getLast());
            int start = 0;
            int end = lastIndex + 1;
            if (localVariableTable != null) {
                boolean found = false;
                for (LocalVariableNode node : localVariableTable) {
                    start = insnList.indexOf(node.start);
                    end = insnList.indexOf(node.end);
                    if (node.index == slot && start <= asmIndex && asmIndex < end) {
                        found = true;
                        break;
                    }
                }
                assert found;
            }

            local2Var.put(new Triple<>(slot, start, end), v);
            return v;
        }
    }

    private boolean match(Pair<Integer, Integer> query, Triple<Integer, Integer, Integer> var) {
        int start = var.second();
        int end = var.third();
        return Objects.equals(query.first(), var.first())
                && start <= query.second()
                && query.second() < end;
    }

    private String getLocalName(int i, @Nullable String name) {
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

    public Var getConstVar(Literal literal) {
        if (literal instanceof NullLiteral) {
            return getNullLiteral();
        } else {
            return newConstVar(getConstVarName(literal), literal);
        }
    }

    private LocalVariableNode searchLocal(int slot, int asmIndex) {
        for (LocalVariableNode node : localVariableTable) {
            int start = insnList.indexOf(node.start);
            int end = insnList.indexOf(node.end);
            if (node.index == slot && start <= asmIndex && asmIndex < end) {
                return node;
            }
        }
        throw new IllegalArgumentException();
    }

    private @Nullable Type getLocalType(int i, int asmIndex) {
        if (localVariableTable == null) {
            return null;
        } else {
            String sig = searchLocal(i, asmIndex).signature;
            return BuildContext.get().fromAsmType(sig);
        }
    }

    private @Nullable String getLocalName(int i, int asmIndex) {
        if (localVariableTable == null || localVariableTable.size() == 0) {
            return null;
        } else {
            return searchLocal(i, asmIndex).name;
        }
    }

    private String getConstVarName(Literal literal) {
        return TEMP_PREFIX + "c" + counter;
    }

    private Var newParameter(int index) {
        return newVar(PARAMETER_PREFIX + index);
    }
    private Var newVar(String name) {
        return new Var(method, name, null, counter++);
    }

    private Var newConstVar(String name, Literal literal) {
        return new Var(method, name, null, counter++, literal);
    }
}
