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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class VarManager implements IVarManager {

    public static final String LOCAL_PREFIX = "%";

    // TODO: use another method to avoid local var has same prefix
    public static final String TEMP_PREFIX = "$-";

    public static final String THIS = "this";

    public static final String NULL_LITERAL = "$null";

    private final JMethod method;

    private final @Nullable List<LocalVariableNode> localVariableTable;

    public final boolean existsLocalVariableTable;

    private final InsnList insnList;

    private int counter;

    private final int INT_CACHE_LOW = -128;

    private final int INT_CACHE_HIGH = 127;

    private final Var[] intConstVarCache;

    private Var[] local2Var; // slot -> Var

    // parsedLocalVarTable :: slot -> (start(inclusive), end(exclusive)) -> VarNode
    private final Map<Pair<Integer, Integer>, LocalVariableNode>[] parsedLocalVarTable;

    private final List<Var> params;

    private final Map<Var, Integer> var2Local;

    private final List<Var> vars;

    private final Set<Var> retVars;

    private @Nullable Var thisVar;

    private @Nullable Var zeroLiteral;

    private @Nullable Var nullLiteral;

    public VarManager(JMethod method,
                      @Nullable List<LocalVariableNode> localVariableTable,
                      InsnList insnList,
                      int maxLocal) {
        this.method = method;
        this.localVariableTable = localVariableTable;
        this.existsLocalVariableTable = localVariableTable != null && !localVariableTable.isEmpty();
        this.insnList = insnList;
        this.intConstVarCache = new Var[-INT_CACHE_LOW + 1 + INT_CACHE_HIGH];
        this.local2Var = new Var[maxLocal];
        this.parsedLocalVarTable = existsLocalVariableTable ? new Map[maxLocal] : null;
        this.params = new ArrayList<>();
        this.var2Local = Maps.newMap();
        this.vars = new ArrayList<>(maxLocal * 6);
        this.retVars = new HashSet<>();

        if (existsLocalVariableTable) {
            processLocalVarTable();
        }

        for (int i = 0; i < maxLocal; ++i) {
            makeLocal(i, getLocalName(i, method.isStatic()));
        }

        int firstParamIndex = method.isStatic() ? 0 : 1;
        int slotOfCurrentParam = firstParamIndex;
        for (int NoOfParam = firstParamIndex; NoOfParam < method.getParamCount() + firstParamIndex; ++NoOfParam) {
            assert slotOfCurrentParam < maxLocal;
            Var v = getLocal(slotOfCurrentParam);
            if (existsLocalVariableTable) {
                // in our assumption, the parameters would occupy a certain slot during the whole method.
                Map<Pair<Integer, Integer>, LocalVariableNode> localVarTableForSlot =
                        parsedLocalVarTable[slotOfCurrentParam];
                if (localVarTableForSlot != null) {
                    localVarTableForSlot
                            .keySet()
                            .stream()
                            .findAny()
                            .map(k -> localVarTableForSlot.get(k).name)
                            .ifPresent(v::setName);
                }
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
            if (parsedLocalVarTable[slot] == null) {
                parsedLocalVarTable[slot] = Maps.newMap();
            }
            parsedLocalVarTable[slot].put(new Pair<>(start, end), node);
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
        Map<Pair<Integer, Integer>, LocalVariableNode> localVarTableForSlot =
                parsedLocalVarTable[slot];
        if (localVarTableForSlot == null) {
            return Optional.empty();
        }
        return localVarTableForSlot.keySet().stream()
                .filter(k -> k.first() <= asmIndex && asmIndex < k.second())
                .findAny()
                .map(k -> localVarTableForSlot.get(k).name);
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

    @Override
    public Var splitVar(Var var, int index) {
        if (isTempVar(var)) {
            if (index == 1) {
                return var;
            } else {
                return newVar(getDefaultSplitName(var.getName(), index));
            }
        }
        return splitLocal(var, index, var2Local.get(var), Stream.of());
    }

    public @Nullable Var getThisVar() {
        return thisVar;
    }

    /**
     * @return parameters except `this`.
     */
    @Override
    public List<Var> getParams() {
        return params;
    }

    public List<Var> getParamThis() {
        List<Var> temp;
        if (thisVar != null) {
            temp = new ArrayList<>(params.size() + 1);
            temp.add(thisVar);
            temp.addAll(params);
        } else {
            temp = new ArrayList<>(params);
        }
        return temp;
    }

    @Override
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

    public int getSlot(Var local) {
        Integer i = var2Local.get(local);
        assert i != null;
        return i;
    }

    public Var[] getLocals() {
        return local2Var;
    }

    @Override
    public Var[] getNonSSAVar() {
        return vars.toArray(Var[]::new);
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
//        assert var.getName().startsWith(LOCAL_PREFIX);
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
                Var splitLocal = getSplitVar(getDefaultSplitName(finalName, count), slot);
                var2Local.put(splitLocal, slot);
                return splitLocal;
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

    public void enlargeLocal(int newMaxLocal, int[] originMapping, int[] paramIndexes) {
        assert newMaxLocal == originMapping.length;
        assert paramIndexes.length <= local2Var.length;
        Var[] newLocal2Var = new Var[newMaxLocal];
        System.arraycopy(local2Var, 0, newLocal2Var, 0, local2Var.length);
        int counter = 0;
        for (int i = local2Var.length; i < originMapping.length; ++i) {
            newLocal2Var[i] = newVar(
                    getDefaultSplitName(newLocal2Var[originMapping[i]].getName(), counter++));
            var2Local.put(newLocal2Var[i], originMapping[i]);
        }

        for (int i = 0; i < paramIndexes.length; ++i) {
            int paramIndex = paramIndexes[i];
            if (paramIndex == -1) {
                continue;
            }
            Var v = newLocal2Var[paramIndex];
            Var vOld = local2Var[i];
            if (v != vOld) {
                replaceParam(vOld, v);
            }
        }

        local2Var = newLocal2Var;
    }

    public void aliasLocal(Var var, int slot) {
        assert !var2Local.containsKey(var);
        var2Local.put(var, slot);
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
        // should we include $null here?
        return literal instanceof IntLiteral intLiteral
                && INT_CACHE_LOW <= intLiteral.getValue()
                && intLiteral.getValue() <= INT_CACHE_HIGH;
    }

    public Var getConstVar(Literal literal) {
        if (literal instanceof NullLiteral) {
            return getNullLiteral();
        } else if (peekConstVar(literal)) {
            IntLiteral intLiteral = (IntLiteral) literal;
            int value = intLiteral.getValue();
            int index = value - INT_CACHE_LOW;
            if (intConstVarCache[index] == null) {
                String name = TEMP_PREFIX + "c" + "i" + value;
                Var v = new Var(method, name, PrimitiveType.INT, counter++, IntLiteral.get(value));
                intConstVarCache[index] = v;
                vars.add(intConstVarCache[index]);
            }
            return intConstVarCache[index];
        } else {
            return newConstVar(getConstVarName(literal), literal);
        }
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

    public boolean isLocal(Var v) {
        return var2Local.containsKey(v);
    }

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
        return v;
    }

    int[] getSlotTable() {
        int[] res = new int[vars.size()];
        Arrays.fill(res, -1);
        var2Local.forEach((k, v) -> {
            res[k.getIndex()] = v;
        });
        return res;
    }

    static int getSlotFast(Var var) {
        return var.getIndex();
    }

    @Override
    public void removeAndReindexVars(Predicate<Var> p) {
        vars.removeIf(p);
        for (int i = 0; i < vars.size(); i++) {
            vars.get(i).setIndex(i);
        }
    }
}
