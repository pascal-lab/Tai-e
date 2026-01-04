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

import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;

import pascal.taie.World;
import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.util.graph.Dominators;

/**
 * Holds the shared state and resources for the IR building process of a single method.
 * <p>
 * This context acts as a hub, allowing various components (like {@link BytecodeProcessor},
 * {@link StackPhiResolver}) to access core data structures (CFG, Symbol Tables, Stacks)
 * without requiring direct dependencies on each other.
 */
public class BytecodeIRBuildContext {

    // --- Immutable Resources (Input & Config) ---

    /**
     * The method to be built
     */
    public final JMethod method;

    /**
     * Bytecode input, a bytecode method represented by {@link JSRInlinerAdapter}
     */
    final JSRInlinerAdapter source;

    /**
     * Type system for frontend
     */
    public final FrontendTypeSystem typeSystem;

    /**
     * If we build SSA IR. Read from {@link pascal.taie.config.Options}
     */
    public final boolean isSSA;

    // --- Core Managers (State Containers) ---

    /**
     * Manager that manage the creation and naming of Tai-e IR variables
     */
    public final VarManager varManager;

    /**
     * Manages the mapping from bytecode instructions to their generated IR statements.
     */
    final StmtManager stmtManager;

    /**
     * Manages load/store operations on local variable slots and handles SSA-related transformations.
     */
    final SlotManager slotManager;

    /**
     * Operand stack for stack manipulation operations
     */
    final OperandStack operandStack;

    // --- Graph Data ---

    /**
     * Build CFG from ASM instructions using {@link BytecodeCFGBuilder}.
     */
    public BytecodeCFG cfg;

    /**
     * Dominator and dominator frontier computed for bytecode block graph
     */
    Dominators<BytecodeBlock> dom;

    BytecodeIRBuildContext(JMethod method, AsmMethodSource methodSource, FrontendTypeSystem typeSystem) {
        this.method = method;
        this.source = methodSource.adapter();
        this.typeSystem = typeSystem;
        this.isSSA = World.get().getOptions().isSSA();

        // Initialize Managers
        this.varManager = new VarManager(this);
        this.stmtManager = new StmtManager(this);
        this.slotManager = new SlotManager(this);
        this.operandStack = new OperandStack(this);
    }

    /**
     * Gets the index of a bytecode instruction.
     */
    int getInsnIndex(AbstractInsnNode insn) {
        assert insn != null;
        return source.instructions.indexOf(insn);
    }

    /**
     * Gets the Tai-e exception type corresponding to the internal name.
     */
    ClassType getExceptionType(String internalName) {
        if (internalName == null) {
            return typeSystem.throwableType();
        } else {
            ReferenceType r = typeSystem.fromAsmInternalName(internalName);
            if (r instanceof ClassType c) {
                return c;
            } else {
                throw new UnsupportedOperationException("Unsupported exception type: " + r);
            }
        }
    }
}
