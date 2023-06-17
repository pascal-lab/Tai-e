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
import pascal.taie.util.collection.Quadruple;
import pascal.taie.util.collection.Triple;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

class VarManager {

    public static final String PARAMETER_PREFIX = "@";
    public static final String LOCAL_PREFIX = "%";

    // TODO： use another method to avoid local var has same prefix
    public static final String TEMP_PREFIX = "$-";

    public static final String THIS = "this";

    public static final String NULL_LITERAL = "$null";

    private final JMethod method;

    private final @Nullable List<LocalVariableNode> localVariableTable;

    private final boolean existsLocalVariableTable;

    private final InsnList insnList;

    private int counter;

    private final Map<Triple<Integer, Integer, Integer>, Var> local2Var; // (slot, start(inclusive), end(exclusive)) -> Var

    private final Map<Quadruple<Integer, String, String, String>, Var> nameAndType2Var; // (varName, desc, signature) -> Var

    private final Map<Triple<Integer, Integer, Integer>, Var> anonymousLocal2Var; // (slot, start(inclusive), end(exclusive)) -> Var

    private final int lastIndex;

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
        this.existsLocalVariableTable = existsLocalVariableTable();
        this.insnList = insnList;
        this.local2Var = Maps.newMap();
        this.nameAndType2Var = existsLocalVariableTable ? Maps.newMap() : null;
        this.anonymousLocal2Var = existsLocalVariableTable ? Maps.newMap() : null;
        this.params = new ArrayList<>();
        this.paramsIndex = Maps.newMap();
        this.vars = new ArrayList<>();
        this.retVars = new HashSet<>();
        this.blockConstCache = Maps.newMap();

        if (existsLocalVariableTable) {
            processLocalVarTable();
        }

        // Test insnList.size to examine whether the method is not concrete.
        // Checking JMethod's modifiers may be a more elegant way.
        this.lastIndex = insnList.size() == 0 ? 0 : insnList.indexOf(insnList.getLast());
        int offset = 0;
        if (!(insnList.getLast() instanceof LabelNode)) {
            offset = 1;
        }

