/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.frontend.java.ir;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.ir.exp.VarMutator;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Manages the {@link Var}s during IR construction.
 * <p>
 * It handles the mapping between new slots and vars, resolving var names, the constant caches,
 * and tracking var attributes like SSA status.
 */
public class VarManager {

    // =================================================================================
    // Constants
    // =================================================================================

    private static final String LOCAL_PREFIX = "$";

    private static final String TEMP_PREFIX = "%";

    private static final String THIS = "this";

    private static final String NULL_LITERAL = "$null";

    // =================================================================================
    // Context
    // =================================================================================

    /**
     * The shared context holding all resources and state for the IR building process.
     */
    private final IRBuilderContext context;

    // =================================================================================
    // Caches
    // =================================================================================

    /**
     * Cache range for small integer constants.
     */
    private final int INT_CACHE_LOW = -8;

    private final int INT_CACHE_HIGH = 7;

    private final int UNDEFINED_LINE_NUMBER = -1;

    private final int CONFLICT_LINE_NUMBER = -2;

    /**
     * Cache for small integer constant variables.
     */
    private final Var[] intConstVarCache;

    private final int[] intConstVarLineNumbers;

    /**
     * Cache for the null literal variable.
     */
    private @Nullable Var nullLiteral;

    // =================================================================================
    // Slot, Name, SSA Management (the attributes of vars)
    // =================================================================================

    /**
     * Map from new slot index to the IR Var.
     * New slots are constructed during SSA transformation when splitting original slots.
     * Only params and non-SSA form vars (need redef, so need slot) will use this mapping.
     */
    private Var[] slot2Var;

    /**
     * Set of Vars that are used and are corresponding to slots (the used Var set for {@link #slot2Var}).
     */
    private Set<Var> usedSlotVars;

    /**
     * slot2Local[slot].get(range) = local variable node in bytecode.
     * Maps from slot and range to the concrete local variable.
     * Used for variable name resolution.
     */
    private final Map<Pair<Integer, Integer>, LocalVariableNode>[] slot2Locals;

    /**
     * Tracks usage count of variable names to resolve conflicts (e.g., "name#1").
     * Used for variable name resolution.
     */
    private final Map<String, Integer> name2Count;

    /**
     * Tracks whether a Var is in ssa form.
     */
    private final Map<Var, Boolean> varIsSSA;

    // =================================================================================
    // Var Storage
    // =================================================================================

    /**
     * Counter for generating unique var IDs.
     */
    private int counter;

    /**
     * List of all variables created for this method.
     */
    private List<Var> allVars;

    private final List<Var> params;

    private final Set<Var> retVars;

    private @Nullable Var thisVar;

    // =================================================================================
    // Initialization
    // =================================================================================

    VarManager(IRBuilderContext context) {
        this.context = context;
        this.intConstVarCache = new Var[-INT_CACHE_LOW + 1 + INT_CACHE_HIGH];
        this.intConstVarLineNumbers = new int[-INT_CACHE_LOW + 1 + INT_CACHE_HIGH];
        Arrays.fill(intConstVarLineNumbers, UNDEFINED_LINE_NUMBER);
        this.counter = 0;
        this.allVars = new ArrayList<>(context.source.maxLocals * 6);
        this.params = new ArrayList<>();
        this.retVars = Sets.newSet();
        this.varIsSSA = Maps.newMap();
        this.name2Count = Maps.newMap();
        // compute slot2Locals mapping for variable name resolution
        this.slot2Locals = computeSlot2Locals();
        // initialize vars for all slots
        buildSlotVars(context);
        // initialize params and this var
        getParamsFromSlotVars(context);
    }

    // =================================================================================
    // Var Management
    // =================================================================================

    public Var getTempVar() {
        Var v = newVar(TEMP_PREFIX + "v" + counter);
        setSSA(v);
        return v;
    }

    public @Nullable Var getThisVar() {
        return thisVar;
    }

    /**
     * @return parameters except `this`.
     */
    public List<Var> getParams() {
        return params;
    }

    public List<Var> getAllVars() {
        return allVars;
    }

    public Set<Var> getRetVars() {
        return retVars;
    }

    void addReturnVar(Var v) {
        this.retVars.add(v);
    }

    /**
     * Get the integer constant var cache along with their line numbers.
     */
    Pair<Var, Optional<Integer>>[] getIntConstVarCache() {
        Pair<Var, Optional<Integer>>[] intConstWithLineNumber = new Pair[this.intConstVarCache.length];
        for (int i = 0; i < this.intConstVarCache.length; i++) {
            Var v = this.intConstVarCache[i];
            int lineNumber = this.intConstVarLineNumbers[i];
            Optional<Integer> lineNumberOpt = lineNumber == UNDEFINED_LINE_NUMBER || lineNumber == CONFLICT_LINE_NUMBER
                    ? Optional.empty()
                    : Optional.of(lineNumber);
            intConstWithLineNumber[i] = new Pair<>(v, lineNumberOpt);
        }
        return intConstWithLineNumber;
    }

