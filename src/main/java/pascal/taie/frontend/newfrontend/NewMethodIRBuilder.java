package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import soot.SootMethod;


import java.util.*;
import java.util.function.Consumer;

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
    private final boolean notHandle;

    // Method to be built
    private MethodDeclaration targetMethod;

    // IR to be built;
    private IR buildRes;

    public NewMethodIRBuilder(String sourceFilePath, String sourceFileName, SootMethod method, JMethod jMethod) {
        this.sourceFilePath = sourceFilePath;
        this.sourceFileName = sourceFileName;
        this.methodName = method.getName();
        this.jMethod = jMethod;
        this.methodSig = method.getSubSignature();
        this.className = method.getDeclaringClass().getName();
        this.notHandle = checkIfNotHandle(method);
        // this.targetClass = ClassHierarchyHolder.getClassHierarchy().getClass(className).getType();
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

        private void buildThis() {
            // thisVar = new Var(jMethod, "this", targetClass);
        }

        private void addStmt(Stmt stmt) {
            stmt.setIndex(stmts.size());
            stmts.add(stmt);
        }

        private void regVar(Var v) {
            vars.add(v);
        }

        private Var newTempVar(ITypeBinding type) {
            int tempNow = tempCounter;
            tempCounter++;
            var v = new Var(jMethod, "temp$" + tempNow, TypeUtils.JDTTypeToTaieType(type));
            regVar(v);
            return v;
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

        private static boolean isSimple(Expression exp) {
            return exp instanceof Name;
        }

        private void processIfNotSimple(Expression exp, Consumer<Var> f) {
            if (IRGenerator.isSimple(exp)) {
                var visitor = new ExpGenerateVisitor();
                exp.accept(visitor);
                assert (visitor.contVar != null);
                f.accept(visitor.contVar);
            } else {
                var temp = newTempVar(exp.resolveTypeBinding());
                var visitor = new ExpGenerateVisitor(temp);
                exp.accept(visitor);
                f.accept(temp);
            }
        }

        private void buildStmt() {
            var visitor = new StmtGenerateVisitor();
            targetMethod.accept(visitor);
        }

        class StmtGenerateVisitor extends ASTVisitor {
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
                processIfNotSimple(exp, (var v) -> {
                    ret = v;
                    addStmt(new Return(v));
                });
                return false;
            }
        }

        class ExpGenerateVisitor extends ASTVisitor {

            /**
             * <p>Var for continuation of this exp</p>
             * <p>if exp is not {@code Name}, this field need to be given outside</p>
             * <p>else, after visit this field will be set to the variable correspond to the {@code Name}</p>
             */
            private Var contVar;

            // Only when exp is a simple expression (i.e. Name)
            public ExpGenerateVisitor() {}

            public ExpGenerateVisitor(ITypeBinding contType) {
                contVar = newTempVar(contType);
            }

            public ExpGenerateVisitor(Var contVar) {
                this.contVar = contVar;
            }

            public Var getContVar() {
                return contVar;
            }

            @Override
            public boolean visit(SimpleName name) {
                var binding = name.resolveBinding();
                if (binding instanceof IVariableBinding) {
                    var v = bindingVarMap.get(binding);
                    if (v == null) {
                        throw new NewFrontendException("Binding <" + binding + "> can't be resolved");
                    }
                    contVar = v;
                } else {
                    throw new NewFrontendException("Exp <" + name + "> can't be handled, not implement");
                }
                return false;
            }

            @Override
            public boolean visit(InfixExpression exp) {
                if (exp.getOperator().equals(InfixExpression.Operator.PLUS)) {
                    processIfNotSimple(exp.getLeftOperand(), (var lVar) -> {
                        processIfNotSimple(exp.getRightOperand(), (var rVar) -> {
                            if (exp.extendedOperands().size() == 0) {
                                addStmt(new Binary(contVar,
                                        new ArithmeticExp(ArithmeticExp.Op.ADD, lVar, rVar)));
                            } else {
                                var temp = newTempVar(exp.resolveTypeBinding());
                                addStmt(new Binary(temp, new ArithmeticExp(ArithmeticExp.Op.ADD, lVar, rVar)));
                                var extOperands = exp.extendedOperands();
                                var i = extOperands.iterator();
                                while (i.hasNext()) {
                                    var expNow = i.next();
                                    var tempNow = i.hasNext() ?
                                            newTempVar(exp.resolveTypeBinding()) : contVar;
                                    var finalTemp = temp;
                                    processIfNotSimple((Expression) expNow, (var v) -> {
                                        addStmt(new Binary(tempNow, new ArithmeticExp(ArithmeticExp.Op.ADD, finalTemp, v)));
                                    });
                                    temp = tempNow;
                                }
                            }
                        });
                    });
                    return false;
                } else {
                    throw new NewFrontendException("Operator <" + exp.getOperator() + "> not implement");
                }
            }
        }
    }
}

