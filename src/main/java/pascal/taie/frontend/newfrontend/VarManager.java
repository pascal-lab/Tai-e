package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import pascal.taie.ir.exp.IntLiteral;
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

    public static final String LOCAL_PREFIX = "%";

    // TODO: use another method to avoid local var has same prefix
    public static final String TEMP_PREFIX = "$-";

    public static final String THIS = "this";

    public static final String NULL_LITERAL = "$null";

    private final JMethod method;

    private final @Nullable List<LocalVariableNode> localVariableTable;

    final boolean existsLocalVariableTable;

    private final InsnList insnList;

    private int counter;

    private final Var[] local2Var; // slot -> Var

    private final Map<Triple<Integer, Integer, Integer>, LocalVariableNode> parsedLocalVarTable; // (slot, start(inclusive), end(exclusive)) -> Var

    private final List<Var> params;

    private final Map<Var, Integer> var2Local;

    private final List<Var> vars;

    private final Set<Var> retVars;

    private @Nullable Var thisVar;

    private @Nullable Var zeroLiteral;

    private @Nullable Var nullLiteral;

    private final Map<Literal, Var> blockConstCache;

    public VarManager(JMethod method,
                      @Nullable List<LocalVariableNode> localVariableTable,
                      InsnList insnList,
                      int maxLocal) {
        this.method = method;
        this.localVariableTable = localVariableTable;
        this.existsLocalVariableTable = localVariableTable != null && !localVariableTable.isEmpty();
        this.insnList = insnList;
        this.local2Var = new Var[maxLocal];
        this.parsedLocalVarTable = existsLocalVariableTable ? Maps.newMap() : null;
        this.params = new ArrayList<>();
        this.var2Local = Maps.newMap();
        this.vars = new ArrayList<>();
        this.retVars = new HashSet<>();
        this.blockConstCache = Maps.newMap();

        if (existsLocalVariableTable) {
            processLocalVarTable();
        }

        for (int i = 0; i < maxLocal; ++i) {
            makeLocal(i, getLocalName(i, method.isStatic()));
        }

        int firstParamIndex = method.isStatic() ? 0 : 1;
        int slotOfCurrentParam = firstParamIndex;
        for (int NoOfParam = firstParamIndex; NoOfParam < method.getParamCount() + firstParamIndex; ++NoOfParam) {
            Var v = getLocal(slotOfCurrentParam);
            if (existsLocalVariableTable) {
                // in our assumption, the parameters would occupy a certain slot during the whole method.
                int finalSlotOfCurrentParam = slotOfCurrentParam;
                parsedLocalVarTable
                        .keySet()
                        .stream()
                        .filter(t -> finalSlotOfCurrentParam == t.first())
                        .findAny()
                        .map(k -> parsedLocalVarTable.get(k).name)
                        .ifPresent(v::setName);
            }
            params.add(v);
            if (Utils.isTwoWord(method.getParamType(NoOfParam - firstParamIndex))) {
                slotOfCurrentParam += 2;
            } else {
                slotOfCurrentParam += 1;
            }
        }

        if (method.isStatic()) {
            thisVar = null;
        } else {
            thisVar = getLocal(0);
        }
    }

    private void processLocalVarTable() {
        assert localVariableTable != null;
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
     *
     * @param slot index of variable in bytecode
     * @return the corresponding TIR variable
     */
    public Var getLocal(int slot) {
        Var v = local2Var[slot];
        assert v != null;
        return v;
    }

    public Var[] getLocals() {
        return local2Var;
    }

    private void makeLocal(int slot, String name) {
        Var v1 = newVar(name);
        var2Local.put(v1, slot);
        local2Var[slot] = v1;
    }

    private String getLocalName(int slot, boolean isStatic) {
        if (slot == 0) {
            return isStatic ? LOCAL_PREFIX + slot : THIS;
        } else {
            return LOCAL_PREFIX + slot;
        }
    }

    public void fixName(Var var, String newName) {
        assert var.getName().startsWith(LOCAL_PREFIX);
        String sub = var.getName().substring(1);
        String[] counter = sub.split("#");
        var.setName(newName + (counter.length >= 2 ? "#" + counter[1] : ""));
    }

    public Var splitLocal(Var old, int count, int slot, Stream<AbstractInsnNode> origins) {
        // TODO: use a global counter for each name
        if (! existsLocalVariableTable) {
            if (count == 1) {
                return old;
            } else {
                return getSplitVar(getDefaultSplitName(old.getName(), count), slot);
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
                return getSplitVar(getDefaultSplitName(finalName, count), slot);
            }
        }
    }

    public Var splitLocal(int slot, int index) {
        Var old = local2Var[slot];
        if (index == 1) {
            return old;
        } else {
            return getSplitVar(getDefaultSplitName(old.getName(), index), slot);
        }
    }

    private Var getSplitVar(String name, int slot) {
        Var v = newVar(name);
        var2Local.put(v, slot);
        return v;
    }

    private static String getDefaultSplitName(String name, int count) {
        return name + "#" + count;
    }

    public void addReturnVar(Var v) {
        this.retVars.add(v);
    }

    public Var getZeroLiteral() {
        if (zeroLiteral == null) {
            zeroLiteral = newConstVar("*intliteral0", IntLiteral.get(0));
            zeroLiteral.setType(PrimitiveType.INT);
        }
        return zeroLiteral;
    }

    public Var getNullLiteral() {
        if (nullLiteral == null) {
            nullLiteral = newConstVar(NULL_LITERAL, NullLiteral.get());
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

    public boolean isNotSpecialVar(Var v) {
        return !v.getName().startsWith("*") && !Objects.equals(v.getName(), NULL_LITERAL);
    }

    /**
     * can only be used before splitting
     */
    public boolean isLocalFast(Var v) { return v.getIndex() < local2Var.length; }

    private static boolean verifyDefs(List<Pair<Integer, Var>> res) {
        var l = res.stream().map(Pair::first).toList();
        return l.size() == l.stream().distinct().toList().size();
    }

    public void replaceParam(Var oldVar, Var newVar) {
        if (oldVar == thisVar) {
            thisVar = newVar;
            return;
        }
        int idx = 0;
        for (; idx < params.size(); ++idx) {
            if (params.get(idx) == oldVar) {
                break;
            }
        }
        assert idx < params.size();
        var2Local.put(newVar, var2Local.get(oldVar));
        params.set(idx, newVar);
    }

    private String getConstVarName(Literal literal) {
        return TEMP_PREFIX + "c" + counter;
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
        Integer i = var2Local.get(var);
        return Objects.requireNonNullElse(i, -1);
    }

    int getSlotFast(Var var) {
        return var.getIndex();
    }
}
