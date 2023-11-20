package pascal.taie.frontend.newfrontend.dbg;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import pascal.taie.frontend.newfrontend.BytecodeBlock;
import pascal.taie.frontend.newfrontend.Utils;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.util.collection.Pair;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BytecodeVisualizer {
    public record BytecodeGraph(List<BytecodeBlock> list,
                         BytecodeBlock entry,
                         List<Pair<List<BytecodeBlock>, BytecodeBlock>> tryCatch) {
    }

    public static String printDot(BytecodeGraph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph {\n");
        for (BytecodeBlock block : graph.list) {
            sb.append(getBlockName(block))
                    .append(" [label=\"")
                    .append(getContents(block))
                    .append("\"];\n");
        }
        for (BytecodeBlock block : graph.list) {
            for (BytecodeBlock succ : block.outEdges()) {
                sb.append(getBlockName(block))
                        .append(" -> ")
                        .append(getBlockName(succ))
                        .append(";\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public static void printDotFile(BytecodeGraph graph, String name) {
        try {
            Path p = Path.of("output", "bytecode", name + ".dot");
            Files.createDirectories(p.getParent());
            Files.writeString(p, printDot(graph));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getBlockName(BytecodeBlock block) {
        return "block" + block.hashCode();
    }

    private static String getContents(BytecodeBlock block) {
        StringBuilder sb = new StringBuilder();
        Textifier textifier = new Textifier();
        if (block.getFrame() != null) {
            sb.append(getFrameInfo(block));
        }
        sb.append("instr:\n");
        for (AbstractInsnNode insn : block.instr()) {
            TraceMethodVisitor mp = new TraceMethodVisitor(textifier);
            insn.accept(mp);
            StringWriter sw = new StringWriter();
            textifier.print(new PrintWriter(sw));
            sb.append(sw);
            sb.append("\n");
            textifier.getText().clear();
        }
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
            if (name.equals(PrimitiveType.LONG.getName()) ||
                name.equals(PrimitiveType.DOUBLE.getName())) {
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
