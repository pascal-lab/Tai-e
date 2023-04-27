package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.VarInsnNode;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.NullType;
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

    public static final String THIS = "this";

    public static final String NULL_LITERAL = "$null";

    private final JMethod method;

    private final @Nullable List<LocalVariableNode> localVariableTable;

    private final InsnList insnList;

    private int counter;

    private final Map<Triple<Integer, Integer, Integer>, Var> local2Var; // (slot, start(inclusive), end(exclusive)) -> Var

    private final Map<Triple<Integer, Integer, Integer>, Var> anonymousLocal2Var; // (slot, start(inclusive), end(exclusive)) -> Var

    private final int lastIndex;

    private final List<Var> params;

    private final Map<Var, Integer> paramsIndex;

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
        this.anonymousLocal2Var = Maps.newMap();
        this.params = new ArrayList<>();
        this.paramsIndex = Maps.newMap();
        this.vars = new ArrayList<>();
        this.retVars = new HashSet<>();

        // Test insnList.size to examine whether the method is not concrete.
        // Checking JMethod's modifiers may be a more elegant way.
        this.lastIndex = insnList.size() == 0 ? 0 : insnList.indexOf(insnList.getLast());

        // Bytecode also stores method parameters in local variable table,
        // and MethodNode.params seems abandoned by ASM.
        // So if there is no localVariableTable, generate param prefix names for params.
        if (!existsLocalVariableTable()) {
            int nowIdx = method.isStatic() ? 0 : 1;
            int n = nowIdx;
            for (int i = nowIdx; i < method.getParamCount() + nowIdx; ++i) {
                Var v = newParameter(i);
                this.params.add(v);
                local2Var.put(new Triple<>(n, 0, lastIndex + 1), v);
                this.paramsIndex.put(v, n);
                if (Utils.isTwoWord(method.getParamType(i - nowIdx))) {
                    n += 2;
                } else {
                    n += 1;
                }
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
     * @param insnNode asm insnNode that require the local variable
     * @return the corresponding TIR variable
     */
    public Var getLocal(int slot, AbstractInsnNode insnNode) {
        int asmIndex = insnList.indexOf(insnNode);
        Pair<Integer, Integer> query = new Pair<>(slot, asmIndex);

        var opt = local2Var.keySet().stream().filter(k -> match(query, k)).findAny();
        if (opt.isPresent()) {
            return local2Var.get(opt.get());
        }

        opt = anonymousLocal2Var.keySet().stream().filter(k -> match(query, k)).findAny();
        if (opt.isPresent()) {
            return anonymousLocal2Var.get(opt.get());
        }

        Var v = newVar(getLocalName(slot, getLocalName(slot, asmIndex)));
        // Note: if reach here, this variable must be a local variable

        // for generalization start could be 0, but in development stage we want to expose more case unexpected.
        int start = asmIndex;
        int end = lastIndex + 1;
        boolean found = false;
        if (existsLocalVariableTable()) {
            for (LocalVariableNode node : localVariableTable) {
                AbstractInsnNode startNode;
                if (node.start.getPrevious() == null) {
                    startNode = node.start; // index of start node == 0
                } else {
                    startNode = node.start.getPrevious();
                    assert startNode instanceof VarInsnNode : "Assume pred to be VarInsnNode" + startNode.getOpcode();
                    assert Opcodes.ISTORE <= startNode.getOpcode() && startNode.getOpcode() <= Opcodes.ASTORE : "Assume pred to be store";
                }
                int currStart = insnList.indexOf(startNode);
                int currEnd = insnList.indexOf(node.end);
                if (node.index == slot && currStart <= asmIndex && asmIndex < currEnd) {
                    start = currStart;
                    end = currEnd;
                    found = true;
                    break;
                }
            }
        }

        if (existsLocalVariableTable() && !found) {
            anonymousLocal2Var.put(new Triple<>(slot, start, end), v);
        } else {
            local2Var.put(new Triple<>(slot, start, end), v);
        }
        return v;
    }

    public Var splitLocal(Var old, int count) {
        Var v = newVar(old.getName() + "#" + count);
        return v;
    }

    private boolean match(Pair<Integer, Integer> query, Triple<Integer, Integer, Integer> var) {
        return query.first().equals(var.first())
                && var.second() <= query.second()
                && query.second() < var.third();
    }

    private boolean existsLocalVariableTable() {
        return localVariableTable != null && localVariableTable.size() != 0;
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
            nullLiteral.setType(NullType.NULL);
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

    public boolean isTempVar(Var v) {
        return v.getName().startsWith(TEMP_PREFIX);
    }

    public boolean isSpecialVar(Var v) {
        return v.getName().startsWith("*") || Objects.equals(v.getName(), NULL_LITERAL);
    }

    public boolean isLocal(Var v) { return ! isTempVar(v) && v != thisVar && ! isSpecialVar(v); }

    public List<Pair<Integer, Var>> getBlockVarWithIdx(BytecodeBlock block) {
        if (block.getFrameLocalType() == null) {
            return getParamsWithIdx();
        }
        List<Pair<Integer, Var>> res = new ArrayList<>();
        int start = insnList.indexOf(block.getFirstBytecode().get());
        int end = insnList.indexOf(block.getLastBytecode());

        local2Var.forEach((k, v) -> {
            if (start >= k.second() && end < k.third() &&
                    block.getFrameLocalType().containsKey(k.first()) &&
                    block.getFrameLocalType(k.first()) != Top.Top &&
                    v != thisVar) {
                res.add(new Pair<>(k.first(), v));
            }
        });
        return res;
    }

    public List<Pair<Integer, Var>> getParamsWithIdx() {
        return params.stream()
                .map(p -> new Pair<>(paramsIndex.get(p), p))
                .toList();
    }

    public List<Var> getBlockVar(BytecodeBlock block) {
        return getBlockVarWithIdx(block)
                .stream()
                .map(Pair::second)
                .toList();
    }

    public void replaceParam(Var oldVar, Var newVar) {
        int idx = 0;
        for (; idx < params.size(); ++idx) {
            if (params.get(idx) == oldVar) {
                break;
            }
        }
        paramsIndex.put(newVar, paramsIndex.get(oldVar));
        params.set(idx, newVar);
    }

    private LocalVariableNode searchLocal(int slot, int asmIndex) {
        for (LocalVariableNode node : localVariableTable) {
            AbstractInsnNode startNode;
            if (node.start.getPrevious() == null) {
                startNode = node.start; // index of start node == 0
            } else {
                startNode = node.start.getPrevious();
                assert startNode instanceof VarInsnNode : "Assume pred to be VarInsnNode";
                assert Opcodes.ISTORE <= startNode.getOpcode() && startNode.getOpcode() <= Opcodes.ASTORE : "Assume pred to be _STORE";
            }
            int start = insnList.indexOf(startNode);
            int end = insnList.indexOf(node.end);
            if (node.index == slot && start <= asmIndex && asmIndex < end) {
                return node;
            }
        }
        /* If not found, what is in the slot is an anonymous local variable generated for syntactic sugar by java compiler
           which does not appear in the LocalVariableTable even with compiling option -g.
           (Refer to var0 in private method p in CollectionTest.(java)|(class) for example.)
           So an unnamed local variable should be returned.
         */
        //throw new IllegalArgumentException();
        return null;
    }

    private @Nullable Type getLocalType(int i, int asmIndex) {
        if (localVariableTable == null) {
            return null;
        } else {
            LocalVariableNode n = searchLocal(i, asmIndex);
            if (n == null) {
                return null;
            }
            String sig = n.signature;
            return BuildContext.get().fromAsmType(sig);
        }
    }

    private @Nullable String getLocalName(int i, int asmIndex) {
        if (localVariableTable == null || localVariableTable.size() == 0) {
            return null;
        } else {
            LocalVariableNode n = searchLocal(i, asmIndex);
            if (n == null) {
                return null;
            }
            return n.name;
        }
    }

    private String getConstVarName(Literal literal) {
        return TEMP_PREFIX + "c" + counter;
    }

    // TODO: check when need to add v to vars
    private Var newParameter(int index) {
        return newVar(PARAMETER_PREFIX + index);
    }
    private Var newVar(String name) {
        Var v = new Var(method, name, null, counter++);
        vars.add(v);
        return v;
    }

    private Var newConstVar(String name, Literal literal) {
        Var v = new Var(method, name, null, counter++, literal);
        vars.add(v);
        return v;
    }
}