    Var getNullLiteral() {
        if (nullLiteral == null) {
            nullLiteral = newConstVar(NULL_LITERAL, NullLiteral.get());
        }
        return nullLiteral;
    }

    /**
     * Creates a new variable for a non-cached constant literal.
     */
    Var getConstVar(Literal literal) {
        assert !(literal instanceof NullLiteral);
        assert !(isCachedInt(literal));
        return newConstVar(TEMP_PREFIX + "c" + counter, literal);
    }

    boolean isCachedInt(Literal literal) {
        return literal instanceof IntLiteral intLiteral
                && INT_CACHE_LOW <= intLiteral.getValue()
                && intLiteral.getValue() <= INT_CACHE_HIGH;
    }

    Var getCachedInt(Literal literal) {
        assert isCachedInt(literal);
        IntLiteral intLiteral = (IntLiteral) literal;
        int index = intLiteral.getValue() - INT_CACHE_LOW;
        if (intConstVarCache[index] == null) {
            String name = getCachedIntName(intLiteral);
            intConstVarCache[index] = newConstVar(name, intLiteral);
        }
        if (intConstVarLineNumbers[index] == UNDEFINED_LINE_NUMBER) {
            intConstVarLineNumbers[index] = context.stmtManager.getLineNumber();
        } else if (intConstVarLineNumbers[index] != context.stmtManager.getLineNumber()) {
            // cache var defined more than once, so no line number
            intConstVarLineNumbers[index] = CONFLICT_LINE_NUMBER;
        }
        return intConstVarCache[index];
    }

    String getCachedIntName(Literal literal) {
        assert isCachedInt(literal);
        IntLiteral intLiteral = (IntLiteral) literal;
        int value = intLiteral.getValue();
        return TEMP_PREFIX + "c" + "i" + value;
    }

    private Var newVar(String name) {
        name = nameWithSuffix(name);
        Var v = new Var(context.method, name, null, -1);
        counter++;
        allVars.add(v);
        return v;
    }

    private Var newConstVar(String name, Literal literal) {
        Var v = new Var(context.method, name, literal.getType(), -1, literal);
        counter++;
        allVars.add(v);
        setNonSSA(v);
        return v;
    }

    // =================================================================================
    // Slot Management
    // =================================================================================

    /**
     * Get the TIR var for the given slot.
     */
    Var getVar(int slot) {
        Var v = slot2Var[slot];
        usedSlotVars.add(v);
        assert v != null;
        return v;
    }

    boolean isSlotVar(Var v) {
        return usedSlotVars.contains(v);
    }

    void removedUnusedSlotVars() {
        List<Var> newAllVars = new ArrayList<>();
        Set<Var> unusedSlotVars = Sets.newSet();
        for (Var v : slot2Var) {
            if (!usedSlotVars.contains(v)) {
                unusedSlotVars.add(v);
            }
        }
        for (Var v : allVars) {
            if (!unusedSlotVars.contains(v)) {
                VarMutator.setIndex(v, newAllVars.size());
                newAllVars.add(v);
            }
        }
        allVars = newAllVars;
    }

    private void buildSlotVars(IRBuilderContext context) {
        this.slot2Var = new Var[context.source.maxLocals];
        this.usedSlotVars = Sets.newSet();
        for (int slot = 0; slot < context.source.maxLocals; ++slot) {
            Var v = newVar(LOCAL_PREFIX + slot);
            slot2Var[slot] = v;
            setNonSSA(v);
        }
    }

    private void getParamsFromSlotVars(IRBuilderContext context) {
        int firstParamIndex = context.method.isStatic() ? 0 : 1;
        int slotOfCurrentParam = firstParamIndex;
        for (int param = firstParamIndex; param < context.method.getParamCount() + firstParamIndex; ++param) {
            assert slotOfCurrentParam < context.source.maxLocals;
            Var v = getVar(slotOfCurrentParam);
            if (existLocalVariables()) {
                // in our assumption, the parameters would occupy a certain slot during the whole method.
                Map<Pair<Integer, Integer>, LocalVariableNode> localVariables =
                        slot2Locals[slotOfCurrentParam];
                if (localVariables != null) {
                    localVariables
                            .keySet()
                            .stream()
                            .findAny()
                            .map(k -> localVariables.get(k).name)
                            .ifPresent((name) -> VarMutator.setName(v, nameWithSuffix(name)));
                }
            }
            params.add(v);
            if (FrontendTypeSystem.isTwoWord(context.method.getParamType(param - firstParamIndex))) {
                slotOfCurrentParam += 2;
            } else {
                slotOfCurrentParam += 1;
            }
        }

        if (context.method.isStatic()) {
            thisVar = null;
        } else {
            thisVar = getVar(0);
            VarMutator.setName(thisVar, THIS);
        }
    }

