package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;

import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import pascal.taie.frontend.newfrontend.exposed.WorldParaHolder;
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.ir.exp.NewArray;
import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.NewMultiArray;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.StaticFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Goto;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.LookupSwitch;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.ir.stmt.TableSwitch;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;
import soot.SootMethod;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static pascal.taie.frontend.newfrontend.MethodCallBuilder.getInitRef;
import static pascal.taie.frontend.newfrontend.MethodCallBuilder.getMethodRef;
import static pascal.taie.frontend.newfrontend.TypeUtils.JDTTypeToTaieType;
import static pascal.taie.frontend.newfrontend.TypeUtils.getIndexOfPrimitive;
import static pascal.taie.frontend.newfrontend.TypeUtils.getJREMethod;
import static pascal.taie.frontend.newfrontend.TypeUtils.getPrimitiveByIndex;
import static pascal.taie.frontend.newfrontend.TypeUtils.getPrimitiveByRef;
import static pascal.taie.frontend.newfrontend.TypeUtils.getRightPrimitiveLiteral;
import static pascal.taie.frontend.newfrontend.TypeUtils.getSimpleJREMethod;
import static pascal.taie.frontend.newfrontend.TypeUtils.getType;
import static pascal.taie.frontend.newfrontend.TypeUtils.getWidenType;

public class NewMethodIRBuilder {
    private static final Logger logger = LogManager.getLogger(NewMethodIRBuilder.class);

    private final String sourceFilePath;
    private final String sourceFileName;
    private final String methodName;
    private final JMethod jMethod;

    // soot style method signature
    private final String methodSig;

    // binary name
    private final String className;

    // if notHandle, then build() return Optional.empty()
    private boolean notHandle;

    // Method to be built
    private MethodDeclaration targetMethod;

    // class where method is declared
    private final JClass targetClass;

    // use linenoManger to retrieve line number;
    private LinenoManger linenoManger;

    public NewMethodIRBuilder(String sourceFilePath, String sourceFileName, SootMethod method, JMethod jMethod) {
        this.sourceFilePath = sourceFilePath;
        this.sourceFileName = sourceFileName;
        this.methodName = method.getName();
        this.jMethod = jMethod;
        this.methodSig = method.getSubSignature();
        this.className = method.getDeclaringClass().getName();
        this.notHandle = checkIfNotHandle(method);

        if (!WorldParaHolder.isWorldReady()) {
            logger.error("NewFrontend can't get the World");
            this.targetClass = null;
            this.notHandle = true;
        } else {
            this.targetClass = WorldParaHolder.getClassHierarchy().getClass(className);
            if (targetClass == null) {
                logger.error("NewFrontend can't get tai-e class for" + className);
                this.notHandle = true;
            }
        }
    }

    // Some Reason for not handle
    public boolean checkIfNotHandle(SootMethod sootMethod) {
        var r1 = methodName.equals("<init>");
        return r1;
    }

    public Optional<IR> build() {
        if (notHandle) {
            return Optional.empty();
        }

        // load source file from disk, then parse it.
        var sourceCharArray = SourceReader.readJavaSourceFile(sourceFilePath);
        if (sourceCharArray.isEmpty()) {
            return Optional.empty();
        }
        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName(sourceFileName);
        var options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_SOURCE, "1.7");
        parser.setCompilerOptions(options);

        // TODO: set real classPaths and sourcePaths.
        String[] classPath = {};
        String[] sourcePath = {};
        String[] encodings = {};
        // the 4th argument may cause unexpected problem
        // when the running jre version is not equal to the requested jre version.
        parser.setEnvironment(classPath, sourcePath, encodings, true);

