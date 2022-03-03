package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;

import pascal.taie.frontend.newfrontend.exposed.WorldParaHolder;
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import soot.SootMethod;


import java.util.*;

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
            logger.error(e);
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
        private Var ret;
        private Var thisVar;

        private int tempCounter;

        public IRGenerator() {
            stmts = new ArrayList<>();
            params = new ArrayList<>();
            vars = new ArrayList<>();
            bindingVarMap = Maps.newMap();
            tempCounter = 0;
        }

        public IR build() {
            buildThis();
            buildPara();
            buildStmt();
            Set<Var> rets = Sets.newSet();
            rets.add(ret);
            return new DefaultIR(jMethod, thisVar, params, rets, vars, stmts, new ArrayList<>());
        }

        private Var newVar(String name, Type type) {
            var v = new Var(jMethod, name, type);
            regVar(v);
            return v;
        }

        private void regVar(Var v) {
            vars.add(v);
        }

        private Var newTempVar(Type type) {
            int tempNow = tempCounter;
            tempCounter++;
            var v = new Var(jMethod, "temp$" + tempNow, type);
            regVar(v);
            return v;
        }

        Var newConstantVar(Literal literal) {
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
            Var var = new Var(jMethod, varName, literal.getType(), literal);
            regVar(var);
            return var;
        }

        private Var getBinding(IBinding binding) {
            return bindingVarMap.computeIfAbsent(binding, (b) -> {
                var v = newVar(b.getName(), TypeUtils.JDTTypeToTaieType(((IVariableBinding) b).getType()));
                return v;
            });
        }

        private void addStmt(int lineNo, Stmt stmt) {
            stmt.setLineNumber(lineNo);
            stmt.setIndex(stmts.size());
            stmts.add(stmt);
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
                var aVar = new Var(jMethod, nameString, type);
                params.add(aVar);
                bindingVarMap.put(name.resolveBinding(), aVar);
            }
        }

        /**
         * @apiNote In tai-e design, method reference is also a literal,
         * but for now, we choose to not handle that.
         */
        private static boolean isLiteral(Expression exp) {
            return exp instanceof BooleanLiteral ||
                    exp instanceof CharacterLiteral ||
                    exp instanceof NullLiteral ||
                    exp instanceof NumberLiteral ||
                    exp instanceof StringLiteral ||
                    exp instanceof TypeLiteral;
        }

        private void buildStmt() {
            var visitor = new StmtGenerateVisitor();
            targetMethod.accept(visitor);
        }


        class VisitorContext {
            private final Stack<Exp> expStack;

            public VisitorContext() {
                expStack = new Stack<>();
            }

            public void pushStack(Exp exp) {
                expStack.add(exp);
            }

            public Exp popStack() {
                return expStack.pop();
            }

        }

        class LinenoASTVisitor extends ASTVisitor {
            private int lineno;
            protected final VisitorContext context;

            public LinenoASTVisitor() {
                this.context = new VisitorContext();
            }

            public LinenoASTVisitor(VisitorContext context) {
                this.context = context;
            }

            protected void addStmt(Stmt stmt) {
                IRGenerator.this.addStmt(lineno, stmt);
            }

            @Override
            public void preVisit(ASTNode node) {
                this.lineno = linenoManger.getLineno(node);
            }

            protected Var expToVar(Exp exp) {
                if (exp instanceof Var v) {
                    return v;
                } else if (exp instanceof BinaryExp exp1) {
                    var v = newTempVar(exp1.getType());
                    addStmt(new Binary(v, exp1));
                    return v;
                } else if (exp instanceof Literal l) {
                    var v = newConstantVar(l);
                    addStmt(new AssignLiteral(v, l));
                    return v;
                } else {
                    throw new NewFrontendException(exp + "is not implemented");
                }
            }

            protected Var popVar() {
                return expToVar(context.popStack());
            }

            protected Var[] popVar2() {
                var v2 = context.popStack();
                var v1 = context.popStack();
                return new Var[] {expToVar(v1), expToVar(v2)};
            }
        }

        class StmtGenerateVisitor extends LinenoASTVisitor {

            public StmtGenerateVisitor() {
                super();
            }

            @Override
            public boolean visit(Block block) {
                var stmts = block.statements();
                for (var i : stmts) {
                    assert (i instanceof Statement);
                    Statement stmt = (Statement) i;
                    stmt.accept(this);
                }
                return false;
            }

            @Override
            public boolean visit(ReturnStatement rs) {
                var exp = rs.getExpression();
                exp.accept(new ExpVisitor(context));
                var retVar = popVar();
                addStmt(new Return(retVar));
                ret = retVar;
                return false;
            }
        }

        class ExpVisitor extends LinenoASTVisitor {

            private ExpVisitor() {  }

            ExpVisitor(VisitorContext context) {
                super(context);
            }

            @Override
            public boolean visit(SimpleName name) {
                var binding = name.resolveBinding();
                if (binding instanceof IVariableBinding) {
                    context.pushStack(getBinding(binding));
                } else {
                    throw new NewFrontendException("Exp <" + name + "> can't be handled, not implement");
                }
                return false;
            }

            @Override
            public boolean visit(InfixExpression exp) {
                if (exp.getOperator().equals(InfixExpression.Operator.PLUS)) {
                    exp.getLeftOperand().accept(this);
                    exp.getRightOperand().accept(this);
                    // NOTE: sequence is reversed to push
                    var lrVar = popVar2();
                    context.pushStack(new ArithmeticExp(ArithmeticExp.Op.ADD, lrVar[0], lrVar[1]));
                    var extOperands = exp.extendedOperands();
                    for (var i : extOperands) {
                        var expNow = (Expression) i;
                        expNow.accept(this);
                        var lrVarNow = popVar2();
                        context.pushStack(new ArithmeticExp(ArithmeticExp.Op.ADD, lrVarNow[0], lrVarNow[1]));
                    }
                    return false;
                } else {
                    throw new NewFrontendException("Operator <" + exp.getOperator() + "> not implement");
                }
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
        }
    }
}

