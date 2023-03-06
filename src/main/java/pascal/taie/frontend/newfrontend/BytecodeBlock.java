package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.List;

public record BytecodeBlock(LabelNode start, List<AbstractInsnNode> instr) {
}