        parser.setSource(sourceCharArray.get());
        CompilationUnit cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());
        if (cu == null) {
            logger.error("parse failed");
            return Optional.empty();
        }
        this.linenoManger = new LinenoManger(cu);

        // find method to be generated.
        var preTargetMethod = new MethodLocator().getMethodBySig(className, methodSig, cu);
        if (preTargetMethod.isEmpty()) {
            logger.error("locate failed");
            return Optional.empty();
        }
        this.targetMethod = preTargetMethod.get();
        logger.debug("method located, name:" + targetMethod.getName());

        this.targetMethod = preTargetMethod.get();
        try {
            var generator = new IRGenerator();
            return Optional.of(generator.build());
        } catch (NewFrontendException e) {
            logger.error(e.getMessage(), e);
            return Optional.empty();
        }
    }

    class IRGenerator {

        private final static String THIS = "%this";

        private final static String STRING_CONSTANT = "%stringconst";

        private final static String CLASS_CONSTANT = "%classconst";

        private final static String NULL_CONSTANT = "%nullconst";

        private final List<Stmt> stmts;

        private final List<Var> params;

        private final Map<IBinding, Var> bindingVarMap;

        private final List<Var> vars;

        private final List<ExceptionEntry> exceptionList;

        private final Set<Var> retVar;

        private Var thisVar;

        private int tempCounter;

        private final StmtGenerateVisitor stmtVisitor;

        private final ExpVisitor expVisitor;

        private final VisitorContext context;

        public IRGenerator() {
            stmts = new ArrayList<>();
            params = new ArrayList<>();
            vars = new ArrayList<>();
            bindingVarMap = Maps.newMap();
            tempCounter = 0;
            context = new VisitorContext();
            stmtVisitor = new StmtGenerateVisitor(context);
            expVisitor = new ExpVisitor(context);
            exceptionList = new ArrayList<>();
            retVar = Sets.newSet();
        }

        public IR build() {
            buildThis();
            buildPara();
            buildStmt();
            logger.info(exceptionList);
            return new DefaultIR(jMethod, thisVar, params, retVar, vars, stmts, exceptionList);
        }

        private int getVarIndex() {
            return vars.size();
        }

        private Var newVar(String name, Type type) {
            var v = new Var(jMethod, name, type, getVarIndex());
            regVar(v);
            return v;
        }

        private void regVar(Var v) {
            vars.add(v);
        }

        private Var newTempVar(Type type) {
            int tempNow = tempCounter;
            tempCounter++;
            var v = new Var(jMethod, "%temp$" + tempNow, type, getVarIndex());
            regVar(v);
            return v;
        }

        Var newTempConstantVar(Literal literal) {
            String varName;
            if (literal instanceof pascal.taie.ir.exp.StringLiteral) {
                varName = STRING_CONSTANT + tempCounter++;
            } else if (literal instanceof ClassLiteral) {
                varName = CLASS_CONSTANT + tempCounter++;
            } else if (literal instanceof pascal.taie.ir.exp.NullLiteral) {
                // each method has at most one variable for null constant,
                // thus we don't need to count for null constant.
                varName = NULL_CONSTANT;
            } else {
                varName = "%" + literal.getType().getName() +
                        "const" + tempCounter++;
            }
            Var var = new Var(jMethod, varName, literal.getType(), getVarIndex(), literal);
            regVar(var);
            return var;
        }

        private Var getBinding(IBinding binding) {
            return bindingVarMap.computeIfAbsent(binding, (b) -> {
                var v = newVar(b.getName(), TypeUtils.JDTTypeToTaieType(((IVariableBinding) b).getType()));
                return v;
            });
        }

        private Type getReturnType() {
            return jMethod.getReturnType();
        }


        /**
         * @apiNote Important: {@code stmt} is illegal before use this function to convert
         * @return right stmt
         */
        private Stmt addStmt(int lineNo, Stmt stmt) {
            stmt.setLineNumber(lineNo);
            stmt.setIndex(stmts.size());
            stmts.add(stmt);
            return stmt;
        }

        private void addRetVar(Var v) {
            retVar.add(v);
        }

        private void buildThis() {
            this.thisVar = newVar(THIS, targetClass.getType());
        }

        private void buildPara() {
            // TODO: varargs
            // This implementation
            var paraTree = targetMethod.parameters();
            for (var i : paraTree) {
                SingleVariableDeclaration svd = (SingleVariableDeclaration) i;
                var name = svd.getName();
                var nameString = name.getIdentifier();
                var type = TypeUtils.JDTTypeToTaieType(svd.getType().resolveBinding());
                var aVar = newVar(nameString, type);
                params.add(aVar);
                bindingVarMap.put(name.resolveBinding(), aVar);
            }
        }

        private void buildStmt() {
            targetMethod.accept(stmtVisitor);
            stmtVisitor.postProcess();
        }

        private boolean checkEndCont() {
            var last = stmts.get(stmts.size() - 1);
            return last instanceof Return ||
                    last instanceof Goto;
        }

        private void visitStmt(Statement stmt) {
            stmt.accept(stmtVisitor);
        }

        private int nowPos() {
            return stmts.size();
        }

        private Stmt getStmt(int i ) { return stmts.get(i); }

        record ExceptionHandler(String target, Type type, String handler) { }

        class ExceptionEntryManager {
            private final Map<String, LinkedList<int[]>> codeRegionMap;
            private final List<ExceptionHandler> handlerList;
            private final List<String> pausedList;

            ExceptionEntryManager() {
                this.codeRegionMap = Maps.newMap();
                this.pausedList = new ArrayList<>();
                this.handlerList = new ArrayList<>();
            }

            public void beginRecording(String label) {
                var newFrame = new int[] { nowPos(), -1 };
                if (codeRegionMap.containsKey(label)) {
                    codeRegionMap.computeIfPresent(label, (k, v) -> {
                        v.addFirst(newFrame);
                        return v;
                    });
                } else {
                    codeRegionMap.computeIfAbsent(label, k -> {
                        LinkedList<int[]> newList = new LinkedList<>();
                        newList.addFirst(newFrame);
                        return newList;
                    });
                }
            }

            public void endRecording(String label) {
                assert (codeRegionMap.containsKey(label));
                codeRegionMap.computeIfPresent(label, (k, v) -> {
                    int[] now = v.getFirst();
                    now[1] = nowPos();
                    return v;
                });
            }

            public void pauseRecording(String label) {
                endRecording(label);
                pausedList.add(label);
            }

            public void continueRecordAll() {
                for (var i : pausedList) {
                    beginRecording(i);
                }
                pausedList.clear();
            }

            public void registerHandler(String label, Type exceptionType, String handlerLabel) {
                handlerList.add(new ExceptionHandler(label, exceptionType, handlerLabel));
            }

            public void resolveEntry(Map<String, Stmt> labelStmtMap) {
                for (var now : handlerList) {
                    assert (codeRegionMap.containsKey(now.target));
                    LinkedList<int[]> l = codeRegionMap.get(now.target);
                    var revTr = l.descendingIterator();
                    while (revTr.hasNext()) {
                        var i = revTr.next();
                        if (i[0] != i[1]) {
                            Catch c = (Catch) labelStmtMap.get(now.handler);
                            ClassType t = (ClassType) now.type;
                            ExceptionEntry entry = new ExceptionEntry(getStmt(i[0]), getStmt(i[1]), c, t);
                            exceptionList.add(entry);
                        }
                    }
                }
            }
        }

        class BlockLabelGenerator {

            private static final char FINALLY = '^';

            private static final char TRY = '@';

            private static final char CATCH = '!';

            private static final char NORMAL = '*';

            private int counter;

            public BlockLabelGenerator() {
                this.counter = 0;
            }

            public String getNewLabel() {
                return NORMAL + Integer.toString(counter++);
            }

            /**
             * Return the new label for a [finally] block
             * @return label, begin with '^'
             */
            public String getNewFinLabel() {
                return String.valueOf(FINALLY) + counter++;
            }

            public String getNewTryLabel() {
                return String.valueOf(TRY) + counter++;
            }

            public String getNewCatchLabel() { return String.valueOf(CATCH) + counter++; }

            public static boolean isFinallyBlock(String label) {
                return label.charAt(0) == FINALLY;
            }

            public static boolean isTryBlock(String label) {
                return label.charAt(0) == TRY;
            }

            public static boolean isCatchBlock(String label) { return label.charAt(0) == CATCH; }

            public static boolean isNormalBlock(String label) { return label.charAt(0) == NORMAL; }

            public static boolean isUserDefinedLabel(String label) {
                return ! isNormalBlock(label) && ! isCatchBlock(label) && ! isFinallyBlock(label)
                        && ! isTryBlock(label);
            }

        }


        class VisitorContext {
            private final Stack<Exp> expStack;
            private final Map<String, Stmt> blockMap;
            private final Map<Integer, String> patchMap;
            private final Map<String, Pair<String, String>> brkContMap;
            private final List<String> assocList;
            private final BlockLabelGenerator labelGenerator;
            private final ExceptionEntryManager exceptionManager;
            private final Map<String, Block> finallyBlockMap;
            private final Map<String, String> finallyLabelMap;
            private final Map<Integer, Var> intLiteralMap;
            private final Map<Integer, List<String>> switchMap;
            private final Map<Integer, String> switchDefaultMap;

            private final LinkedList<String> contextCtrlList;

            public VisitorContext() {
                this.blockMap = Maps.newMap();
                this.patchMap = Maps.newMap();
                this.brkContMap = Maps.newMap();
                expStack = new Stack<>();
                this.assocList = new ArrayList<>();
                this.labelGenerator = new BlockLabelGenerator();
                this.exceptionManager = new ExceptionEntryManager();
                this.contextCtrlList = new LinkedList<>();
                this.finallyBlockMap = Maps.newMap();
                this.finallyLabelMap = Maps.newMap();
                this.intLiteralMap = Maps.newMap();
                this.switchMap = Maps.newMap();
                this.switchDefaultMap = Maps.newMap();
            }

            public void pushStack(Exp exp) {
                expStack.add(exp);
            }

            public Exp popStack() {
                return expStack.pop();
            }

            public void assocLabel(String label) {
                this.assocList.add(label);
            }

            public void addPatchList(int stmtIdx, String label) {
                this.patchMap.put(stmtIdx, label);
            }

            public void addBlockMap(String label, Stmt stmt) {
                this.blockMap.put(label, stmt);
            }

            public void addSwitchMap(int stmtIdx, List<String> label) {
                this.switchMap.put(stmtIdx, label);
            }

            public void addSwitchDefaultMap(int stmtIdx, String label) {
                this.switchDefaultMap.put(stmtIdx, label);
            }

            private void buildUF(UnionFind uf) {
                blockMap.forEach((label, stmt) -> {
                    var nowIdx = stmt.getIndex();
                    var stmtReg = stmt;
                    while (stmtReg instanceof Goto g) {
                        var gotoIdx = patchMap.get(g.getIndex());
                        stmtReg = blockMap.get(gotoIdx);
                        uf.union(nowIdx, stmtReg.getIndex());
                        nowIdx = stmtReg.getIndex();
                    }
                });
            }

            private void setStmtTarget(Stmt stmt, Stmt target) {
                if (stmt instanceof Goto g) {
                    g.setTarget(target);
                } else if (stmt instanceof If i) {
                    i.setTarget(target);
                } else {
                    throw new NewFrontendException(stmt + " is not goto stmt, why use this function?");
                }
            }

            /**
             * there may by no return statement in a method
             * it will cause some label detaching from ir
             * e.g.
             * public void f(int x) {
             *     if (x > 0) {
             *         return;
             *     }
             *     +------------+ there is a [goto] pointed to here, but here contains nothing
             * }
             * this function will add the return
             */
            private void confirmNoDetach() {
                if (assocList.size() != 0) {
                    Stmt stmt = addStmt(-1, new Return());
                    for (var i : assocList) {
                        context.addBlockMap(i, stmt);
                    }
                    assocList.clear();
                }
            }

            /**
             * Use union-find set to resolve all goto statements
             */
            public void resolveGoto() {
                confirmNoDetach();
                var uf = new UnionFind(stmts.size());
                buildUF(uf);
                patchMap.forEach((i, label) -> {
                    var stmt = stmts.get(i);
                    var labelStmt = blockMap.get(label);
                    var tagStmt = stmts.get(uf.find(labelStmt.getIndex()));
                    setStmtTarget(stmt, tagStmt);
                });

                switchMap.forEach((i, labels) -> {
                    var stmt = (SwitchStmt) stmts.get(i);
                    List<Stmt> targets = labels
                            .stream()
                            .map(blockMap::get)
                            .toList();
                    stmt.setTargets(targets);
                });

                switchDefaultMap.forEach((i, label) -> {
                    var stmt = (SwitchStmt) stmts.get(i);
                    Stmt now = blockMap.get(label);
                    assert now != null;
                    stmt.setDefaultTarget(now);
                });
            }

            public @Nullable Stmt getStmtByLabel(String label) {
                return blockMap.get(label);
            }

            public String getNewLabel() {
                return labelGenerator.getNewLabel();
            }

            public String getNewFinLabel() { return labelGenerator.getNewFinLabel(); }

            public void assocBreakAndContinue(String nowLabel, String breakLabel, String continueLabel) {
                this.brkContMap.put(nowLabel, new Pair<>(breakLabel, continueLabel));
            }

            public String getBreakLabel(String label) {
                var p = this.brkContMap.get(label);
                if (p != null) {
                    return p.first();
                } else {
                    throw new NewFrontendException("label: "  + label + " don't exist in current context, illegal state");
                }
            }

            public String getContinueLabel(String label) {
                var p = this.brkContMap.get(label);
                if (p != null) {
                    return p.second();
                } else {
                    throw new NewFrontendException("label: "  + label + " don't exist in current context, illegal state");
                }
            }

            public @Nullable Block getFinallyBlock(String label) {
                return this.finallyBlockMap.get(label);
            }

            public @Nullable Block getFinallyBlockBy(String label) {
                return this.finallyBlockMap.get(this.finallyLabelMap.get(label));
            }

            public void assocFinallyBlock(String label, Block finBlock) {
                this.finallyBlockMap.put(label, finBlock);
            }

            public void assocFinallyLabel(String tryOrCatch, String fin) {
                this.finallyLabelMap.put(tryOrCatch, fin);
            }

            public LinkedList<String> getContextCtrlList() {
                return this.contextCtrlList;
            }

            public void pushCtrlList(String label) {
                this.getContextCtrlList().addFirst(label);
            }

            public void popCtrlList() {
                this.contextCtrlList.removeFirst();
            }

            public ExceptionEntryManager getExceptionManager() {
                return this.exceptionManager;
            }

            public BlockLabelGenerator getLabelGenerator() { return this.labelGenerator; }

            public void handleUserLabel(String breakLabel, String contLabel) {
                if (contextCtrlList.size() > 0) {
                    if (BlockLabelGenerator.isUserDefinedLabel(this.contextCtrlList.getFirst())) {
                        assert (assocList.contains(this.contextCtrlList.getFirst()));
                        assocBreakAndContinue(this.contextCtrlList.getFirst(), breakLabel, contLabel);
                    }
                }
            }

            public Var getInt(LinenoASTVisitor visitor, int i) {
                if (this.intLiteralMap.containsKey(i)) {
                    return intLiteralMap.get(i);
                } else {
                    Var v = newTempConstantVar(IntLiteral.get(i));
                    intLiteralMap.put(i, v);
                    visitor.newAssignment(v, IntLiteral.get(i));
                    return v;
                }
            }

        }

        class LinenoASTVisitor extends ASTVisitor {
            private int lineno;
            protected final VisitorContext context;

            protected int getLineno() {
                return lineno;
            }

            public LinenoASTVisitor() {
                this.context = new VisitorContext();
            }

            public LinenoASTVisitor(VisitorContext context) {
                this.context = context;
            }

            @Override
            public void preVisit(ASTNode node) {
                this.lineno = linenoManger.getLineno(node);
            }

            public void postProcess() {
                context.resolveGoto();
                context.getExceptionManager().resolveEntry(context.blockMap);
            }

            protected void visitExp(Expression expression) {
                // assert this two context is equal, or everything is illegal
                assert (this.context == IRGenerator.this.context);
                expression.accept(expVisitor);
            }

            protected void addStmt(Stmt stmt) {
                // first use IRGenerator to get right index and lineno
                var newStmt = IRGenerator.this.addStmt(lineno, stmt);
                if (context.assocList.size() != 0) {
                    for (var i : context.assocList) {
                        context.addBlockMap(i, newStmt);
                    }
                }
                context.assocList.clear();
            }

            protected void addGoto(String label) {
                var go = new Goto();
                var top = stmts.size();
                addStmt(go);
                context.addPatchList(top, label);
            }

            /**
             * <p>If stmts[end] will cover this goto
             * (i.e. the last statement of stmts[end] is Return)
             * we don't addGoto.</p>
             * <p>However, this function should not be always called.
             * Because there may exist another path to this goto statement.</p>
             */
            protected void addCheckedGoto(String label) {
                if (! checkEndCont()) {
                    addGoto(label);
                }
            }

            protected void addIf(ConditionExp exp, String label) {
                var go = new If(exp);
                var top = stmts.size();
                addStmt(go);
                context.addPatchList(top, label);
            }

            protected void addTableSwitch(Var v, int lowIdx, int highIdx, List<String> label, String defaultLabel) {
                var top = stmts.size();
                addStmt(new TableSwitch(v, lowIdx, highIdx));
                context.addSwitchMap(top, label);
                context.addSwitchDefaultMap(top, defaultLabel);
            }

            protected void addLookupSwitch(Var v, List<Integer> values, List<String> label, String defaultLabel) {
                var top = stmts.size();
                addStmt(new LookupSwitch(v, values));
                context.addSwitchMap(top, label);
                context.addSwitchDefaultMap(top, defaultLabel);
            }

            /**
             * some JDT method will always return a raw Type List
             * @param exp   an Expression List returned by some JDT method
             */
            protected Exp listCompute(List<Expression> exp, List<Type> types, Function<List<Var>, Exp> f) {
                List<Var> list = new ArrayList<>();
                for (var i = 0; i < exp.size(); ++i) {
                    visitExp(exp.get(i));
                    list.add(popVar(types.get(i)));
                }
                return f.apply(list);
            }

            protected Var genNewObject1(MethodRef init, List<Var> args, Type declClassType) {
                Var temp = newTempVar(declClassType);
                addStmt(new New(jMethod, temp, new NewInstance((ClassType) declClassType)));
                context.pushStack(new InvokeSpecial(init, temp, args));
                popSideEffect();
                return temp;
            }

            protected Exp genNewCast(Exp exp, Type t) {
                Var v = expToVar(exp);
                return new CastExp(v, t);
            }

            protected Var genNewObject(MethodRef init, List<Expression> args, Type declClassType) {
                return (Var) listCompute(args, init.getParameterTypes(), l -> genNewObject1(init, l, declClassType));
            }

            private Exp unboxingConversion(Exp exp, ReferenceType expType, PrimitiveType targetType) {
                Var v = expToVar(exp, expType);
                return new InvokeVirtual(
                        getSimpleJREMethod(expType.getName(), targetType + "Value"),
                        v,
                        new ArrayList<>());
            }

            private Exp boxingConversion(Exp exp, PrimitiveType expType, ReferenceType targetType) {
                Var v = expToVar(exp, expType);
                return new InvokeStatic(
                        getJREMethod(targetType.getName(), "valueOf",
                                List.of(getPrimitiveByRef(targetType)), targetType),
                        List.of(v));
            }

            private Exp widening(Exp exp, PrimitiveType expType, PrimitiveType targetType) {
                // JDT Should check correctness for us
                assert expType != targetType;
                if (getWidenType(expType).equals(targetType)) {
                    return exp;
                } else {
                    return genNewCast(exp, targetType);
                }
            }

            protected Exp preformTypeConversion(Exp exp, Type type) {
                Type expType = exp.getType();
                // 1. if [type] and [expType] both reference type,
                //    then there's no need for type conversion
                if (expType instanceof ReferenceType &&
                        type instanceof ReferenceType) {
                    return exp;
                }
                // 2. if [type] is reference, and [expType] is primitive,
                //    then try to perform boxing conversion
                else if (expType instanceof PrimitiveType p
                        && type instanceof ReferenceType r) {
                    return boxingConversion(exp, p, r);
                }
                else {
                    // 3. if [type] is primitive, and [expType] is reference,
                    //    then try to perform unboxing conversion
                    if (expType instanceof ReferenceType r &&
                            type instanceof PrimitiveType p) {
                        exp = unboxingConversion(exp, r, p);
                        expType = exp.getType();
                    }
                    // 4. if [type] is primitive, and [expType] is primitive,
                    //    then try to perform widening primitive conversion
                    if (expType instanceof PrimitiveType e &&
                            type instanceof PrimitiveType t) {
                        return widening(exp, e, t);
                    }
                    throw new NewFrontendException("illegal state, expType is " + expType + ", type is " + type);
                }
            }

            /**
             * See JLS17 ch 5.6, pp. 141 for detail
             * @param contextChoose  <p>0  for  numeric arithmetic context</p>
             *                 <p>1  for  numeric choice context</p>
             *                 <p>2  for  numeric array context</p>
             * @param operands operands for Numeric Promotion
             * @return type that operands will promote to
             */
            protected Type resolveNumericPromotion(int contextChoose, List<Expression> operands) {
                var typeList = operands.stream()
                        .map(k -> getIndexOfPrimitive(JDTTypeToTaieType(k.resolveTypeBinding())))
                        .max(Integer::compareTo);
                assert typeList.isPresent();
                int maxType = typeList.get();
                // see JLS 5.6 for detail
                if (maxType >= getIndexOfPrimitive(PrimitiveType.LONG)) {
                    return getPrimitiveByIndex(maxType);
                } else if (contextChoose == 0 || contextChoose == 2) {
                    return PrimitiveType.INT;
                } else {
                    return getPrimitiveByIndex(maxType);
                }
            }

            protected void newAssignment(LValue left, Exp right) {
                if (left instanceof Var v) {
                    if (right instanceof BinaryExp exp) {
                        addStmt(new Binary(v, exp));
                    } else if (right instanceof Literal l) {
                        addStmt(new AssignLiteral(v, l));
                    } else if (right instanceof Var v2) {
                        addStmt(new Copy(v, v2));
                    } else if (right instanceof InvokeExp exp) {
                        addStmt(new Invoke(jMethod, exp, v));
                    } else if (right instanceof FieldAccess exp) {
                        addStmt(new LoadField(v, exp));
                    } else if (right instanceof ArrayAccess exp) {
                        addStmt(new LoadArray(v, exp));
                    } else if (right instanceof NewArray exp) {
                        addStmt(new New(jMethod, v, exp));
                    } else if (right instanceof NewMultiArray exp) {
                        addStmt(new New(jMethod, v, exp));
                    } else if (right instanceof CastExp exp) {
                        addStmt(new Cast(v, exp));
                    }
                    else {
                        throw new NewFrontendException(right + " is not implemented");
                    }
                } else if (left instanceof FieldAccess s) {
                    addStmt(new StoreField(s, expToVar(right, left.getType())));
                } else if (left instanceof ArrayAccess a) {
                    addStmt(new StoreArray(a, expToVar(right, left.getType())));
                }
                else {
                    throw new NewFrontendException(left + " is not implemented");
                }
            }

            protected void handleAssignment(Expression lExp, Expression rExp) {
                visitExp(rExp);
                Exp r = context.popStack();
                LValue v;
                visitExp(lExp);
                v = popLValue();
                r = preformTypeConversion(r, v.getType());
                newAssignment(v, r);
                context.pushStack(v);
            }

            protected Var expToVar(Exp exp, Type varType) {
                if (! varType.equals(exp.getType())) {
                    exp = preformTypeConversion(exp, varType);
                }
                return expToVar(exp);
            }

            protected Var expToVar(Exp exp) {
                if (exp instanceof Var v) {
                    return v;
                } else if (exp instanceof ConditionExp exp1) {
                    return collectBoolEval(exp1);
                } else if (exp instanceof BinaryExp
                        || exp instanceof InvokeExp
                        || exp instanceof FieldAccess
                        || exp instanceof ArrayAccess
                        || exp instanceof NewExp
                        || exp instanceof CastExp) {
                    var v = newTempVar(exp.getType());
                    newAssignment(v, exp);
                    return v;
                } else if (exp instanceof Literal l) {
                    // TODO: hook all literal
                    if (exp instanceof IntLiteral i) {
                        return context.getInt(this, i.getValue());
                    }
                    var v = newTempConstantVar(l);
                    newAssignment(v, exp);
                    return v;
                } else {
                    throw new NewFrontendException(exp + "  is not implemented");
                }
            }

            protected void expToSideEffect(Exp exp) {
                if (exp instanceof InvokeExp exp1) {
                    addStmt(new Invoke(jMethod, exp1));
                }
            }

            protected void genIfHeader(ConditionExp e, String trueLabel, String falseLabel, boolean next) {
                addIf(e, trueLabel);
                if (next) {
                    addGoto(falseLabel);
                }
            }

            private Var collectBoolEval(BiConsumer<String, String> condMaker) {
                Var v = newTempVar(PrimitiveType.BOOLEAN);
                String trueLabel = context.getNewLabel();
                String falseLabel = context.getNewLabel();
                String endLabel = context.getNewLabel();
                condMaker.accept(trueLabel, falseLabel);
                context.assocLabel(falseLabel);
                newAssignment(v, IntLiteral.get(0));
                addGoto(endLabel);
                context.assocLabel(trueLabel);
                newAssignment(v, IntLiteral.get(1));
                context.assocLabel(endLabel);
                return v;
            }

            private Var collectBoolEval(ConditionExp exp) {
                return collectBoolEval((t, f) -> genIfHeader(exp, t, f, false));
            }

            protected Var collectBoolEval(Expression exp) {
                return collectBoolEval((t, f) -> transformCond(exp, t, f, false));
            }

            protected Var genCondExpression(ConditionalExpression ce) {
                Expression cond = ce.getExpression();
                Expression thenExp = ce.getThenExpression();
                Expression elseExp = ce.getElseExpression();
                Var v = newTempVar(PrimitiveType.BOOLEAN);
                String trueLabel = context.getNewLabel();
                String falseLabel = context.getNewLabel();
                String endLabel = context.getNewLabel();
                Type t = JDTTypeToTaieType(ce.resolveTypeBinding());
                transformCond(cond, trueLabel, falseLabel, false);
                context.assocLabel(falseLabel);
                visitExp(elseExp);
                newAssignment(v, popExp(t));
                addGoto(endLabel);
                context.assocLabel(trueLabel);
                visitExp(thenExp);
                newAssignment(v, popExp(t));
                context.assocLabel(endLabel);
                return v;
            }

            protected void expToControlFlow(Exp exp, String trueLabel, String falseLabel, boolean next) {
                ConditionExp cond;
                if (exp instanceof ConditionExp e) {
                    cond = e;
                } else {
                    Var v = expToVar(exp, PrimitiveType.BOOLEAN);
                    cond = new ConditionExp(ConditionExp.Op.EQ, v, context.getInt(this, 0));
                }
                genIfHeader(cond, trueLabel, falseLabel, next);
            }

            protected Exp popExp(Type t) {
                return preformTypeConversion(context.popStack(), t);
            }

            protected void popControlFlow(String trueLabel, String falseLabel, boolean next) {
                expToControlFlow(context.popStack(), trueLabel, falseLabel, next);
            }

            protected void popSideEffect() {
                Exp e = context.popStack();
                expToSideEffect(e);
            }

            protected LValue popLValue () {
                Exp e = context.popStack();
                if (e instanceof Var v) {
                    return v;
                } else if (e instanceof FieldAccess f) {
                    return f;
                } else if (e instanceof ArrayAccess a) {
                    return a;
                } else {
                    throw new NewFrontendException(e + " can't be LValue, some error occur before");
                }
            }

            protected Var popVar(Type type) {
                return expToVar(context.popStack(), type);
            }

            protected Var popVar() {
                return expToVar(context.popStack());
            }

            protected Var[] popVar2(Type t1, Type t2) {
                var v2 = context.popStack();
                var v1 = context.popStack();
                return new Var[] {expToVar(v1, t1), expToVar(v2, t2)};
            }

            protected void handleSingleVarDecl(VariableDeclaration vd) {
                var name = vd.getName();
                var val = vd.getInitializer();
                if (val == null) {
                    visitExp(name);
                } else {
                    handleAssignment(name, val);
                }
                popSideEffect();
            }

            protected List<Expression> getAllOperands(InfixExpression exp) {
                var left = exp.getLeftOperand();
                var right = exp.getRightOperand();
                var extend = exp.extendedOperands();
                List<Expression> l = new ArrayList<>();
                l.add(left);
                l.add(right);
                for (var i : extend) {
                    l.add((Expression) i);
                }
                return l;
            }


            protected void transformCond(Expression exp,
                                       String trueLabel,
                                       String falseLabel,
                                       boolean next) {
                if (exp instanceof InfixExpression infix) {
                    if (infix.getOperator().equals(InfixExpression.Operator.CONDITIONAL_OR)) {
                        var operands = getAllOperands(infix);
                        for (var i = 0; i < operands.size() - 1; ++i) {
                            var falseNow = context.getNewLabel();
                            transformCond(operands.get(i), trueLabel, falseNow, false);
                            context.assocLabel(falseNow);
                        }
                        transformCond(operands.get(operands.size() - 1), trueLabel, falseLabel, next);
                        return;
                    } else if (infix.getOperator() == InfixExpression.Operator.CONDITIONAL_AND) {
                        var operands = getAllOperands(infix);
                        for (var i = 0; i < operands.size() - 1; ++i) {
                            var trueNow = context.getNewLabel();
                            transformCond(operands.get(i), trueNow, falseLabel, true);
                            context.assocLabel(trueNow);
                        }
                        transformCond(operands.get(operands.size() - 1), trueLabel, falseLabel, next);
                        return;
                    }
                } else if (exp instanceof PrefixExpression prefix) {
                    if (prefix.getOperator().equals(PrefixExpression.Operator.NOT)) {
                        transformCond(prefix.getOperand(), falseLabel, trueLabel, !next);
                        return;
                    }
                } else if (exp instanceof ParenthesizedExpression p) {
                    transformCond(p.getExpression(), trueLabel, falseLabel, next);
                    return;
                }
                visitExp(exp);
                popControlFlow(trueLabel, falseLabel, next);
            }

            protected InvokeExp genSimpleInvoke(Var v,
                                                String methodName,
                                                List<Var> args,
                                                List<Type> paraType,
                                                Type retType) {
                ClassType t = (ClassType) v.getType();
                MethodRef r = MethodRef.get(t.getJClass(), methodName, paraType, retType, false);
                return new InvokeVirtual(r, v, args);
            }

            protected InvokeExp genEquals(Var v1, Var v2) {
                Type t = v1.getType();
                Type objType = getType(ClassNames.OBJECT);
                return genSimpleInvoke(v1, "equals",
                        List.of(v2), List.of(objType), PrimitiveType.BOOLEAN);
            }

            protected void genStmts(List<Statement> stmts1) {
                for (var i : stmts1) {
                    visitStmt(i);
                }
            }
        }

        class StmtGenerateVisitor extends LinenoASTVisitor {

            public StmtGenerateVisitor(VisitorContext context) {
                super(context);
            }

            public boolean visit(EmptyStatement es) {
                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(Block block) {
                var stmts = block.statements();
                genStmts(stmts);
                return false;
            }

            @Override
            public boolean visit(ReturnStatement rs) {
                var exp = rs.getExpression();
                Var retVar;
                if (exp != null) {
                    visitExp(exp);
                    retVar = popVar(getReturnType());
                    addStmt(new Return(retVar));
                    addRetVar(retVar);
                } else {
                    addStmt(new Return());
                }
                return false;
            }

            /**
             * If encounter here, then {@code ass} is a {@code Expression Statement}
             * e.g. x = 10;
             */
            @Override
            public boolean visit(Assignment ass) {
                // let ExpVisitor handle this assignment
                visitExp(ass);
                // ignore the value of this assignment
                popSideEffect();
                return false;
            }

            @Override
            public boolean visit(SingleVariableDeclaration svd) {
                handleSingleVarDecl(svd);
                return false;
            }

            @Override
            public boolean visit(VariableDeclarationStatement vds) {
                var singleDecls = vds.fragments();
                for (var i : singleDecls) {
                    var singleDecl = (VariableDeclarationFragment) i;
                    handleSingleVarDecl(singleDecl);
                }
                return false;
            }

            // Generate code like :
            //            +--------------+
            //            |    Branch    |------+
            //            +--------------+      |
            //      +-----|  goto False  |      | *No need to check this goto*
            //      |     +--------------+<-----+
            //      |     |    True      |
            //      |     +--------------+
            //      |     |  goto Next   |------+ *This goto can be checked*
            //      +---->+--------------+      |
            //            |    False     |      |
            //            +--------------+<-----+  (Break target)
            //            |     Next     |
            //            +--------------+
            @Override
            public boolean visit(IfStatement ifStmt) {
                var exp = ifStmt.getExpression();
                var thenStmt = ifStmt.getThenStatement();
                var elseStmt = ifStmt.getElseStatement();
                var trueLabel = context.getNewLabel();
                var falseLabel = context.getNewLabel();
                var endLabel = context.getNewLabel();
                transformCond(exp, trueLabel, falseLabel, true);
                context.assocLabel(trueLabel);
                thenStmt.accept(this);
                if (elseStmt != null) {
                    addCheckedGoto(endLabel);
                }
                // here, if [else] don't exist, falseLabel will assoc to end
                // true statement will just fall through
                context.assocLabel(falseLabel);
                if (elseStmt != null) {
                    elseStmt.accept(this);
                }
                // assoc label here to confirm breakLabel is legal
                context.assocLabel(endLabel);
                return false;
            }

            // Generate code like :
            //            +--------------+
            //            |    Inits     |
            //            +--------------+<---------+ (Continue target)
            //            |    Branch    |------+   |
            //            +--------------+      |   |
            //      +-----|  goto Break  |      |   |
            //      |     +--------------+<-----+   |
            //      |     |    Body      |          |
            //      |     +--------------+          |
            //      |     |   Updates    |          |
            //      |     +--------------+          |
            //      |     |  goto Branch |----------+ *This goto can be checked*
            //      +---->+--------------+            (Break target)
            //            |    Break     |
            //            +--------------+
            //
            private void genNormalLoop(List<Expression> inits,
                                       Expression cond,
                                       Statement body,
                                       List<Expression> updates) {
                var contLabel = context.getNewLabel();
                var breakLabel = context.getNewLabel();
                var bodyLabel = context.getNewLabel();
                context.handleUserLabel(breakLabel, contLabel);
                for (var i : inits) {
                    visitExp(i);
                    popSideEffect();
                }
                context.assocLabel(contLabel);
                context.getContextCtrlList().addFirst(contLabel);
                context.assocBreakAndContinue(contLabel, breakLabel, contLabel);
                transformCond(cond, bodyLabel, breakLabel, true);
                context.assocLabel(bodyLabel);
                body.accept(this);
                context.getContextCtrlList().removeFirst();
                for (var i : updates) {
                    visitExp(i);
                }
                addCheckedGoto(contLabel);
                context.assocLabel(breakLabel);
            }

            @Override
            public boolean visit(WhileStatement ws) {
                genNormalLoop(new ArrayList<>(), ws.getExpression(),  ws.getBody(), new ArrayList<>());
                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(ForStatement fs) {
                var init = fs.initializers();
                var body = fs.getBody();
                var exp = fs.getExpression();
                var updates = fs.updaters();
                // type soundness is promised by JDT (or JDT will gen parse error)
                // ignore the warning
                genNormalLoop(init, exp, body, updates);
                return false;
            }

            // Generate code like :
            //     +----->+--------------+ (Continue target)
            //     |      |    Body      |
            //     |      +--------------+
            //     +------|    Branch    |
            //            +--------------+ (Break target)
            //            |    Break     |
            //            +--------------+
            //
            @Override
            public boolean visit(DoStatement ds) {
                var exp = ds.getExpression();
                var stmt = ds.getBody();
                var contLabel = context.getNewLabel();
                var breakLabel = context.getNewLabel();
                context.handleUserLabel(breakLabel, contLabel);
                context.assocLabel(contLabel);
                context.assocBreakAndContinue(contLabel, breakLabel, contLabel);
                context.getContextCtrlList().addFirst(contLabel);
                stmt.accept(this);
                context.getContextCtrlList().removeFirst();
                transformCond(exp, contLabel, breakLabel, false);
                context.assocLabel(breakLabel);
                return false;
            }

            /**
             * Generate break/continue statement.
             * Because of the existence of try-catch-finally,
             * this need to be carefully handled
             * @param label may be null, break/continue label
             * @param brkOrCont 0 for break, 1 for continue
             */
            private void genContBrk(@Nullable SimpleName label, int brkOrCont) {
                for (var i : context.getContextCtrlList()) {
                    if (BlockLabelGenerator.isTryBlock(i) ||
                        BlockLabelGenerator.isCatchBlock(i)) {
                        // First, pause the record of this [Try]/[Catch] Block
                        // Because the code to be generated is not belong to this [Try]/[Catch] Block
                        // It's belong to the [Finally] of the [Try]/[Catch] Block
                        context.getExceptionManager().pauseRecording(i);
                        // Generate this [Finally] Block
                        Block fin = context.getFinallyBlockBy(i);
                        if (fin != null) {
                            fin.accept(this);
                        }
                    } else {
                        if (label == null) {
                            // if [label] is null, then we have got the innermost loop block, generate goto here
                            if (brkOrCont == 0) {
                                addGoto(context.getBreakLabel(i));
                            } else {
                                addGoto(context.getContinueLabel(i));
                            }
                            // Continue recording for all outer [Try] block
                            // Because there might be other path in this [Try] block
                            context.getExceptionManager().continueRecordAll();
                            return;
                        } else if (i.equals(label.getIdentifier())) {
                            // Or we have got the loop block indicate by [label]
                            if (brkOrCont == 0) {
                                addGoto(context.getBreakLabel(i));
                            } else {
                                addGoto(context.getContinueLabel(i));
                            }
                            context.getExceptionManager().continueRecordAll();
                            return;
                        }
                        // else, just continue to find the right label
                    }
                }
                throw new NewFrontendException("can't find the label for " + label + ", does this label have break/continue target?");
            }

            @Override
            public boolean visit(BreakStatement bs) {
                var label = bs.getLabel();
                genContBrk(label, 0);
                return false;
            }

            @Override
            public boolean visit(ContinueStatement cs) {
                var label = cs.getLabel();
                genContBrk(label, 1);
                return false;
            }

            @Override
            public boolean visit(LabeledStatement stmt) {
                var label = stmt.getLabel();
                var inner = stmt.getBody();
                context.assocLabel(label.getIdentifier());
                context.getContextCtrlList().addFirst(label.getIdentifier());
                // TODO: add break/Continue for all Statement
                inner.accept(this);
                return false;
            }


            private String handleCatch(CatchClause cc, String tryLabel, String finLabel1, String finLabel2) {
                // Here, we still need to recording
                String nowLabel = context.labelGenerator.getNewCatchLabel();
                // Note: it's safe to assoc a label here
                // because a catch block at least generate one statement: catch ...
                context.assocLabel(nowLabel);
                context.getExceptionManager().beginRecording(nowLabel);
                context.assocFinallyLabel(nowLabel, finLabel2);
                SingleVariableDeclaration decl = cc.getException();
                Block body = cc.getBody();
                ITypeBinding exceptionType = decl.getType().resolveBinding();
                Type taieType = TypeUtils.JDTTypeToTaieType(exceptionType);
                visitExp(decl.getName());
                addStmt(new Catch(popVar()));
                // add exception handler
                // ([Try Begin] [Try End])ₙ [Catch Begin] [Type]
                context.getExceptionManager().registerHandler(tryLabel, taieType, nowLabel);
                context.getContextCtrlList().addFirst(nowLabel);
                body.accept(this);
                context.getExceptionManager().endRecording(nowLabel);
                context.getContextCtrlList().removeFirst();
                addGoto(finLabel2);
                return nowLabel;
            }

            // Normally, generate code like:
            // +---------------+
            // |   Try Block   |         Finally 2 block handle "normal" control flow
            // +---------------+
            // | Goto Finally2 |-----+         Finally 1 block begin with [Catch e]
            // +---------------+     |                         end   with [Throw e]
            // |    Catch 1    |     |         which act as default exception handler
            // +---------------+     |
            // | Goto Finally2 |-----+
            // +---------------+     |
            // |  .........    |-----+
            // +---------------+     |
            // |    Catch n    |     |
            // +---------------+     |
            // | Goto Finally2 |-----+
            // +---------------+     |
            // | Finally 1     |     |
            // +---------------+<----+
            // | Finally 2     |
            // +---------------+
            @Override
            public boolean visit(TryStatement ts) {
                Block tryBlock = ts.getBody();
                List catches = ts.catchClauses();
                Block finBlock = ts.getFinally();
                String tryLabel = context.labelGenerator.getNewTryLabel();
                String finLabel1 = context.getNewFinLabel();
                String finLabel2 = context.getNewFinLabel();
                context.getContextCtrlList().addFirst(tryLabel);
                context.assocFinallyLabel(tryLabel, finLabel2);

                if (finBlock != null) {
                    context.assocFinallyBlock(finLabel2, finBlock);
                }

                // first handle [Try] block
                context.getExceptionManager().beginRecording(tryLabel);
                context.assocLabel(tryLabel);
                tryBlock.accept(this);
                context.getExceptionManager().endRecording(tryLabel);
                addGoto(finLabel2);

                // now, we are not in [Try] block
                context.getContextCtrlList().removeFirst();
                List<String> catchLabel = new ArrayList<>();
                for (var i : catches) {
                    var l = handleCatch((CatchClause) i, tryLabel, finLabel1, finLabel2);
                    catchLabel.add(l);
                }

                if (finBlock != null) {
                    // add exception handler
                    // ([Try Begin] [Try End])ₙ [Finally-Catch] [Any]
                    context.getExceptionManager().registerHandler(tryLabel, TypeUtils.anyException(), finLabel1);
                    // add exception handler
                    // ([Catch Begin] [Catch End])ₙ [Finally-Catch] [Any]
                    for (var i : catchLabel) {
                        context.getExceptionManager().registerHandler(i, TypeUtils.anyException(), finLabel1);
                    }

                    // generate finLabel1 block
                    context.assocLabel(finLabel1);
                    Var e = newTempVar(TypeUtils.anyException());
                    addStmt(new Catch(e));
                    finBlock.accept(this);
                    addStmt(new Throw(e));

                    // generate finLabel2 block
                    context.assocLabel(finLabel2);
                    finBlock.accept(this);
                } else {
                    // if [finally] don't exist, we still assoc next statement with [finLabel2]
                    context.assocLabel(finLabel2);
                }
                return false;
            }

            @Override
            public boolean visit(ThrowStatement thr) {
                Expression e = thr.getExpression();
                visitExp(e);
                addStmt(new Throw(popVar()));
                return false;
            }

            private void genSwitchCtrl(boolean isPrimitive,
                                       Var switchVar,
                                       List<Expression> exps,
                                       List<String> targets,
                                       @Nullable String defaultLabel) {
                if (isPrimitive) {
                    // exps will be unique, if it's continuous, we use [tableswitch]
                    // otherwise, use [lookupswitch]
                    // if it's primitive type, we convert all [exps] to [int]
                    List<Integer> rightExps = exps
                            .stream()
                            .map(i -> Integer.valueOf(i.resolveConstantExpressionValue().toString()))
                            .toList();
                    // to check if it's a continuous list, first sort it.
                    List<Pair<Integer, String>> l = new ArrayList<>();
                    for (int i = 0; i < exps.size(); ++i) {
                        l.add(new Pair<>(rightExps.get(i), targets.get(i)));
                    }
                    l.sort(Comparator.comparing(Pair::first));
                    int min = l.get(0).first();
                    int max = l.get(l.size() - 1).first();
                    // l is continuous iff (max - min == l.size() - 1)
                    if (max - min == l.size() - 1) {
                        var nowTargets = l.stream().map(Pair::second).toList();
                        addTableSwitch(switchVar, min, max, nowTargets, defaultLabel);
                    } else {
                        addLookupSwitch(switchVar, rightExps, targets, defaultLabel);
                    }

                } else {
                    // transform to if
                    for (int i = 0; i < exps.size(); ++i) {
                        visitExp(exps.get(i));
                        Var v = popVar();
                        context.pushStack(genEquals(switchVar, v));
                        popControlFlow(targets.get(i), null, false);
                    }
                }
            }

            // switch (var) { case ... }
            // if [var] is Enum or String, we transform it to [if]
            // else, use tai-e (lookupswitch/tableswitch)
            // Generate code like:
            //        +----------------+
            //        |  Switch Header |------+
            //        +----------------+<-----+
            //        |  Defalut       |      |
            //        +----------------+      |
            //   +----|  Goto End      |      |
            //   |    +----------------+<-----+
            //   |    |  Case 1        |      |
            //   |    +----------------+      |
            //   +----|  Goto End      |      |
            //   |    +----------------+<-----+
            //   |    |   ...          |
            //   +--->+----------------+
            //        |  End           |
            //        +----------------+
            @Override
            public boolean visit(SwitchStatement ss) {
                visitExp(ss.getExpression());
                Var v = popVar();
                boolean isPrimitive = v.getType() instanceof PrimitiveType;
                String now = context.getNewLabel();
                String end = context.getNewLabel();
                context.handleUserLabel(end, null);
                context.getContextCtrlList().addFirst(now);
                context.assocBreakAndContinue(now, end, null);
                Map<String, List<Statement>> blocks = Maps.newMap();
                List<String> targets = new ArrayList<>();
                List<Expression> caseExps = new ArrayList<>();
                String defaultLabel = null;

                // JDT handle switch like [ SwitchCase1, SwitchCase2, Statement1, Statement2, SwitchCase3, ... ]
                // we first separate these Statements
                var i = ss.statements().iterator();
                Statement s = (Statement) i.next();
                while (i.hasNext()) {
                    String nowLabel = context.getNewLabel();
                    while (s instanceof SwitchCase sc)
                    {
                        if (sc.isDefault()) {
                            defaultLabel = nowLabel;
                        } else {
                            for (var exp : sc.expressions()) {
                                caseExps.add((Expression) exp);
                                targets.add(nowLabel);
                            }
                        }
                        if (! i.hasNext()) {
                            break;
                        }
                        s = (Statement) i.next();
                    }
                    List<Statement> nowStmt = new ArrayList<>();
                    while (! (s instanceof SwitchCase))
                    {
                        nowStmt.add(s);
                        if (! i.hasNext()) {
                            break;
                        }
                        s = (Statement) i.next();
                    }
                    blocks.put(nowLabel, nowStmt);
                }

                if (defaultLabel == null) {
                    defaultLabel = context.getNewLabel();
                    blocks.put(defaultLabel, List.of());
                }
                genSwitchCtrl(isPrimitive, v, caseExps, targets, defaultLabel);

                List<Statement> defaultStmts = blocks.get(defaultLabel);
                blocks.remove(defaultLabel);
                context.assocLabel(defaultLabel);
                genStmts(defaultStmts);
                addCheckedGoto(end);

                blocks.forEach((label, stmts) -> {
                    context.assocLabel(label);
                    genStmts(stmts);
                    addGoto(end);
                });

                context.getContextCtrlList().removeFirst();
                context.assocLabel(end);
                return false;
            }

            @Override
            public boolean visit(ExpressionStatement es) {
                Expression exp = es.getExpression();
                visitExp(exp);
                popSideEffect();
                return false;
            }
        }

        class ExpVisitor extends LinenoASTVisitor {

            ExpVisitor(VisitorContext context) {
                super(context);
            }

            private void binaryCompute(InfixExpression exp, BiFunction<Var, Var, Exp> f) {
                Type t = resolveNumericPromotion(0, getAllOperands(exp));
                exp.getLeftOperand().accept(this);
                var lVar = popVar(t);
                exp.getRightOperand().accept(this);
                var rVar = popVar(t);
                context.pushStack(f.apply(lVar, rVar));
                var extOperands = exp.extendedOperands();
                for (var i : extOperands) {
                    var expNow = (Expression) i;
                    expNow.accept(this);
                    var lrVarNow = popVar2(t, t);
                    context.pushStack(f.apply(lrVarNow[0], lrVarNow[1]));
                }
            }


            private FieldAccess handleField(IVariableBinding binding, Var object) {
                ITypeBinding declClass = binding.getDeclaringClass();
                JClass jClass = TypeUtils.getTaieClass(declClass);
                ITypeBinding fieldType = binding.getType();
                Type taieType = TypeUtils.JDTTypeToTaieType(fieldType);
                boolean isStatic = Modifier.isStatic(binding.getModifiers());
                FieldRef ref = FieldRef.get(jClass, binding.getName(), taieType, isStatic);

                if (isStatic) {
                    return new StaticFieldAccess(ref);
                } else {
                    return new InstanceFieldAccess(ref, object);
                }
            }

            private Var getThis(ITypeBinding classBinding) {
                Type classType = TypeUtils.JDTTypeToTaieType(classBinding);
                if (thisVar.getType().equals(classType)) {
                    return thisVar;
                } else {
                    // TODO: check if it's correct for inner class
                    return newVar(THIS, classType);
                }
            }

            private void genStringBuilderAppend(Var sb, List<Expression> exp) {
                // don't use [listCompute]
                for (var i : exp) {
                    i.accept(this);
                    Var v = popVar();
                    List<Var> args = new ArrayList<>();
                    args.add(v);
                    MethodRef append = TypeUtils.getStringBuilderAppend(v.getType());
                    context.pushStack(new InvokeVirtual(append, sb, args));
                    popSideEffect();
                }
            }

            /**
             * @param exp an InfixExpression, where {@code Op} is {@code Plus}.
             * @apiNote the value {@code exp} can't be const.
             * i.e. {@code "12321" + 1} is not handled by this function.
             */
            private void genStringBuild(InfixExpression exp) {
                assert exp.getOperator() == InfixExpression.Operator.PLUS;
                ClassType sb = TypeUtils.getStringBuilder();
                MethodRef newSb = TypeUtils.getNewStringBuilder();
                Var sbVar = genNewObject(newSb, List.of(), sb);
                genStringBuilderAppend(sbVar, getAllOperands(exp));
                context.pushStack(new InvokeVirtual(TypeUtils.getToString(), sbVar, new ArrayList<>()));
            }

            // Ref: JLS17 6.5.6.1
            @Override
            public boolean visit(SimpleName name) {
                var binding = name.resolveBinding();
                if (binding instanceof IVariableBinding binding1) {
                    if (binding1.isField()) {
                        context.pushStack(handleField(binding1,
                                getThis(binding1.getDeclaringClass())));
                    } else {
                        context.pushStack(getBinding(binding));
                    }
                    return false;
                } else {
                    throw new NewFrontendException("Exp [ " + name + " ] can't be handled");
                }
            }

            // Ref: JLS17 6.5.6.2
            @Override
            public boolean visit(QualifiedName name) {
                Name qualifier = name.getQualifier();
                SimpleName name1 = name.getName();
                IBinding q = qualifier.resolveBinding();
                if (q instanceof ITypeBinding) {
                    context.pushStack(handleField((IVariableBinding) name1.resolveBinding(), null));
                } else if (q instanceof IVariableBinding) {
                    qualifier.accept(this);
                    context.pushStack(handleField((IVariableBinding) name1.resolveBinding(), popVar()));
                } else {
                    throw new NewFrontendException("Exp [ " + name + " ] can't be handled");
                }
                return false;
            }

            @Override
            public boolean visit(InfixExpression exp) {
                // if this expression is const value, just use it.
                if (exp.resolveConstantExpressionValue() != null) {
                    context.pushStack(TypeUtils.getRightPrimitiveLiteral(exp));
                    return false;
                }

                // if this expression type is [String], use another function to handle it
                if (exp.resolveTypeBinding().getBinaryName().equals(ClassNames.STRING)) {
                    genStringBuild(exp);
                    return false;
                }

                var op = exp.getOperator();
                var opStr = op.toString();
                // handle arithmetic expressions
                switch (opStr) {
                    case "+", "-", "*", "/", "%" -> {
                        binaryCompute(exp, (l, r) ->
                                new ArithmeticExp(TypeUtils.getArithmeticOp(op), l, r));
                        return false;
                    }
                    case "<<", ">>", ">>>" -> {
                        binaryCompute(exp, (l, r) ->
                                new ShiftExp(TypeUtils.getShiftOp(op), l, r));
                        return false;
                    }
                    case "|", "^", "&" -> {
                        binaryCompute(exp, (l, r) ->
                                new BitwiseExp(TypeUtils.getBitwiseOp(op), l, r));
                        return false;
                    }
                    case ">", ">=", "==", "<=", "<", "!=" -> {
                        binaryCompute(exp, (l, r) ->
                                new ConditionExp(TypeUtils.getConditionOp(op), l, r));
                        return false;
                    }
                    case "||", "&&" -> {
                        Var v = collectBoolEval(exp);
                        context.pushStack(v);
                        return false;
                    }
                    default -> throw new NewFrontendException("Operator <" + exp.getOperator() + "> not implement");
                }
            }





            @Override
            public boolean visit(ParenthesizedExpression exp) {
                return true;
            }

            @Override
            public boolean visit(NullLiteral literal) {
                context.pushStack(pascal.taie.ir.exp.NullLiteral.get());
                return false;
            }

            @Override
            public boolean visit(NumberLiteral literal) {
                context.pushStack(TypeUtils.getRightPrimitiveLiteral(literal));
                return false;
            }

            @Override
            public boolean visit(StringLiteral literal) {
                context.pushStack(TypeUtils.getStringLiteral(literal));
                return false;
            }

            @Override
            public boolean visit(Assignment ass) {
                var lExp = ass.getLeftHandSide();
                var rExp = ass.getRightHandSide();
                // if left-hand side is a SimpleName, and right-hand side is a literal
                // tai-e ir need to put literal into result var
                // so just visit left will not output the correct ir
                // this need to be carefully handled
                handleAssignment(lExp, rExp);
                return false;
            }

            /**
             * @apiNote <p> this function only applied to [for init]
             * and wrapped [Expression Statement] </p>
             * <p> for api compatible, we still push a null into stack </p>
             */
            @Override
            public boolean visit(VariableDeclarationExpression vde) {
                var singleDecls = vde.fragments();
                for (var i : singleDecls) {
                    var singleDecl = (VariableDeclarationFragment) i;
                    handleSingleVarDecl(singleDecl);
                }
                context.pushStack(pascal.taie.ir.exp.NullLiteral.get());
                return false;
            }

            public Exp makeInvoke(Expression object, IMethodBinding binding, List<Expression> args) {
                IMethodBinding decl = binding.getMethodDeclaration();
                int modifier = decl.getModifiers();
                MethodRef ref = getMethodRef(binding);
                Exp exp;
                List<Type> paramsType = Arrays.stream(binding.getParameterTypes())
                        .map(TypeUtils::JDTTypeToTaieType)
                        .toList();// 1. if this method is [static], that means it has nothing to do with [object]
                //    JDT has resolved the binding of method
                if (Modifier.isStatic(modifier)) {
                    exp = listCompute(args, paramsType, (l) -> new InvokeStatic(ref, l));
                    return exp;
                }
                // here, if [object] is [null], we can confirm it's a call to [this]
                Var o;
                boolean checkInterface;
                // TODO: not correct! what if this is inner class?
                if (object == null) {
                    o = thisVar;
                    assert targetClass != null;
                    checkInterface = targetClass.isInterface();
                } else {
                    object.accept(this);
                    o = popVar();
                    checkInterface = object.resolveTypeBinding().isInterface();
                }
                // 2. if the type [object] is [Interface], then use [InvokeInterface]
                // TODO: implement [JEP 181](https://openjdk.java.net/jeps/181)
                // TODO: check [ArrayType::clone, ArrayType::length]
                if (checkInterface) {
                    exp = listCompute(args, paramsType, l -> new InvokeInterface(ref, o, l));
                }
                // 3. if the name of this method call is "<init>"
                //    or this method is [private]
                //    then use [InvokeSpecial]
                else if (ref.getName().equals("<init>") || Modifier.isPrivate(modifier)) {
                    exp = listCompute(args, paramsType, l -> new InvokeSpecial(ref, o, l));
                }
                // 4. otherwise, use [InvokeVirtual]
                else {
                    exp = listCompute(args, paramsType, l -> new InvokeVirtual(ref, o, l));
                }
                return exp;
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(MethodInvocation mi) {
                Expression object = mi.getExpression();
                IMethodBinding binding = mi.resolveMethodBinding();
                List l = mi.arguments();
                Exp invoke = makeInvoke(object, binding, l);
                context.pushStack(invoke);
                return false;
            }

            @Override
            public boolean visit(ThisExpression te) {
                Name name = te.getQualifier();
                if (name != null) {
                    Type t = TypeUtils.JDTTypeToTaieType(name.resolveTypeBinding());
                    Var v = newVar(THIS, t);
                    context.pushStack(v);
                } else {
                    context.pushStack(thisVar);
                }
                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(ClassInstanceCreation cic) {
                ITypeBinding binding = cic.resolveTypeBinding();
                Type type = TypeUtils.JDTTypeToTaieType(binding);
                IMethodBinding methodBinding = cic.resolveConstructorBinding();
                MethodRef init = getInitRef(methodBinding);
                Var temp = genNewObject(init, cic.arguments(), type);
                context.pushStack(temp);
                return false;
            }


            private void initArr(Expression exp, LValue access) {
                if (exp instanceof ArrayInitializer init) {
                    var exps = init.expressions();
                    boolean isOneDim;
                    if (init.expressions().size() == 0) {
                        isOneDim = true;
                    } else {
                        isOneDim = !(init.expressions().get(0) instanceof ArrayInitializer);
                    }
                    var type = (pascal.taie.language.type.ArrayType)
                            TypeUtils.JDTTypeToTaieType(init.resolveTypeBinding());
                    Var length = context.getInt(this, init.expressions().size());
                    List<Var> lengths = new ArrayList<>();
                    lengths.add(length);
                    newAssignment(access, isOneDim ? new NewArray(type, length) :
                            new NewMultiArray(type, lengths));
                    for (var i = 0; i < init.expressions().size(); ++i) {
                        context.pushStack(access);
                        initArr((Expression) exps.get(i), new ArrayAccess(popVar(), context.getInt(this, i)));
                    }
                } else {
                    exp.accept(this);
                    newAssignment(access, popVar(access.getType()));
                }
            }

            private void handleArrInit(ArrayInitializer init) {
                ITypeBinding typeBinding = init.resolveTypeBinding();
                var type = (pascal.taie.language.type.ArrayType) TypeUtils.JDTTypeToTaieType(typeBinding);
                Var temp = newTempVar(type);
                initArr(init, temp);
                context.pushStack(temp);
            }

            // Tai-e IR can represent both fix-sized and var-sized multiArray
            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(ArrayCreation ac) {
                ArrayType t = ac.getType();
                var taieType = (pascal.taie.language.type.ArrayType) TypeUtils.JDTTypeToTaieType(t.resolveBinding());
                ArrayInitializer arrInit = ac.getInitializer();
                if (t.getDimensions() == 1 && arrInit == null) {
                    // if this is one dimension array, use [NewArray]
                    Expression exp = (Expression) ac.dimensions().get(0);
                    exp.accept(this);
                    Var length = popVar(PrimitiveType.INT);
                    context.pushStack(new NewArray(taieType, length));
                } else if (arrInit == null) {
                    context.pushStack(
                            listCompute(ac.dimensions(),
                                    Collections.nCopies(ac.dimensions().size(), PrimitiveType.INT),
                                    l -> new NewMultiArray(taieType, l)));
                } else {
                    handleArrInit(ac.getInitializer());
                }
                return false;
            }

            @Override
            public boolean visit(ArrayInitializer init) {
                handleArrInit(init);
                return false;
            }

            @Override
            public boolean visit(org.eclipse.jdt.core.dom.ArrayAccess access) {
                access.getIndex().accept(this);
                access.getArray().accept(this);
                context.pushStack(new ArrayAccess(popVar(), popVar(PrimitiveType.INT)));
                return false;
            }

            @Override
            public boolean visit(ExpressionMethodReference mr) {
                IMethodBinding binding = mr.resolveMethodBinding();
                ITypeBinding typeBinding = mr.resolveTypeBinding();
                Type funcInterType = TypeUtils.JDTTypeToTaieType(typeBinding);
                MethodType targetType = MethodType.get(new ArrayList<>(), funcInterType);
                MethodType samMethodType = TypeUtils.extractFuncInterface(typeBinding);
                MethodType instantiatedMethodType = TypeUtils.getMethodType(binding);
                MethodRef ref = getMethodRef(binding);
                MethodHandle mh;
                boolean needObj = true;
                if (Modifier.isStatic(binding.getModifiers())) {
                    mh = MethodHandle.get(MethodHandle.Kind.REF_invokeStatic, ref);
                    needObj = false;
                } else {
                    if (mr.getExpression() instanceof Name n) {
                        // if [mr] is [C::inst], where [inst] is instance method,
                        // we don't need to push [object] into [dynArgs]
                        needObj = !(n.resolveBinding().getKind() == IBinding.TYPE);
                    }
                    if (mr.getExpression().resolveTypeBinding().isInterface()) {
                        mh = MethodHandle.get(MethodHandle.Kind.REF_invokeInterface, ref);
                    } else if (Modifier.isPrivate(binding.getModifiers())) {
                        mh = MethodHandle.get(MethodHandle.Kind.REF_invokeSpecial, ref);
                    } else {
                        mh = MethodHandle.get(MethodHandle.Kind.REF_invokeVirtual, ref);
                    }
                }
                List<Var> dynArgs = new ArrayList<>();
                if (needObj) {
                    mr.getExpression().accept(this);
                    dynArgs.add(popVar());
                }
                List<Literal> staticArgs = new ArrayList<>();
                staticArgs.add(samMethodType);
                staticArgs.add(mh);
                staticArgs.add(instantiatedMethodType);
                InvokeDynamic inDy = new InvokeDynamic(TypeUtils.getMetaFactory(),
                        binding.getName(), targetType, staticArgs, dynArgs);
                context.pushStack(inDy);
                return false;
            }

            @Override
            public boolean visit(CastExpression cast) {
                cast.getExpression().accept(this);
                Exp exp = context.popStack();
                context.pushStack(genNewCast(exp,
                        JDTTypeToTaieType(cast.getType().resolveBinding())));
                return false;
            }

            @Override
            public boolean visit(BooleanLiteral b) {
                context.pushStack(getRightPrimitiveLiteral(b));
                return false;
            }

            @Override
            public boolean visit(CharacterLiteral c) {
                context.pushStack(getRightPrimitiveLiteral(c));
                return false;
            }

            @Override
            public boolean visit(PrefixExpression pe) {
                switch (pe.getOperator().toString()) {
                    case "!" -> {
                        collectBoolEval(pe);
                        return false;
                    }
                }
                return false;
            }

            @Override
            public boolean visit(ConditionalExpression ce) {
                Exp e = genCondExpression(ce);
                context.pushStack(e);
                return false;
            }
        }
    }
}

