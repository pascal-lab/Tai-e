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

package pascal.taie.frontend.newfrontend.bcir;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import pascal.taie.frontend.newfrontend.Utils;
import pascal.taie.util.Indexer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.LongType.LONG;

/**
 * A utility class to visualize the bytecode graph in DOT format.
 * Currently only triggered by {@link BytecodeIRBuilder#dump()}.
 */
public class BytecodeVisualizer {

    public static String printDot(BytecodeGraph graph, Indexer<AbstractInsnNode> insnIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph {\n");
        for (BytecodeBlock block : graph.getBlockSortedList()) {
            sb.append(getBlockName(block))
                    .append(" [label=\"")
                    .append(getBlockDisplayName(block))
                    .append("\n")
                    .append(getContents(graph, block, insnIndex))
                    .append("\"];\n");
        }
        for (BytecodeBlock block : graph.getBlockSortedList()) {
            for (int i = 0; i < graph.getOutEdgesCount(block.getIndex()); i++) {
                int succIndex = graph.getOutEdge(block.getIndex(), i);
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

    public static void printDotFile(BytecodeGraph graph, Indexer<AbstractInsnNode> indexer, String name) {
        try {
            if (name.length() > 200) {
                name = name.substring(0, 200);
            }
            Path p = Path.of("output", "bytecode", name + ".dot");
            Files.createDirectories(p.getParent());
            Files.writeString(p, printDot(graph, indexer));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getBlockName(BytecodeBlock block) {
        return "block" + block.hashCode();
    }

    private static String getBlockDisplayName(BytecodeBlock block) {
        return "block" + " (" + block.getIndex() + ")";
    }

    private static String getContents(BytecodeGraph g, BytecodeBlock block, Indexer<AbstractInsnNode> indexer) {
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
            sb.append(printInsn(insn));
            sb.append("\n");
        }
        return sb.toString().replace("\"", "\\\"");
    }

    public static String printInsn(AbstractInsnNode insn) {
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
            sb.append(i).append("->").append(
                    Utils.fromAsmFrameType(stack.get(i)).getName());
            if (i != stack.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]\n");
        int n = 0;
        sb.append("local: [");
        for (int i = 0; i < block.getFrame().local.size(); i++) {
            Object o = block.getFrame().local.get(i);
            String name = Utils.fromAsmFrameType(o).getName();
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
}