        // Bytecode also stores method parameters in local variable table,
        // and MethodNode.params seems abandoned by ASM.
        // So if there is no localVariableTable, generate param prefix names for params.
        // And we should get the parameters in advance to register a position in params for them
        // in case there does not exist reference to a parameter in the code.
        int firstParamIndex = method.isStatic() ? 0 : 1;
        int slotOfCurrentParam = firstParamIndex;
        for (int NoOfParam = firstParamIndex; NoOfParam < method.getParamCount() + firstParamIndex; ++NoOfParam) {
            // TODO: May be buggy when using 0 in the query for that it might return null I do
            // not sure that the LabelNode that denotes the start or the end for a VarTableNode
            // is precisely the First and the Last of the insnList.
            Pair<Integer, Integer> query = new Pair<>(slotOfCurrentParam, 0);
            var opt = local2Var.keySet().stream().filter(k -> match(query, k)).findAny();
            Var v = opt.isPresent() ?
                    local2Var.get(opt.get())
                    : newParameter(NoOfParam);

            this.params.add(v);
            local2Var.put(new Triple<>(slotOfCurrentParam, 0, lastIndex + offset), v);
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
            if (existsLocalVariableTable) {
                thisVar = nameAndType2Var
                        .values()
                        .stream()
                        .filter(v -> v.getName().equals("this"))
                        .findAny()
                        .get();
            } else {
                Var t = newVar(THIS);
                thisVar = t;
                local2Var.put(new Triple<>(0, 0, lastIndex + offset), t);
            }
        }
    }

    private void processLocalVarTable() {
        for (LocalVariableNode node : localVariableTable) {
            int start = insnList.indexOf(node.start);
            int end = insnList.indexOf(getNextTrueInsnNode(node.end));
            int slot = node.index;
            String varName = node.name;
            String descriptor = node.desc;
            String signature = node.signature;
            var t = new Quadruple<>(slot, varName, descriptor, signature);
            Var v = nameAndType2Var.computeIfAbsent(t, k -> newVar(varName)); // for `this`, varName.equals("this")
            local2Var.put(new Triple<>(slot, start, end), v);
        }
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
        Var v = newVar(TEMP_PREFIX + "v" + counter);
        return v;
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
        if (Utils.isVarStore(insnNode)) {
            /*
             * for VarStore, you have to use the next InsnNode (actual JVM Bytecode)
             * as the program point to query for the variable that being stored.
             * (See the definition of start_pc of local_variable_table entry)
             */
            insnNode = getNextTrueInsnNode(insnNode);
        }
        int asmIndex = insnList.indexOf(insnNode);
        return existsLocalVariableTable ?
                getLocalWithLocalVarTable(slot, asmIndex) : getLocalWithoutLocalVarTable(slot, asmIndex);
    }

    private Var getLocalWithLocalVarTable(int slot, int asmIndex) {
        Pair<Integer, Integer> query = new Pair<>(slot, asmIndex);

        var opt = local2Var.keySet().stream().filter(k -> match(query, k)).findAny();
        if (opt.isPresent()) {
            return local2Var.get(opt.get());
        }

        /* If not found, it means that the variable in the slot is an anonymous local variable
           generated for syntactic sugar by java compiler, which does not appear in the
           LocalVariableTable even with compiling option -g.
           (Refer to var0 in private method p in CollectionTest.(java)|(class) for example.)
           So an unnamed local variable should be returned.
         */

        // Note: if reach here, this variable must be a local variable // ???
        opt = anonymousLocal2Var.keySet().stream().filter(k -> match(query, k)).findAny();
        if (opt.isPresent()) {
            return anonymousLocal2Var.get(opt.get());
        }

        int start = 0;
        int end = lastIndex + 1;
        Var v = newVar(getLocalName(slot, null));
        anonymousLocal2Var.put(new Triple<>(slot, start, end), v);
        return v;
    }

    private Var getLocalWithoutLocalVarTable(int slot, int asmIndex) {
        Pair<Integer, Integer> query = new Pair<>(slot, asmIndex);

        var opt = local2Var.keySet().stream().filter(k -> match(query, k)).findAny();
        if (opt.isPresent()) {
            return local2Var.get(opt.get());
        }

        // Note: if reach here, this variable must be a local variable

        // TODO: for generalization the initial start could be 0,
        // but in development stage we want to expose more case unexpected.
        int start = 0;
        int end = lastIndex + 1;
        Var v = newVar(getLocalName(slot, null));
        local2Var.put(new Triple<>(slot, start, end), v);
        return v;
    }

    public Var splitLocal(Var old, int count) {
        Var v = newVar(old.getName() + "#" + count);
        return v;
    }

    private static boolean match(Pair<Integer, Integer> query, Triple<Integer, Integer, Integer> var) {
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

    public List<Pair<Integer, Var>> getBlockVarWithIdx(BytecodeBlock block) {
        if (block.getFrameLocalType() == null) {
            return getParamsWithIdx();
        }
        List<Pair<Integer, Var>> res = new ArrayList<>();
        int start = insnList.indexOf(block.getFirstBytecode().get());
        int end = insnList.indexOf(block.getLastBytecode());

        BiConsumer<Triple<Integer, Integer, Integer>, Var> c = (k, v) -> {
            if (/*start >= k.second() && end < k.third() && */
                    block.getFrameLocalType().containsKey(k.first()) &&
                    block.getFrameLocalType(k.first()) != Top.Top &&
                    v != thisVar) {
                res.add(new Pair<>(k.first(), v));
            }
        };

        local2Var.forEach(c);
        if (existsLocalVariableTable) {
            anonymousLocal2Var.forEach(c);
        }
        return res;
    }

    /**
     * @param block index of the AsmNode
     * @return live vars before the first AsmNode of the block.
     */
    public List<Pair<Integer, Var>> getDefsBeforeStartOfABlock(BytecodeBlock block) {
        List<Pair<Integer, Var>> res = new ArrayList<>();

        int index = insnList.indexOf(block.getFirstBytecode().get());

        /*
         * Though some entries of attribute information may be associated with the same program point (i.e. the real bytecode),
         * we still assume that they have some sequential order, and the order is implemented by the
         * order of ASM AbstractNodes in InsnList plus the postponement of end of localVarEntryEnd.
         * Here (a controlFlow meeting point), we need to take 3 kinds of information (i.e. mapFrame,
         * localVarEntryEnd and the program point) into consideration.
         * We view them as such order:
         * [the meeting point] -> [mapFrame] -> [localVarEntryEnd] -> [program point]
         * (The assumption comes from a corner case met in jre17:<com.sun.media.sound.DirectAudioDevice$DirectDL: int write(byte[],int,int)>)
         */
        local2Var.forEach((k, v) -> {
            if (k.second() <= index && index <= k.third() &&
                    block.getFrameLocalType().containsKey(k.first()) &&
                    block.getFrameLocalType(k.first()) != Top.Top) {
                res.add(new Pair<>(k.first(), v));
            }
        });
        if (existsLocalVariableTable) {
            anonymousLocal2Var.forEach((k, v) -> {
                if (/*k.second() <= index && index < k.third() && // which is always true */
                        res.stream().noneMatch(p -> p.first().equals(k.first())) &&
                        block.getFrameLocalType().containsKey(k.first()) &&
                        block.getFrameLocalType(k.first()) != Top.Top) {
                    res.add(new Pair<>(k.first(), v));
                }
            });
        }

        {
            var l = res.stream().map(Pair::first).toList();
            assert l.size() == l.stream().distinct().toList().size();
        }

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
        blockConstCache.put(literal, v);
        return v;
    }
}
