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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static pascal.taie.frontend.java.ir.Utils.isVarStore;

/**
 * Builds a {@link BytecodeCFG} from ASM bytecode instructions.
 * <p>
 * This builder constructs the control flow graph through the following steps:
 * <ol>
 *     <li><b>Step 1</b>: Identify block boundaries by scanning instructions for
 *         control flow changes (jumps, switches, returns, throws) and try-catch
 *         boundaries. Also tracks variable access for rwTable building.</li>
 *     <li><b>Step 2</b>: Build the sorted block list, merge empty blocks, and
 *         add fall-through edges between consecutive blocks.</li>
 *     <li><b>Step 3</b>: Add jump and switch edges based on the last instruction
 *         of each block.</li>
 *     <li><b>Step 4</b>: Add exception edges from try blocks to their handlers.</li>
 * </ol>
 * <p>
 * The builder uses callbacks ({@link InstructionVisitor}) to decouple CFG construction
 * from IR-specific processing like rwTable building.
 */
final class BytecodeCFGBuilder {

    /**
     * Callback interface for visiting variable-accessing instructions during CFG construction.
     * This allows the caller to perform additional processing (e.g., rwTable building)
     * without coupling CFG construction to IR-specific logic.
     */
    @FunctionalInterface
    interface InstructionVisitor {
        /**
         * Called for each variable-accessing instruction (LOAD, STORE, IINC).
         *
         * @param index   the instruction index in the bytecode array
         * @param varSlot the local variable slot number being accessed
         * @param isRead  {@code true} if reading from the variable,
         *                {@code false} if writing to it
         */
        void visitVarInsn(int index, int varSlot, boolean isRead);
    }

    // ==================== Fields ====================

    /**
     * The ASM method adapter containing bytecode instructions.
     * JSRInlinerAdapter ensures JSR/RET instructions are inlined.
     */
    private final JSRInlinerAdapter source;

    /**
     * Callback for visiting variable-accessing instructions.
     * Used by {@link BytecodeIRBuilder} to build the rwTable.
     */
    private final InstructionVisitor visitor;

    /**
     * Function to resolve exception type names to {@link ClassType}.
     * Maps internal names (e.g., "java/lang/Exception") to ClassType,
     * or {@code null} to the default Throwable type.
     */
    private final Function<String, ClassType> exceptionTypeResolver;

    /**
     * Mapping from bytecode instruction index to its containing block.
     * <p>
     * During construction:
     * <ul>
     *     <li>Non-null entries mark block start positions</li>
     *     <li>After Step 2, empty blocks are merged (their entries point to successor blocks)</li>
     *     <li>Blocks with {@code index == -1} are empty/merged and should be skipped</li>
     * </ul>
     */
    private BytecodeBlock[] insn2Block;

    /**
     * Counter for the maximum number of blocks created.
     * Used to pre-allocate edge arrays in {@link BytecodeCFG}.
     */
    private int maxBlockCounter = 0;

    // ==================== Constructor ====================

    BytecodeCFGBuilder(JSRInlinerAdapter source, InstructionVisitor visitor,
                       Function<String, ClassType> exceptionTypeResolver) {
        this.source = source;
        this.visitor = visitor;
        this.exceptionTypeResolver = exceptionTypeResolver;
    }

    // ==================== Main Build Method ====================

