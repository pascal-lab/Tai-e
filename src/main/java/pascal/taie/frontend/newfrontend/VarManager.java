package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Triple;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

class VarManager {

    public static final String PARAMETER_PREFIX = "@";
    public static final String LOCAL_PREFIX = "%";

    // TODO: use another method to avoid local var has same prefix
    public static final String TEMP_PREFIX = "$-";

    public static final String THIS = "this";

    public static final String NULL_LITERAL = "$null";

    private final JMethod method;

    private final @Nullable List<LocalVariableNode> localVariableTable;

    private final boolean existsLocalVariableTable;

    private final InsnList insnList;

    private int counter;

    private final Map<Integer, Var> local2Var; // slot -> Var

    private final Map<Triple<Integer, Integer, Integer>, LocalVariableNode> parsedLocalVarTable; // (slot, start(inclusive), end(exclusive)) -> Var

    private final List<Var> params;

    private final Map<Var, Integer> paramsIndex;

    private final List<Var> vars;

    private final Set<Var> retVars;

    private @Nullable Var thisVar;

    private @Nullable Var zeroLiteral;

    private @Nullable Var nullLiteral;

    private final Map<Literal, Var> blockConstCache;

    public VarManager(JMethod method,
                      @Nullable List<LocalVariableNode> localVariableTable,
                      InsnList insnList) {
        this.method = method;
        this.localVariableTable = localVariableTable;
        this.existsLocalVariableTable = localVariableTable != null && localVariableTable.size() != 0;
        this.insnList = insnList;
        this.local2Var = Maps.newMap();
        this.parsedLocalVarTable = existsLocalVariableTable ? Maps.newMap() : null;
        this.params = new ArrayList<>();
        this.paramsIndex = Maps.newMap();
        this.vars = new ArrayList<>();
        this.retVars = new HashSet<>();
        this.blockConstCache = Maps.newMap();

        if (existsLocalVariableTable) {
            processLocalVarTable();
        }

        int firstParamIndex = method.isStatic() ? 0 : 1;
        int slotOfCurrentParam = firstParamIndex;
        for (int NoOfParam = firstParamIndex; NoOfParam < method.getParamCount() + firstParamIndex; ++NoOfParam) {
            Var v = newParameter(NoOfParam);

            this.params.add(v);
            local2Var.put(slotOfCurrentParam, v);
            this.paramsIndex.put(v, slotOfCurrentParam);
            if (Utils.isTwoWord(method.getParamType(NoOfParam - firstParamIndex))) {
                slotOfCurrentParam += 2;
            } else {
                slotOfCurrentParam += 1;
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

    private void processLocalVarTable() {
        for (LocalVariableNode node : localVariableTable) {
            int start = insnList.indexOf(node.start);
            int end = insnList.indexOf(getNextTrueInsnNode(node.end));
            int slot = node.index;
            parsedLocalVarTable.put(new Triple<>(slot, start, end), node);
        }
    }

    public Optional<String> getName(int slot, AbstractInsnNode insnNode) {
        if (Utils.isVarStore(insnNode)) {
            /*
             * for VarStore, you have to use the next InsnNode (actual JVM Bytecode)
             * as the program point to query for the variable that being stored.
             * (See the definition of start_pc of local_variable_table entry)
             */
            insnNode = getNextTrueInsnNode(insnNode);
        }
        int asmIndex = insnList.indexOf(insnNode);
        return parsedLocalVarTable.keySet().stream()
                .filter(k -> match(new Pair<>(slot, asmIndex), k))
                .findAny()
                .map(k -> parsedLocalVarTable.get(k).name);
    }

    private static boolean match(Pair<Integer, Integer> query, Triple<Integer, Integer, Integer> var) {
        return query.first().equals(var.first())
                && var.second() <= query.second()
                && query.second() < var.third();
    }

    /**
     * @param insnNode the query node
     * @return the next true JVM Bytecode node if found, else the last insnNode of the insnList.
     */
    private static AbstractInsnNode getNextTrueInsnNode(AbstractInsnNode insnNode) {
        if (insnNode.getNext() == null) {
            return insnNode;
        }

        insnNode = insnNode.getNext();
        while ((insnNode instanceof LabelNode || insnNode instanceof FrameNode || insnNode instanceof LineNumberNode)
               && insnNode.getNext() != null) {
            insnNode = insnNode.getNext();
        }
        return insnNode;
    }

    public Var getTempVar() {
        return newVar(TEMP_PREFIX + "v" + counter);
    }

    public @Nullable Var getThisVar() {
        return thisVar;
    }

    public List<Var> getParams() {
        return params;
    }

    public List<Var> getParamThis() {
        List<Var> temp = new ArrayList<>();
        if (! method.isStatic()) {
            temp.add(thisVar);
        }
        temp.addAll(params);
        return temp;
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
        return local2Var.computeIfAbsent(slot, s -> newVar(LOCAL_PREFIX + s));
    }

    public Var splitLocal(Var old, int count, int slot, Stream<AbstractInsnNode> origins) {
        // TODO: use a global counter for each name
        if (! existsLocalVariableTable) {
            if (count == 1) {
                return old;
            } else {
                return newVar(getDefaultSplitName(old.getName(), count));
            }
        } else {
            Optional<String> name = origins
                    .map(orig -> getName(slot, orig))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
            String finalName = name.orElse(old.getName());

            if (count == 1) {
                old.setName(finalName);
                return old;
            } else {
                return newVar(getDefaultSplitName(finalName, count));
            }
        }
    }

    private static String getDefaultSplitName(String name, int count) {
        return name + "#" + count;
    }

    public boolean existsLocalVariableTable() {
        return existsLocalVariableTable;
    }

    public void addReturnVar(Var v) {
        this.retVars.add(v);
    }

    public Var getZeroLiteral() {
        if (zeroLiteral == null) {
            zeroLiteral = newVar("*intliteral0");
            zeroLiteral.setType(PrimitiveType.INT);
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

    public boolean peekConstVar(Literal literal) {
        return blockConstCache.containsKey(literal);
    }

    public Var getConstVar(Literal literal) {
        if (literal instanceof NullLiteral) {
            return getNullLiteral();
        } else {
            if (blockConstCache.containsKey(literal)) {
                return blockConstCache.get(literal);
            }
            return newConstVar(getConstVarName(literal), literal);
        }
    }

    public void clearConstCache() {
        blockConstCache.clear();
    }

    public boolean isTempVar(Var v) {
        return v.getName().startsWith(TEMP_PREFIX) && v != nullLiteral;
    }

    public boolean isSpecialVar(Var v) {
        return v.getName().startsWith("*") || Objects.equals(v.getName(), NULL_LITERAL);
    }

    public boolean isLocal(Var v) { return ! isTempVar(v) && ! isSpecialVar(v); }

    /**
     * @param block index of the AsmNode
     * @return live vars before the first AsmNode of the block.
     */
    public List<Pair<Integer, Var>> getDefsBeforeStartOfABlock(BytecodeBlock block) {
        List<Pair<Integer, Var>> res = new ArrayList<>();

        local2Var.forEach((k, v) -> {
            if (block.getFrameLocalType().containsKey(k) &&
                    block.getFrameLocalType(k) != Top.Top) {
                res.add(new Pair<>(k, v));
            }
        });

        {
            // TODO: refactor this check to a function call
            var l = res.stream().map(Pair::first).toList();
            assert l.size() == l.stream().distinct().toList().size();
        }

        return res;
    }

    public void replaceParam(Var oldVar, Var newVar) {
        if (oldVar == thisVar) {
            thisVar = newVar;
            return;
        }
        int idx = 0;
        assert !params.isEmpty();
        for (; idx < params.size(); ++idx) {
            if (params.get(idx) == oldVar) {
                break;
            }
        }
        assert idx < params.size();
        paramsIndex.put(newVar, paramsIndex.get(oldVar));
        params.set(idx, newVar);
    }

    private String getConstVarName(Literal literal) {
        return TEMP_PREFIX + "c" + counter;
    }

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
        blockConstCache.put(literal, v);
        return v;
    }

    int getSlot(Var var) {
        // TODO: there is a faster way to do this, may optimize
        return local2Var.entrySet()
                .stream()
                .filter((entry) -> entry.getValue() == var)
                .findFirst()
                .get()
                .getKey();
    }
}
