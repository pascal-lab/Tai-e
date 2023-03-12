package pascal.taie.frontend.newfrontend;

import org.apache.commons.lang3.NotImplementedException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Stream;

import static pascal.taie.frontend.newfrontend.Utils.*;

public class AsmIRBuilder {

    private IR ir;

    private final JMethod method;

    private final JSRInlinerAdapter source;

    public Map<LabelNode, BytecodeBlock> getLabel2Block() {
        return label2Block;
    }

    private Map<LabelNode, BytecodeBlock> label2Block;

    private LabelNode entry;

    private VarManger manger;

    private Map<Exp, AbstractInsnNode> exp2Orig;

    private Map<AbstractInsnNode, Stmt> asm2Stmt;

    public AsmIRBuilder(JMethod method, JSRInlinerAdapter source) {
        this.method = method;
        this.source = source;
    }

    public void build() {
        buildCFG();
    }

    private LocalVariableNode searchLocal(int index) {
        for (LocalVariableNode node : source.localVariables) {
            if (node.index == index) {
                return node;
            }
        }
        throw new IllegalArgumentException();
    }

    private @Nullable Type getLocalType(int i) {
        if (source.localVariables == null) {
            return null;
        } else {
            String sig = searchLocal(i).signature;
            return BuildContext.get().fromAsmType(sig);
        }
    }

    private @Nullable String getLocalName(int i) {
        if (source.localVariables == null) {
            return null;
        } else {
            return searchLocal(i).name;
        }
    }

    private Stmt getAssignStmt(Var v, Exp e) {
        if (e instanceof BinaryExp binaryExp) {
            return new Binary(v, binaryExp);
        } else {
            throw new NotImplementedException();
        }
    }

    private Var getLocalOrParam(int i) {
        return manger.getLocal(i, getLocalName(i), getLocalType(i));
    }

    private Var getTempVar() {
        return manger.getTempVar();
    }

    private AbstractInsnNode getOrig(Exp e) {
        return exp2Orig.get(e);
    }

    private void assocStmt(AbstractInsnNode node, Stmt stmt) {
        asm2Stmt.put(node, stmt);
    }

    private Var popVar(Stack<Exp> stack) {
        Exp e = stack.pop();
        if (e instanceof Var v) {
            return v;
        } else {
            Var v = getTempVar();
            Stmt auxStmt = getAssignStmt(v, e);
            assocStmt(getOrig(e), auxStmt);
            return v;
        }
    }

    private void pushExp(AbstractInsnNode node, Stack<Exp> stack, Exp e) {
        exp2Orig.put(e, node);
        stack.push(e);
    }

    private void buildIR() {
        Var thisVar = manger.getThisVar();
        List<Var> params = manger.getParams();
        List<Var> vars = manger.getVars();
        List<Stmt> stmts = new ArrayList<>();
        Set<Var> retVars = manger.getRetVars();
        List<ExceptionEntry> entries = new ArrayList<>();
        ir = new DefaultIR(method, thisVar, params, retVars , vars, stmts, entries);
    }

    private void buildBlockStmt(BytecodeBlock block) {
        Stack<Var> inStack = block.getInStack();
        Stack<Exp> nowStack = new Stack<>();
        nowStack.addAll(inStack);

        for (AbstractInsnNode node : block.instr()) {
            if (node instanceof VarInsnNode varNode) {
                if (varNode.getOpcode() == Opcodes.ILOAD) {
                    nowStack.push(getLocalOrParam(varNode.var));
                }
            } else if (node instanceof InsnNode insnNode) {
                if (insnNode.getOpcode() == Opcodes.IADD) {
                    Var v1 = popVar(nowStack);
                    Var v2 = popVar(nowStack);
                    pushExp(node, nowStack, new ArithmeticExp(ArithmeticExp.Op.ADD, v1, v2));
                }
            }
        }
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
                if (! (now instanceof LabelNode) && ! (now instanceof LineNumberNode)) {
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
        if (node == null || isReturn(node) || isThrow(node)) {
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

    private void traverseBlocks() {
        Set<BytecodeBlock> visited = new HashSet<>();
        BytecodeBlock entry = label2Block.get(this.entry);
        entry.setInStack(new Stack<>());

        Queue<BytecodeBlock> workList = new LinkedList<>();
        workList.offer(entry);

        while (workList.peek() != null) {
            BytecodeBlock bb = workList.poll();
            visited.add(bb);

            buildBlockStmt(bb);

            for (BytecodeBlock succ : bb.outEdges()) {
                if (!visited.contains(succ)) {
                    workList.offer(succ);
                }
            }
        }
    }

    public IR getIr() {
        return ir;
    }


}
