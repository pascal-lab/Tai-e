package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import pascal.taie.ir.IR;
import pascal.taie.util.collection.Maps;

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

        for (TryCatchBlockNode now : source.tryCatchBlocks) {
            queue.add(now.handler);
        }

        while (!queue.isEmpty()) {
            LabelNode currentBegin = queue.poll();
            if (isVisited(currentBegin)) {
                continue;
            }

            BytecodeBlock bb = getBlock(currentBegin);
            List<AbstractInsnNode> instr = bb.instr();

            AbstractInsnNode now = currentBegin.getNext();
            while (now != null) {
                if (! (now instanceof LabelNode)) {
                    instr.add(now);
                }
                if (isCFEdge(now)) {
                    break;
                }
                now = now.getNext();
            }
            collectJumpLabels(bb, now).forEach(label -> {
                BytecodeBlock target = getBlock(label);
                target.inEdges().add(bb);
                bb.outEdges().add(target);
                queue.add(label);
            });
            bb.setComplete();
        }
    }

    private BytecodeBlock getBlock(LabelNode label) {
        return label2Block.computeIfAbsent(label, this::createNewBlock);
    }

    private BytecodeBlock createNewBlock(LabelNode label) {
        return new BytecodeBlock(label, new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), null);
    }

    private boolean isVisited(LabelNode label) {
        return label2Block.containsKey(label) &&
                label2Block.get(label).isComplete();
    }

    private Stream<LabelNode> collectJumpLabels(BytecodeBlock currentBlock, AbstractInsnNode node) {
        if (node == null) {
            return Stream.empty();
        } else if (node instanceof JumpInsnNode jump) {
            if (node.getOpcode() == Opcodes.GOTO) {
                return Stream.of(jump.label);
            } else {
                return Stream.concat(Stream.of(jump.label),
                        collectFallThrough(currentBlock, node.getNext()));
            }
        } else if (node instanceof LookupSwitchInsnNode lookup) {
            return Stream.concat(Stream.of(lookup.dflt), lookup.labels.stream());
        } else if (node instanceof TableSwitchInsnNode table) {
            return Stream.concat(Stream.of(table.dflt), table.labels.stream());
        } else if (node instanceof LabelNode) {
            return collectFallThrough(currentBlock, node);
        } else {
            throw new IllegalStateException();
        }
    }

    private Stream<LabelNode> collectFallThrough(BytecodeBlock currentBlock, AbstractInsnNode node) {
        if (node == null) {
            return Stream.empty();
        } else if (node instanceof LabelNode l) {
            currentBlock.setFallThrough(getBlock(l));
            return Stream.of(l);
        } else {
            LabelNode l = createNewLabel(node);
            BytecodeBlock bb = getBlock(l);
            currentBlock.setFallThrough(bb);
            return Stream.of(l);
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