    /**
     * Builds the CFG from the source bytecode instructions.
     * <p>
     * The construction process:
     * <ol>
     *     <li>Initialize {@link #insn2Block} and create entry block</li>
     *     <li>Create blocks for try-catch boundaries</li>
     *     <li>Scan all instructions to identify block boundaries based on:
     *         <ul>
     *             <li>Jump instructions (GOTO, IF_*)</li>
     *             <li>Switch instructions (TABLESWITCH, LOOKUPSWITCH)</li>
     *             <li>Return/throw instructions</li>
     *             <li>Variable stores inside try blocks (for exception safety)</li>
     *         </ul>
     *     </li>
     *     <li>Build sorted block list, merge empty blocks, add fall-through edges</li>
     *     <li>Add jump/switch target edges</li>
     *     <li>Add exception handler edges</li>
     * </ol>
     *
     * @return the built CFG, or {@code null} if source has no instructions
     */
    BytecodeCFG build() {
        int size = source.instructions.size();
        if (size == 0) {
            return null;
        }

        insn2Block = new BytecodeBlock[size];
        // fallThroughTable[i] == true means instruction i falls through to i+1
        boolean[] fallThroughTable = new boolean[size];
        Arrays.fill(fallThroughTable, true);

        AbstractInsnNode begin = source.instructions.getFirst();
        if (begin == null) {
            return null;
        }
        insn2Block[0] = getOrCreateBlock(begin);
        BytecodeBlock entry = insn2Block[0];

        // Create blocks for try-catch boundaries (start, end, handler)
        for (TryCatchBlockNode tcb : source.tryCatchBlocks) {
            getOrCreateBlock(tcb.start);
            getOrCreateBlock(tcb.end);
            getOrCreateBlock(tcb.handler);
        }

        // Build exception switch table for tracking whether we're inside a try block.
        // This is needed because variable stores inside try blocks require
        // block splitting for correct exception handling semantics.
        FlattenExceptionTable fet = new FlattenExceptionTable(source);
        Pair<int[], Integer> exceptionSwitchesPair = fet.buildExceptionSwitches();
        int[] trySwitch = exceptionSwitchesPair.first();
        int trySwitchSize = exceptionSwitchesPair.second();

        // Step 1: Scan instructions to identify block boundaries
        boolean inTry = false;
        int trySwitchIndex = 0;
        for (int i = 0; i < size; ++i) {
            // Update inTry status by processing trySwitch events at this index.
            // Each event toggles the inTry state (entering or leaving a try block).
            while (trySwitchIndex < trySwitchSize && trySwitch[trySwitchIndex] == i) {
                inTry = !inTry;
                trySwitchIndex++;
            }

            AbstractInsnNode insn = source.instructions.get(i);
            // needNoBlock: if false, the next instruction starts a new block
            boolean needNoBlock = true;
            // splitBefore: if true, this instruction starts a new block
            boolean splitBefore = false;

            if (insn instanceof JumpInsnNode jmp) {
                // Jump target needs a block; fall-through only for conditional jumps
                getOrCreateBlock(jmp.label);
                needNoBlock = false;
                fallThroughTable[i] = jmp.getOpcode() != Opcodes.GOTO;
            } else if (insn instanceof LookupSwitchInsnNode lookup) {
                // All switch targets need blocks; no fall-through
                getOrCreateBlock(lookup.dflt);
                for (LabelNode label : lookup.labels) {
                    getOrCreateBlock(label);
                }
                needNoBlock = false;
                fallThroughTable[i] = false;
            } else if (insn instanceof TableSwitchInsnNode table) {
                // All switch targets need blocks; no fall-through
                getOrCreateBlock(table.dflt);
                for (LabelNode label : table.labels) {
                    getOrCreateBlock(label);
                }
                needNoBlock = false;
                fallThroughTable[i] = false;
            } else {
                // Check for return/throw instructions (inlined from Utils::isReturnOrThrow)
                // Note: If ASM version is upgraded, verify opcode ranges are still valid
                if (insn instanceof InsnNode insnNode) {
                    int opcode = insnNode.getOpcode();
                    if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                            || opcode == Opcodes.ATHROW) {
                        needNoBlock = false;
                        fallThroughTable[i] = false;
                    }
                } else if (insn instanceof VarInsnNode varNode) {
                    // Variable stores inside try blocks need block splitting for
                    // correct exception handling (the store must be atomic w.r.t. exceptions)
                    if (inTry && isVarStore(varNode)) {
                        splitBefore = true;
                    }
                    // Notify visitor for rwTable building
                    visitor.visitVarInsn(i, varNode.var, !isVarStore(varNode));
                } else if (insn instanceof IincInsnNode iincNode) {
                    // IINC is both a read and write; needs splitting inside try blocks
                    if (inTry) {
                        splitBefore = true;
                    }
                    visitor.visitVarInsn(i, iincNode.var, true);  // read
                    visitor.visitVarInsn(i, iincNode.var, false); // write
                }
            }

            // Create block for next instruction if current instruction breaks flow
            if (!needNoBlock) {
                AbstractInsnNode next = insn.getNext();
                if (next != null) {
                    getOrCreateBlock(next);
                }
            }
            // Create block at current instruction if splitting is needed
            if (splitBefore) {
                getOrCreateBlock(insn);
            }
        }

