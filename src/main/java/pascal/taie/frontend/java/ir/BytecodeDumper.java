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
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.Type;
import pascal.taie.util.Indexer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static pascal.taie.frontend.java.ir.Top.TOP;
import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.FloatType.FLOAT;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;

/**
 * A utility class to dump the bytecode CFG to DOT graph.
 * Currently only triggered by {@link BytecodeIRBuilder#dump()}.
 */
class BytecodeDumper {

    static void printDotFile(BytecodeCFG graph, Indexer<AbstractInsnNode> indexer, String name) {
        try {
            if (name.length() > 200) {
                name = name.substring(0, 200);
            }
            Path p = Path.of("output", "bytecode", name + ".dot");
            Files.createDirectories(p.getParent());
            Files.writeString(p, toDot(graph, indexer));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toDot(BytecodeCFG graph, Indexer<AbstractInsnNode> insnIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph {\n");
        for (BytecodeBlock block : graph.getBlocks()) {
            sb.append(getBlockName(block))
                    .append(" [label=\"")
                    .append(getBlockDisplayName(block))
                    .append("\n")
                    .append(getContents(block, insnIndex))
                    .append("\"];\n");
        }
        for (BytecodeBlock block : graph.getBlocks()) {
            for (int i = 0; i < graph.getOutDegree(block.getIndex()); i++) {
                int succIndex = graph.getSucc(block.getIndex(), i);
                BytecodeBlock succ = graph.getNode(succIndex);
                sb.append(getBlockName(block))
                        .append(" -> ")
                        .append(getBlockName(succ))
                        .append(";\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String getBlockName(BytecodeBlock block) {
        return "block" + block.hashCode();
    }

    private static String getBlockDisplayName(BytecodeBlock block) {
        return "block" + " (" + block.getIndex() + ")";
    }

    private static String getContents(BytecodeBlock block,
                                      Indexer<AbstractInsnNode> indexer) {
        StringBuilder sb = new StringBuilder();
        if (block.getFrame() != null) {
            sb.append(getFrameInfo(block));
        }
        sb.append("instr:\n");
        int count = 0;
        for (AbstractInsnNode insn : block.instr()) {
            sb.append("[").append(count++)
                    .append(" @ ")
                    .append(indexer.getIndex(insn))
                    .append("]").append("   ");
            sb.append(formatInsn(insn));
            sb.append("\n");
        }
        return sb.toString().replace("\"", "\\\"");
    }

    static String formatInsn(AbstractInsnNode insn) {
        Textifier textifier = new Textifier();
        StringBuilder sb = new StringBuilder();
        TraceMethodVisitor mp = new TraceMethodVisitor(textifier);
        insn.accept(mp);
        StringWriter sw = new StringWriter();
        textifier.print(new PrintWriter(sw));
        sb.append(sw);
        textifier.getText().clear();
        return sb.toString();
    }

    private static String getFrameInfo(BytecodeBlock block) {
        assert block.getFrame() != null;
        StringBuilder sb = new StringBuilder();
        sb.append("stack: [");
        List<Object> stack = block.getFrame().stack;
        for (int i = 0; i < stack.size(); i++) {
            sb.append(i).append("->").append(fromAsmFrameType(stack.get(i)).getName());
            if (i != stack.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]\n");
        int n = 0;
        sb.append("local: [");
        for (int i = 0; i < block.getFrame().local.size(); i++) {
            Object o = block.getFrame().local.get(i);
            String name = fromAsmFrameType(o).getName();
            if (name.equals(LONG.getName()) ||
                name.equals(DOUBLE.getName())) {
                n++;
            }
            sb.append(n).append("->").append(name);
            n++;
            if (i != block.getFrame().local.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]\n");
        return sb.toString();
    }

    /**
     * Convert ASM frame type to Type.
     */
    private static Type fromAsmFrameType(Object o) {
        if (o instanceof Integer i) {
            return switch (i) {
                case 0 -> TOP; // Opcodes.Top
                case 1 -> INT; // Opcodes.INTEGER
                case 2 -> FLOAT; // Opcodes.FLOAT
                case 3 -> DOUBLE; // Opcodes.DOUBLE
                case 4 -> LONG; // Opcodes.LONG
                case 5 -> NullType.NULL; // Opcodes.NULL
                case 6 -> Uninitialized.UNINITIALIZED; // Opcodes.UNINITIALIZED_THIS
                default -> throw new IllegalArgumentException();
            };
        } else if (o instanceof LabelNode) {
            return Uninitialized.UNINITIALIZED;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private enum Uninitialized implements Type {

        UNINITIALIZED;

        @Override
        public String getName() {
            return "<uninitialized-type>";
        }
    }
}
