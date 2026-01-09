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
import pascal.taie.language.type.NullType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


import static pascal.taie.language.type.IntType.INT;

public class VarManager {

    private static final String LOCAL_PREFIX = "$";

    // TODO: use another method to avoid local var has same prefix
    private static final String TEMP_PREFIX = "%";

    private static final String THIS = "this";

    private static final String NULL_LITERAL = "$null";


    private final IRBuilderContext context;


    private final int INT_CACHE_LOW = -8;

    private final int INT_CACHE_HIGH = 7;

    private final Var[] intConstVarCache;

    private @Nullable Var nullLiteral;


    /**
     * slot2Variable[slot].get(range) = local variable node.
     * Maps from slot and range to the concrete local variable.
     */
    private final Map<Pair<Integer, Integer>, LocalVariableNode>[] slot2Locals;

    private final BitSet varIsSSA;

    private final Map<String, Integer> name2Count;


    private int counter;

    private final List<Var> allVars;

    private Var[] slot2Var;

    private final Set<Var> slotVars;


    private final List<Var> params;

    private final Set<Var> retVars;

    private final @Nullable Var thisVar;

    VarManager(IRBuilderContext context) {
        this.context = context;

        this.intConstVarCache = new Var[-INT_CACHE_LOW + 1 + INT_CACHE_HIGH];

        this.counter = 0;
        this.allVars = new ArrayList<>(context.source.maxLocals * 6);

        this.params = new ArrayList<>();
        this.retVars = Sets.newSet();

        this.varIsSSA = new BitSet();
        this.slot2Locals = computeSlot2Locals();
        this.name2Count = Maps.newMap();

        this.slot2Var = new Var[context.source.maxLocals];
        this.slotVars = Sets.newSet();
        for (int slot = 0; slot < context.source.maxLocals; ++slot) {
            String name;
            if (slot == 0) {
                name = this.context.method.isStatic() ? LOCAL_PREFIX + slot : THIS;
            } else {
                name = LOCAL_PREFIX + slot;
            }
            Var v = newVar(name);
            slotVars.add(v);
            slot2Var[slot] = v;
            setNonSSA(v);
        }

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
        }
    }

    void makeVarsForNewSlots(int newSlotSize, int[] newSlot2Origin) {
        assert newSlotSize == newSlot2Origin.length;
        Var[] newSlot2Var = new Var[newSlotSize];
        System.arraycopy(slot2Var, 0, newSlot2Var, 0, slot2Var.length);
        for (int i = slot2Var.length; i < newSlot2Origin.length; ++i) {
            newSlot2Var[i] = newVar(nameWithSuffix(newSlot2Var[newSlot2Origin[i]].getName()));
            slotVars.add(newSlot2Var[i]);
        }

        slot2Var = newSlot2Var;
    }

    boolean existLocalVariables() {
        return context.source.localVariables != null
                && !context.source.localVariables.isEmpty();
    }

    Var[] getIntConstVarCache() {
        return intConstVarCache;
    }

    private Map<Pair<Integer, Integer>, LocalVariableNode>[] computeSlot2Locals() {
        if (!existLocalVariables()) {
            return null;
        }
        Map<Pair<Integer, Integer>, LocalVariableNode>[] slot2Variables = new Map[context.source.maxLocals];
        for (LocalVariableNode node : context.source.localVariables) {
            int start = context.getInsnIndex(node.start);
            int end = context.getInsnIndex(getNextTrueInsnNode(node.end));
            int slot = node.index;
            if (slot2Variables[slot] == null) {
                slot2Variables[slot] = Maps.newMap();
            }
            slot2Variables[slot].put(new Pair<>(start, end), node);
        }
        return slot2Variables;
    }

    Optional<String> getName(int slot, AbstractInsnNode insnNode) {
        if (Utils.isVarStore(insnNode)) {
            /*
             * for VarStore, you have to use the next InsnNode (actual JVM Bytecode)
             * as the program point to query for the variable that being stored.
             * (See the definition of start_pc of local_variable_table entry)
             */
            insnNode = getNextTrueInsnNode(insnNode);
        }
        int insnIndex = context.getInsnIndex(insnNode);
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
     * Get the TIR var for a <code>this</code> variable, parameter or local variable
     *
     * @param slot index of variable in bytecode
     * @return the corresponding TIR variable
     */
    Var getVar(int slot) {
        Var v = slot2Var[slot];
        assert v != null;
        return v;
    }

    Var getNullLiteral() {
        if (nullLiteral == null) {
            nullLiteral = newConstVar(NULL_LITERAL, NullLiteral.get());
            VarMutator.setType(nullLiteral, NullType.NULL);
        }
        return nullLiteral;
    }

    boolean shouldCacheConst(Literal literal) {
        // TODO: should we include $null here?
        return literal instanceof IntLiteral intLiteral
                && INT_CACHE_LOW <= intLiteral.getValue()
                && intLiteral.getValue() <= INT_CACHE_HIGH;
    }

    Var getConstVar(Literal literal) {
        if (literal instanceof NullLiteral) {
            return getNullLiteral();
        } else if (shouldCacheConst(literal)) {
            IntLiteral intLiteral = (IntLiteral) literal;
            int value = intLiteral.getValue();
            int index = value - INT_CACHE_LOW;
            if (intConstVarCache[index] == null) {
                String name = TEMP_PREFIX + "c" + "i" + value;
                Var v = new Var(context.method, name, INT, counter++, IntLiteral.get(value));
                intConstVarCache[index] = v;
                allVars.add(intConstVarCache[index]);
            }
            return intConstVarCache[index];
        } else {
            return newConstVar(getConstVarName(), literal);
        }
    }

    boolean isTempVar(Var v) {
        return v.getName().startsWith(TEMP_PREFIX) && v != nullLiteral;
    }

    static boolean mayRename(Var v) {
        return v.getName().startsWith(TEMP_PREFIX) ||
                v.getName().startsWith(LOCAL_PREFIX);
    }

    void aliasForSlot(Var var) {
        assert !slotVars.contains(var);
        slotVars.add(var);
    }

    boolean isForSlot(Var v) {
        return slotVars.contains(v);
    }

    private String getConstVarName() {
        return TEMP_PREFIX + "c" + counter;
    }

    private Var newVar(String name) {
        Var v = new Var(context.method, name, null, counter++);
        allVars.add(v);
        return v;
    }

    private Var newConstVar(String name, Literal literal) {
        Var v = new Var(context.method, name, literal.getType(), counter++, literal);
        allVars.add(v);
        setNonSSA(v);
        return v;
    }

    String nameWithSuffix(String name) {
        if (name2Count.containsKey(name)) {
            int count = name2Count.get(name);
            name2Count.put(name, count + 1);
            return name + "#" + count;
        } else {
            name2Count.put(name, 1);
            return name;
        }
    }

    boolean isSSAVar(Var v) {
        return varIsSSA.get(v.getIndex());
    }

    void setSSA(Var v) {
        varIsSSA.set(v.getIndex(), true);
    }

    void setNonSSA(Var v) {
        varIsSSA.set(v.getIndex(), false);
    }
}