        // Step 2: Build sorted block list and add fall-through edges
        List<BytecodeBlock> sortedBlockList = new ArrayList<>(size / 4);
        BytecodeCFG cfg = new BytecodeCFG(maxBlockCounter, entry, sortedBlockList, insn2Block);

        // edgeInsn[blockIndex] = the last real instruction of that block
        // (for edge processing)
        AbstractInsnNode[] edgeInsn = new AbstractInsnNode[size];
        BytecodeBlock current = insn2Block[0];
        assert current != null;
        int start = 0;

        for (int end = 1; end < size; ++end) {
            if (insn2Block[end] != null) {
                // Found a block boundary at 'end', process block [start, end)
                AbstractInsnNode edge = source.instructions.get(end - 1);

                // Find the last real instruction in [start, end) for edge processing.
                // Skip pseudo-instructions (opcode == -1) like labels and line numbers.
                for (int curr = end - 1; curr >= start; --curr) {
                    edge = source.instructions.get(curr);
                    if (edge.getOpcode() != -1) {
                        break;
                    }
                }

                if (edge.getOpcode() == -1 && current != entry) {
                    // Block contains only pseudo-instructions (empty block).
                    // Merge with next block by updating insn2Block mapping.
                    insn2Block[start] = insn2Block[end];
                    current = insn2Block[end];
                } else {
                    // Process current block: assign index, record edge instruction, add to list
                    int counter = sortedBlockList.size();
                    current.setIndex(counter);
                    edgeInsn[counter] = edge;
                    sortedBlockList.add(current);
                    current.setInstr(new BytecodeListSlice(source.instructions, start, end));

                    // Add fall-through edge if the last instruction falls through
                    if (fallThroughTable[end - 1]) {
                        cfg.addEdge(counter, counter + 1);
                    }
                    current = insn2Block[end];
                    start = end;
                }
            }
        }

        // Handle the last block [start, size)
        boolean emptyLast = start == size - 1
                && source.instructions.getLast().getOpcode() == -1;
        if (!emptyLast) {
            int counter = sortedBlockList.size();
            current.setIndex(counter);
            edgeInsn[counter] = source.instructions.getLast();
            sortedBlockList.add(current);
            current.setInstr(new BytecodeListSlice(source.instructions, start, size));
        }

        // Step 3: Add jump/switch edges based on last instruction of each block
        for (int i = 0; i < sortedBlockList.size(); ++i) {
            AbstractInsnNode insn = edgeInsn[i];
            BytecodeBlock bb = sortedBlockList.get(i);
            if (insn instanceof JumpInsnNode jmp) {
                addEdge(cfg, bb, jmp.label);
            } else if (insn instanceof LookupSwitchInsnNode lookup) {
                addEdges(cfg, bb, lookup.labels);
                addEdge(cfg, bb, lookup.dflt);
            } else if (insn instanceof TableSwitchInsnNode table) {
                addEdges(cfg, bb, table.labels);
                addEdge(cfg, bb, table.dflt);
            }
        }

        // Step 4: Add exception edges from try blocks to handlers
        addExceptionEdges(cfg, sortedBlockList);

