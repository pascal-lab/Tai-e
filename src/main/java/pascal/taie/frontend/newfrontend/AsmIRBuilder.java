package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import pascal.taie.ir.IR;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Stream;

import static pascal.taie.frontend.newfrontend.Utils.isCFEdge;

public class AsmIRBuilder {

    private IR ir;

    private final JSRInlinerAdapter source;

    public Map<LabelNode, BytecodeBlock> getLabel2Block() {
        return label2Block;
    }

    private Map<LabelNode, BytecodeBlock> label2Block;

    private LabelNode entry;

    public AsmIRBuilder(JSRInlinerAdapter source) {
        this.source = source;
    }

    public void build() {
        buildCFG();
    }

    private void buildCFG() {
        label2Block = Maps.newMap();

        AbstractInsnNode begin = source.instructions.getFirst();
        Queue<LabelNode> queue = new LinkedList<>();
        if (begin == null) {
            return;
        }

        if (begin instanceof LabelNode l) {
            entry = l;
        } else {
            entry = createNewLabel(begin);
        }

        queue.add(entry);

        while (!queue.isEmpty()) {
            LabelNode currentBegin = queue.poll();
            if (label2Block.containsKey(currentBegin)) {
                continue;
            }

            List<AbstractInsnNode> block = new ArrayList<>();
            label2Block.put(currentBegin, new BytecodeBlock(currentBegin, block));

            AbstractInsnNode now = currentBegin.getNext();
            while (now != null) {
                if (! (now instanceof LabelNode)) {
                    block.add(now);
                }
                if (isCFEdge(now)) {
                    break;
                }
                now = now.getNext();
            }
            collectJumpLabels(now).forEach(queue::add);
        }
    }

    private Stream<LabelNode> collectJumpLabels(AbstractInsnNode node) {
        if (node == null) {
            return Stream.empty();
        } else if (node instanceof JumpInsnNode jump) {
            if (node.getOpcode() == Opcodes.GOTO) {
                return Stream.of(jump.label);
            } else {
                return Stream.concat(Stream.of(jump.label),
                        collectNextLabel(node.getNext()));
            }
        } else if (node instanceof LookupSwitchInsnNode lookup) {
            return Stream.concat(Stream.of(lookup.dflt), lookup.labels.stream());
        } else if (node instanceof TableSwitchInsnNode table) {
            return Stream.concat(Stream.of(table.dflt), table.labels.stream());
        } else if (node instanceof LabelNode) {
            return collectNextLabel(node);
        } else {
            throw new IllegalStateException();
        }
    }

    private Stream<LabelNode> collectNextLabel(AbstractInsnNode node) {
        if (node == null) {
            return Stream.empty();
        } else if (node instanceof LabelNode l) {
            return Stream.of(l);
        } else {
            return Stream.of(createNewLabel(node));
        }
    }

    private LabelNode createNewLabel(AbstractInsnNode next) {
        LabelNode node = new LabelNode() {
            @Override
            public AbstractInsnNode getNext() {
                return next;
            }
        };

        return node;
    }

    public IR getIr() {
        return ir;
    }


}
