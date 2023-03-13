package pascal.taie.frontend.newfrontend;

import org.apache.commons.lang3.NotImplementedException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
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

    private final VarManager manager;

    private final Map<Exp, AbstractInsnNode> exp2Orig;

    private final Map<AbstractInsnNode, Stmt> asm2Stmt;

    public AsmIRBuilder(JMethod method, JSRInlinerAdapter source) {
        this.method = method;
        this.source = source;
        this.manager = new VarManager(method, source.parameters, source.localVariables);
        this.asm2Stmt = Maps.newMap();
        this.exp2Orig = Maps.newMap();
    }

    public void build() {
        buildCFG();
    }

    private Stmt getAssignStmt(Var v, Exp e) {
        if (e instanceof BinaryExp binaryExp) {
            return new Binary(v, binaryExp);
        } else {
            throw new NotImplementedException();
        }
    }

    private AbstractInsnNode getOrig(Exp e) {
        return exp2Orig.get(e);
    }

    private void assocStmt(AbstractInsnNode node, Stmt stmt) {
        asm2Stmt.put(node, stmt);
    }

    private Var toVar(Exp e) {
        Var v = manager.getTempVar();
        Stmt auxStmt = getAssignStmt(v, e);
        assocStmt(getOrig(e), auxStmt);
        return v;
    }

    private Var popVar(Stack<Exp> stack) {
        Exp e = stack.pop();
        if (e instanceof Var v) {
            return v;
        } else {
            return toVar(e);
        }
    }

    private void ensureStackSafety(Stack<Exp> stack, Function<Exp, Boolean> predicate) {
        for (int i = 0; i < stack.size(); ++i) {
            Exp e = stack.get(i);
            if (predicate.apply(e)) {
                stack.set(i, toVar(e));
            }
        }
    }

    private boolean maySideEffect(Exp e) {
        return !(e instanceof Var);
    }

    private void pushExp(AbstractInsnNode node, Stack<Exp> stack, Exp e) {
        exp2Orig.put(e, node);
        ensureStackSafety(stack, this::maySideEffect);
        stack.push(e);
    }

    public void buildIR() {
        traverseBlocks();
        List<Stmt> stmts = getStmts();
        Var thisVar = manager.getThisVar();
        List<Var> params = manager.getParams();
        List<Var> vars = manager.getVars();
        Set<Var> retVars = manager.getRetVars();
        List<ExceptionEntry> entries = new ArrayList<>();
        ir = new DefaultIR(method, thisVar, params, retVars , vars, stmts, entries);
    }

    private Stmt getFirstStmt(LabelNode label) {
        BytecodeBlock block = label2Block.get(label);
        assert block != null;
        if (block.getFirstStmt() != null) {
            return block.getFirstStmt();
        }

        for (AbstractInsnNode node : block.instr()) {
            if (asm2Stmt.containsKey(node)) {
                Stmt first = asm2Stmt.get(node);
                block.setFirstStmt(first);
                return first;
            }
        }
        throw new IllegalStateException();
    }

    private void setSwitchTargets(List<LabelNode> labels, LabelNode dflt, Stmt stmt) {
        assert stmt instanceof SwitchStmt;
        SwitchStmt switchStmt = (SwitchStmt) stmt;
        List<Stmt> cases = labels.stream().map(this::getFirstStmt).toList();
        Stmt defaultStmt = getFirstStmt(dflt);
        switchStmt.setTargets(cases);
        switchStmt.setDefaultTarget(defaultStmt);
    }

    private void setJumpTargets(AbstractInsnNode node, Stmt stmt) {
        if (node instanceof JumpInsnNode jump) {
            Stmt first = getFirstStmt(jump.label);
            if (stmt instanceof Goto gotoStmt) {
                gotoStmt.setTarget(first);
            } else if (stmt instanceof If ifStmt) {
                ifStmt.setTarget(first);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (node instanceof LookupSwitchInsnNode lookup) {
            setSwitchTargets(lookup.labels, lookup.dflt, stmt);
        } else if (node instanceof TableSwitchInsnNode table) {
            setSwitchTargets(table.labels, table.dflt, stmt);
        }
        // node is not jump, do nothing
    }

    private List<Stmt> getStmts() {
        List<Stmt> res = new ArrayList<>();
        // TODO: start from 0 or 1 ?
        int counter = 0;
        for (AbstractInsnNode node : source.instructions) {
            if (asm2Stmt.containsKey(node)) {
                Stmt stmt = asm2Stmt.get(node);
                setJumpTargets(node, stmt);
                stmt.setIndex(counter++);
                res.add(stmt);
            }
        }
        return res;
    }

    private boolean inRange(int opcode, int min, int max) {
        return opcode <= min && opcode >= max;
    }
    private int unifyIfOp(int opcode) {
        if (inRange(opcode, Opcodes.IFEQ, Opcodes.IFLE)) {
            return opcode - Opcodes.IFEQ + Opcodes.IF_ACMPEQ;
        } else if (inRange(opcode, Opcodes.FCMPL, Opcodes.FCMPG)) {
            return opcode - Opcodes.FCMPL + Opcodes.DCMPL;
        } else {
            return opcode;
        }
    }

    private ConditionExp.Op toTIRCondOp(int opcode) {
        opcode = unifyIfOp(opcode);
        return switch (opcode) {
            case Opcodes.IF_ICMPEQ, Opcodes.IF_ACMPEQ, Opcodes.IFNULL -> ConditionExp.Op.EQ;
            case Opcodes.IF_ICMPNE, Opcodes.IF_ACMPNE, Opcodes.IFNONNULL -> ConditionExp.Op.NE;
            case Opcodes.IF_ICMPLT -> ConditionExp.Op.LT;
            case Opcodes.IF_ICMPGE -> ConditionExp.Op.GE;
            case Opcodes.IF_ICMPGT -> ConditionExp.Op.GT;
            case Opcodes.IF_ICMPLE -> ConditionExp.Op.LE;
            default -> throw new IllegalArgumentException();
        };
    }

    private ConditionExp getIfExp(Stack<Exp> stack, int opcode) {
        Var v1 = popVar(stack);
        Var v2;
        if (inRange(opcode, Opcodes.IFEQ, Opcodes.IFLE)) {
            v2 = manager.getZeroLiteral();
        } else if (opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL) {
            v2 = manager.getNullLiteral();
        } else {
            v2 = popVar(stack);
        }
        return new ConditionExp(toTIRCondOp(opcode), v1, v2);
    }

    private void storeExp(Stack<Exp> stack, VarInsnNode varNode) {
        int idx = varNode.var;
        Var v = manager.getLocal(idx);
        Exp top = stack.pop();
        ensureStackSafety(stack, e -> e.getUses().contains(v));
        assocStmt(varNode, getAssignStmt(v, top));
    }

    private void buildBlockStmt(BytecodeBlock block) {
        Stack<Var> inStack = block.getInStack();
        Stack<Exp> nowStack = new Stack<>();
        nowStack.addAll(inStack);

        for (AbstractInsnNode node : block.instr()) {
            if (node instanceof VarInsnNode varNode) {
                switch (varNode.getOpcode()) {
                    case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD ->
                        pushExp(node, nowStack, manager.getLocal(varNode.var));
                    case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE ->
                            storeExp(nowStack, varNode);
                    default -> // we can never reach here, JSRInlineAdapter should eliminate all rets
                            throw new IllegalStateException();
                }
            } else if (node instanceof InsnNode insnNode) {
                if (insnNode.getOpcode() == Opcodes.IADD) {
                    Var v1 = popVar(nowStack);
                    Var v2 = popVar(nowStack);
                    pushExp(node, nowStack, new ArithmeticExp(ArithmeticExp.Op.ADD, v1, v2));
                } else if (insnNode.getOpcode() == Opcodes.IRETURN) {
                    Var v1 = popVar(nowStack);
                    manager.addReturnVar(v1);
                    assocStmt(insnNode, new Return(v1));
                }
            } else if (node instanceof JumpInsnNode jump) {
                if (jump.getOpcode() == Opcodes.GOTO) {
                    assocStmt(jump, new Goto());
                } else {
                    ConditionExp cond = getIfExp(nowStack, jump.getOpcode());
                    assocStmt(jump, new If(cond));
                }
            }
        }

        if (block.getOutStack() == null) {
            // Web has not been constructed. So all the succs do not have inStack.
            block.setOutStack(regularizeStack(nowStack));
        } else {
            // TODO: merge stack
            // In the early stage of development, we assume that there is no Var remained in the nowStack.
        }
    }

    private Stack<Var> regularizeStack(Stack<Exp> origin) {
        //
        /*
            TODO: regularization, including:
            1. conversion from non-Var Exp to Var,
            2. no the same Vars in a stack.

            The conversion should have effect on the InsnNode that generated the exp.
            In the early stage of development, we assume that there is no Exp remained in the nowStack,
            so an empty Stack is enough.
         */
        return new Stack<>();
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