        return cfg;
    }

    // ==================== Exception Edge Processing ====================

    /**
     * Adds exception edges from all blocks in try regions to their handlers.
     * <p>
     * For each try-catch block, adds edges from every block in the try region
     * [start, end) to the handler block. Also sets the exception type on the
     * handler block.
     */
    private void addExceptionEdges(BytecodeCFG cfg, List<BytecodeBlock> sortedBlockList) {
        for (TryCatchBlockNode tcb : source.tryCatchBlocks) {
            BytecodeBlock handler = searchForValidBlock(tcb.handler);
            BytecodeBlock start = searchForValidBlock(tcb.start);
            int end = searchForValidBlockOrEnd(tcb.end, sortedBlockList.size());
            // Add exception edge from each block in [start, end) to handler
            for (int i = start.getIndex(); i < end; ++i) {
                cfg.addExceptionEdge(i, handler.getIndex());
            }
            // Set the caught exception type on the handler block
            handler.addExceptionHandlerType(exceptionTypeResolver.apply(tcb.type));
        }
    }

    // ==================== Block Creation ====================

    /**
     * Gets or creates a block starting at the given instruction.
     * <p>
     * If no block exists at the instruction's index, creates a new one.
     * For non-label instructions, creates a synthetic label that links
     * to the instruction.
     *
     * @param node the instruction where the block should start
     * @return the existing or newly created block
     */
    private BytecodeBlock getOrCreateBlock(AbstractInsnNode node) {
        int idx = getIndex(node);
        if (insn2Block[idx] == null) {
            LabelNode labelNode = (node instanceof LabelNode ln)
                    ? ln
                    : createSyntheticLabel(node);
            insn2Block[idx] = new BytecodeBlock(labelNode);
            ++maxBlockCounter;
        }
        return insn2Block[idx];
    }

    /**
     * Creates a synthetic label for a non-label instruction.
     * <p>
     * The synthetic label overrides {@link LabelNode#getNext()} to return
     * the original instruction, maintaining the instruction chain.
     *
     * @param next the instruction this label should point to
     */
    private static LabelNode createSyntheticLabel(AbstractInsnNode next) {
        return new LabelNode() {
            @Override
            public AbstractInsnNode getNext() {
                return next;
            }
        };
    }

    // ==================== Edge Addition ====================

    /**
     * Adds a control flow edge from a block to a label target.
     * <p>
     * If the target label points to an empty/merged block (index == -1),
     * searches forward to find the actual valid target block.
     */
    private void addEdge(BytecodeCFG cfg, BytecodeBlock from, LabelNode target) {
        BytecodeBlock to = insn2Block[getIndex(target)];
        assert to != null;
        if (to.getIndex() == -1) {
            // Target is an empty/merged block - find the valid successor
            to = searchForValidBlock(target);
        }
        cfg.addEdge(from.getIndex(), to.getIndex());
    }

    /**
     * Adds control flow edges from a block to multiple label targets.
     */
    private void addEdges(BytecodeCFG cfg, BytecodeBlock from, List<LabelNode> targets) {
        for (LabelNode target : targets) {
            addEdge(cfg, from, target);
        }
    }

    // ==================== Block Search ====================

    /**
     * Searches for the first valid block starting from the given label.
     * <p>
     * Skips empty/merged blocks (those with {@code index == -1} or null entries)
     * by scanning forward in the instruction array.
     */
    private BytecodeBlock searchForValidBlock(LabelNode startLabel) {
        int idx = getIndex(startLabel);
        while (insn2Block[idx] == null || insn2Block[idx].getIndex() == -1) {
            idx++;
        }
        return insn2Block[idx];
    }

    /**
     * Searches for the first valid block index starting from the given label,
     * or returns {@code blockCount} if no valid block is found.
     * <p>
     * Used for try-catch end boundaries, which may point past the last block.
     */
    private int searchForValidBlockOrEnd(LabelNode startLabel, int blockCount) {
        int idx = getIndex(startLabel);
        int maxSize = insn2Block.length;
        while (idx < maxSize) {
            if (insn2Block[idx] != null && insn2Block[idx].getIndex() != -1) {
                return insn2Block[idx].getIndex();
            }
            idx++;
        }
        return blockCount;
    }

    // ==================== Utility ====================

    /**
     * Gets the index of an instruction in the bytecode array.
     */
    private int getIndex(AbstractInsnNode node) {
        return source.instructions.indexOf(node);
    }
}