    /**
     * Create new Vars for new slots after SSA transformation.
     */
    void makeVarsForNewSlots(int newSlotSize, int[] newSlot2Origin) {
        assert newSlotSize == newSlot2Origin.length;
        Var[] newSlot2Var = new Var[newSlotSize];
        System.arraycopy(slot2Var, 0, newSlot2Var, 0, slot2Var.length);
        for (int i = slot2Var.length; i < newSlot2Origin.length; ++i) {
            newSlot2Var[i] = newVar(newSlot2Var[newSlot2Origin[i]].getName());
        }

        slot2Var = newSlot2Var;
    }

    // =================================================================================
    // Var Name Resolution
    // =================================================================================

    /**
     * Try to resolve the actual name of a slot.
     */
    void tryActualVarName(Var v, int slot, AbstractInsnNode insn) {
        if (existLocalVariables()) {
            if (withSyntheticName(v)) {
                Optional<String> name = getName(slot, insn);
                name.ifPresent((n) -> {
                    String realName = nameWithSuffix(n);
                    VarMutator.setName(v, realName);
                });
            } else {
                Optional<String> name = getName(slot, insn);
                name.ifPresent((n) -> {
                    String oldName = removeSuffix(v.getName());
                    if (!n.equals(oldName)) {
                        // name conflict, must because of cached const vars, use temp name
                        if (v.isConst()) {
                            Literal literal = v.getConstValue();
                            if (literal instanceof NullLiteral) {
                                VarMutator.setName(v, NULL_LITERAL);
                            } else if (isCachedInt(literal)) {
                                VarMutator.setName(v, getCachedIntName(literal));
                            }
                        }
                    }
                });
            }
        }
    }

    private String removeSuffix(String name) {
        int index = name.indexOf('#');
        if (index != -1) {
            return name.substring(0, index);
        }
        return name;
    }

    /**
     * Checks if the LocalVariables is present in the bytecode.
     * LocalVariables are used for var name resolution.
     */
    private boolean existLocalVariables() {
        return context.source.localVariables != null
                && !context.source.localVariables.isEmpty();
    }

    /**
     * Parses the LocalVariables into a queryable structure.
     */
    private Map<Pair<Integer, Integer>, LocalVariableNode>[] computeSlot2Locals() {
        if (!existLocalVariables()) {
            return null;
        }
        Map<Pair<Integer, Integer>, LocalVariableNode>[] slot2Variables = new Map[context.source.maxLocals];
        for (LocalVariableNode node : context.source.localVariables) {
            int start = AsmInsnUtils.getInsnIndex(context.source, node.start);
            int end = AsmInsnUtils.getInsnIndex(context.source, getNextTrueInsnNode(node.end));
            int slot = node.index;
            if (slot2Variables[slot] == null) {
                slot2Variables[slot] = Maps.newMap();
            }
            slot2Variables[slot].put(new Pair<>(start, end), node);
        }
        return slot2Variables;
    }

    /**
     * Resolve the source name of a var at a specific slot and instruction.
     */
    private Optional<String> getName(int slot, AbstractInsnNode insnNode) {
        if (AsmInsnUtils.isVarStore(insnNode)) {
            /*
             * for VarStore, you have to use the next InsnNode (actual JVM Bytecode)
             * as the program point to query for the variable that being stored.
             * (See the definition of start_pc of local_variable_table entry)
             */
            insnNode = getNextTrueInsnNode(insnNode);
        }
        int insnIndex = AsmInsnUtils.getInsnIndex(context.source, insnNode);
        Map<Pair<Integer, Integer>, LocalVariableNode> localVariables =
                slot2Locals[slot];
        if (localVariables == null) {
            return Optional.empty();
        }
        return localVariables.keySet().stream()
                .filter(k -> k.first() <= insnIndex && insnIndex < k.second())
                .findAny()
                .map(k -> localVariables.get(k).name);
    }

    /**
     * Skips label/frame/line nodes to find the next actual bytecode instruction.
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

    /**
     * Checks if a variable name is synthetic (auto-generated like $1 or %v1).
     */
    private static boolean withSyntheticName(Var v) {
        return v.getName().startsWith(TEMP_PREFIX) ||
                v.getName().startsWith(LOCAL_PREFIX);
    }

    /**
     * Generates a unique name by appending a suffix if the name is already used (e.g., "x#1").
     */
    private String nameWithSuffix(String name) {
        if (name2Count.containsKey(name)) {
            int count = name2Count.get(name);
            name2Count.put(name, count + 1);
            return name + "#" + count;
        } else {
            name2Count.put(name, 1);
            return name;
        }
    }

    // =================================================================================
    // Var Attributes
    // =================================================================================

    boolean isTempVar(Var v) {
        return v.getName().startsWith(TEMP_PREFIX) && v != nullLiteral;
    }

    boolean isSSAVar(Var v) {
        return varIsSSA.getOrDefault(v, false);
    }

    void setSSA(Var v) {
        varIsSSA.put(v, true);
    }

    void setNonSSA(Var v) {
        varIsSSA.put(v, false);
    }
}
