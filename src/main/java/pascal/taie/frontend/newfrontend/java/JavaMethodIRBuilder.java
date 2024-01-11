package pascal.taie.frontend.newfrontend.java;

import javax.annotation.Nullable;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
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
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import pascal.taie.World;
import pascal.taie.frontend.newfrontend.JavaMethodSource;
import pascal.taie.frontend.newfrontend.Utils;
import pascal.taie.ir.DefaultIR;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.ComparisonExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InstanceOfExp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.InvokeInterface;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.ir.exp.NegExp;
import pascal.taie.ir.exp.NewArray;
import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.NewMultiArray;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.StaticFieldAccess;
import pascal.taie.ir.exp.UnaryExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.InstanceOf;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static pascal.taie.frontend.newfrontend.java.JDTStringReps.getBinaryName;
import static pascal.taie.frontend.newfrontend.java.MethodCallBuilder.*;
import static pascal.taie.frontend.newfrontend.java.TypeUtils.*;


// TODO: check all the .clone() calls
public class JavaMethodIRBuilder {

    private final JMethod jMethod;

    // binary name
    private final String className;

    // Method to be built
    private final MethodDeclaration targetMethod;

    // class where method is declared
    private final JClass targetClass;

    // use linenoManger to retrieve line number;
    private final LinenoManger linenoManger;

    private final JavaMethodSource source;

    private final InnerClassDescriptor innerDesc;

    private final AST rootAst;

    private final ASTNode anonymousParent;

    private final List<ITypeBinding> synParaTypes;

    private final List<String> synParaNames;

    public JavaMethodIRBuilder(JavaMethodSource methodSource, JMethod jMethod) {
        this.jMethod = jMethod;
        this.className = jMethod.getDeclaringClass().getName();
        this.targetClass = jMethod.getDeclaringClass();
        this.targetMethod = methodSource.decl();
        this.linenoManger = new LinenoManger(methodSource.cu());
        this.source = methodSource;
        this.rootAst = targetMethod == null ? methodSource.cu().getAST() : targetMethod.getAST();
        this.innerDesc = InnerClassManager.get().getInnerClassDesc(
                getDeclClass());
        if (getDeclClass().isAnonymous()) {
            anonymousParent = source.source().getTypeDeclaration().getParent();
        } else {
            anonymousParent = null;
        }

        if (isCtor()) {
            if (isEnumType(getDeclClass())) {
                synParaTypes = List.of(rootAst.resolveWellKnownType(ClassNames.STRING),
                        rootAst.resolveWellKnownType(JDT_INT));
                synParaNames = List.of(getAnonymousSynCtorArgName(0), getAnonymousSynCtorArgName(1));
            } else if (innerDesc != null) {
                synParaTypes = innerDesc.synParaTypes();
                synParaNames = innerDesc.synParaNames();
            } else {
                synParaTypes = List.of();
                synParaNames = List.of();
            }
        } else {
            synParaTypes = List.of();
            synParaNames = List.of();
        }
    }

    private ITypeBinding getDeclClass() {
        return ClassExtractor.getBinding(source.source().getTypeDeclaration());
    }

    public IR build() {
        IRGenerator generator = new IRGenerator();
        return generator.build();
    }

    private MethodDeclaration getTargetMethod() {
        return targetMethod;
    }

    private boolean isTargetClass(ITypeBinding binding) {
        return getBinaryName(binding).equals(className);
    }

    private boolean isSuperClass(ITypeBinding binding) {
        return World.get().getTypeSystem().isSubtype(
                JDTTypeToTaieType(binding),
                targetClass.getType());
    }

    private boolean isCtor() {
        return jMethod.isConstructor();
    }

    private boolean needToBuildInnerClassAssign() {
        return isCtor() && innerDesc != null;
    }

    private boolean needToSendArgToAnotherCtor() {
        return !synParaNames.isEmpty();
    }

    private boolean isEnumCtor() {
        return isCtor() && getDeclClass().isEnum();
    }

    class IRGenerator {

        private static final String THIS = "%this";

        private static final String STRING_CONSTANT = "%stringconst";

        private static final String CLASS_CONSTANT = "%classconst";

        private static final String NULL_CONSTANT = "%nullconst";

        /**
         * params of this method
         */
        private final List<Var> params;

        /**
         * all vars of this method. note every var need to be added in this list
         */
        private final List<Var> vars;

        /**
         * exception entries of this method
         */
        private final List<ExceptionEntry> exceptionList;

        /**
         * return value of this method
         */
        private final Set<Var> retVar;

        /**
         * a var in this method represent 'this'
         */
        private Var thisVar;

        /**
         * index counter of temp variable
         */
        private int tempCounter;

        /**
         * instance for visit {@code Statement}
         */
        private final StmtGenerateVisitor stmtVisitor;

        /**
         * instance for visit {@code Expression}
         */
        private final ExpVisitor expVisitor;

        /**
         * context of {@code stmtVisitor} and {@code  expVisitor}
         */
        private final VisitorContext context;

        /**
         * manager to manage stmts
         */
        private final StmtManager stmtManager;

        public IRGenerator() {
            params = new ArrayList<>();
            vars = new ArrayList<>();
            tempCounter = 0;
            context = new VisitorContext();
            stmtVisitor = new StmtGenerateVisitor(context);
            expVisitor = new ExpVisitor(context);
            exceptionList = new ArrayList<>();
            retVar = Sets.newSet();
            stmtManager = new StmtManager();
        }

        public StmtManager getStmtManager() {
            return stmtManager;
        }

        public IR build() {
            buildThis();
            buildPara();
            buildBody();
            return new DefaultIR(jMethod, thisVar, params,
                    retVar, vars, stmtManager.getStmts(), exceptionList);
        }

        private int getVarIndex() {
            return vars.size();
        }

        /**
         * this function return a new tai-e Var, used for variable in source
         *
         * @param name name of this variable
         * @param type type of this variable
         * @return a tai-e var for this varibable
         */
        private Var newVar(String name, Type type) {
            var v = new Var(jMethod, name, type, getVarIndex());
            regVar(v);
            return v;
        }

        private void regVar(Var v) {
            vars.add(v);
        }

        /**
         * this function return a new temporary variable, used for generated variable
         *
         * @param type type of variable to be returned
         * @return a new variable {@code v} with {@code v.getType()} = {@code type}
         */
        private Var newTempVar(Type type) {
            int tempNow = tempCounter;
            tempCounter++;
            var v = new Var(jMethod, "#temp$" + tempNow, type, getVarIndex());
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

        private Type getReturnType() {
            return jMethod.getReturnType();
        }

        private void addRetVar(Var v) {
            retVar.add(v);
        }

        private void buildThis() {
            if (! jMethod.isStatic()) {
                this.thisVar = newVar(THIS, targetClass.getType());
            }
        }

        @SuppressWarnings("unchecked")
        private void buildPara() {
            for (int i = 0; i < jMethod.getParamCount(); ++i) {
                String name = jMethod.getParamName(i);
                assert name != null;
                params.add(newVar(name, jMethod.getParamType(i)));
            }

            if (targetMethod != null) {
                List<SingleVariableDeclaration> paraTree = targetMethod.parameters();
                for (int i = 0; i < paraTree.size(); ++i) {
                    SingleVariableDeclaration svd_i = paraTree.get(i);
                    context.putBinding((IVariableBinding) svd_i.getName().resolveBinding(),
                            params.get(i + synParaNames.size()));
                }
            }

            assert params.size() == jMethod.getParamCount();
        }

        private ITypeBinding getSuperClass(ITypeBinding binding) {
            ITypeBinding superClass = binding.getSuperclass();
            if (superClass != null) {
                return superClass;
            } else {
                return rootAst.resolveWellKnownType(ClassNames.OBJECT);
            }
        }

        private void buildBody() {
            if (jMethod.getName().equals(MethodNames.CLINIT)) {
                buildClinit();
                context.pushFlow(true);
            } else if (isEnumType(getDeclClass()) && jMethod.getName().equals(ENUM_METHOD_VALUES)) {
                buildEnumValues();
            } else if (isEnumType(getDeclClass()) && jMethod.getName().equals(ENUM_METHOD_VALUE_OF)) {
                buildEnumValuesOf();
            } else {
                buildCtor();
                if (targetMethod != null) {
                    buildStmt();
                } else {
                    context.pushFlow(true);
                }
            }
            buildEnd();
        }

        public void buildEnd() {
            context.confirmNoDetach();
            getStmtManager().resolveAll();
            context.getExceptionManager().resolveEntry(getStmtManager().blockMap);
        }

        /**
         * Assign compiler generated param to the corresponding field
         * This occurs when this class is an inner class
         */
        private void buildInnerClassAssign() {
            if (! needToBuildInnerClassAssign()) {
                return;
            }

            for (int i = 0; i < synParaTypes.size(); ++i) {
                String name = synParaNames.get(i);
                JField field = jMethod.getDeclaringClass().getDeclaredField(name);
                assert field != null;
                assert params.size() > i;
                Var param = params.get(i);
                newAssignment(new InstanceFieldAccess(field.getRef(), getThisVar()), param);
            }
        }

        /**
         * build IR for the values() function of an enum
         */
        private void buildEnumValues() {
            JField values = jMethod.getDeclaringClass().getDeclaredField(ENUM_VALUES);
            assert values != null;
            context.pushStack(new StaticFieldAccess(values.getRef()));
            Var v = popVar();
            context.pushStack(new InvokeVirtual(getArrayClone(), v, List.of()));
            v = popVar();
            context.pushStack(new CastExp(v, values.getType()));
            v = popVar();
            addStmt(new Return(v));
            addRetVar(v);
            context.pushFlow(false);
        }

        /**
         * build IR for the valueOf() function of an enum
         */
        private void buildEnumValuesOf() {
            context.pushStack(ClassLiteral.get(jMethod.getDeclaringClass().getType()));
            Var v = popVar();
            context.pushStack(new InvokeStatic(getEnumMethodValueOf(), List.of(v, params.get(0))));
            v = popVar();
            context.pushStack(new CastExp(v, jMethod.getDeclaringClass().getType()));
            v = popVar();
            addStmt(new Return(v));
            addRetVar(v);
            context.pushFlow(false);
        }

        private void buildClinit() {
            for (JavaClassInit init : source.source().getClassInits()) {
                if (init instanceof StaticInit init1) {
                    visitStmt(init1.initializer().getBody());
                } else if (init instanceof EnumInit init2) {
                    buildEnumInit(init2);
                } else if (init instanceof FieldInit init3) {
                    buildFieldInit(init3);
                }
            }
        }

        /**
         * build the init function of an enum.
         *
         * <ol>
         *     <li>for each constant, create and assign it to a static field</li>
         *     <li>collect all created constants to an array, assign it to VALUES field</li>
         * </ol>
         */
        @SuppressWarnings("unchecked")
        private void buildEnumInit(EnumInit enumInit) {
            List<Var> consts = new ArrayList<>();
            for (int i = 0; i < enumInit.enumConsts().size(); ++i) {
                EnumConstantDeclaration enumConst = enumInit.enumConsts().get(i);
                String name = enumConst.getName().toString();
                List<Var> args = new ArrayList<>();
                context.pushStack(pascal.taie.ir.exp.StringLiteral.get(name));
                args.add(popVar());
                context.pushStack(IntLiteral.get(i));
                args.add(popVar());
                IMethodBinding ctor = enumConst.resolveConstructorBinding();
                List<Expression> arguments = enumConst.arguments();
                Pair<List<Type>, List<Expression>> argTypes = makeParamAndArgs(ctor, arguments);
                for (Expression exp : argTypes.second()) {
                    visitExp(exp);
                    args.add(popVar());
                }
                ClassType type = Objects.requireNonNull(getTaieClass(ctor.getDeclaringClass())).getType();
                MethodRef enumAnonymousInitRef = getEnumInitRef(ctor, false);
                Var now = genNewObject1(enumAnonymousInitRef, args, type);
                assert now.getType() != null;
                JField f = jMethod.getDeclaringClass().getDeclaredField(name);
                assert f != null;
                newAssignment(new StaticFieldAccess(f.getRef()), now);
                consts.add(now);
            }
            JField values = jMethod.getDeclaringClass().getDeclaredField(ENUM_VALUES);
            assert values != null;
            context.pushStack(IntLiteral.get(enumInit.enumConsts().size()));
            Var length = popVar();
            context.pushStack(new NewArray((ArrayType) values.getType(), length));
            Var arr = popVar();
            for (int i = 0; i < consts.size(); ++i) {
                context.pushStack(IntLiteral.get(i));
                Var idx = popVar();
                newAssignment(new ArrayAccess(arr, idx), consts.get(i));
            }
            newAssignment(new StaticFieldAccess(values.getRef()), arr);
        }

        private void buildCtor() {
            if (!isCtor()) {
                return;
            }

            // see JLS 8. Chap 12.5 Creation of New Class Instances
            if (targetMethod == null) {
                buildEmptyCtor();
            } else {
                AtomicBoolean meetThisCall = new AtomicBoolean(false);
                AtomicBoolean meetSuperCall = new AtomicBoolean(false);

                // `javac` will the inner class assign process before `step 2.`
                // we follow `javac`
                targetMethod.accept(new ASTVisitor() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public boolean visit(ConstructorInvocation node) {
                        // step 2.
                        meetThisCall.set(true);
                        IMethodBinding binding = node.resolveConstructorBinding();
                        List<Var> synArgs = new ArrayList<>();
                        if (needToSendArgToAnotherCtor()) {
                            for (int i = 0; i < synParaNames.size(); ++i) {
                                assert params.get(i).getType().equals(
                                        JDTTypeToTaieType(synParaTypes.get(i)));
                                synArgs.add(params.get(i));
                            }
                        }
                        List<Expression> args = node.arguments();
                        InvokeInstanceExp invoke = getInitInvoke(getThisVar(), synArgs, args,
                                binding, getInitRef(binding));
                        addStmt(new Invoke(jMethod, invoke));
                        context.pushFlow(true);
                        return false;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public boolean visit(SuperConstructorInvocation node) {
                        meetSuperCall.set(true);
                        buildInnerClassAssign();
                        InvokeInstanceExp invoke = getSuperInitInvoke(node.getExpression(),
                                node.resolveConstructorBinding(),
                                node.arguments());
                        addStmt(new Invoke(jMethod, invoke));
                        context.pushFlow(true);
                        return false;
                    }
                });

                if (meetThisCall.get()) {
                    // step 5. build body
                    return;
                }

                // step 3.
                if (! meetSuperCall.get()) {
                    buildInnerClassAssign();
                    genSuperInitCall();
                }

                // step 4.
                buildInstanceInit();
                // step 5. build body
                // return;
            }
        }

        private void buildEmptyCtor() {
            buildInnerClassAssign();
            genSuperInitCall();
            buildInstanceInit();
        }

        private void appendSynArgsForSuperInit(List<Var> synArgs, ITypeBinding superType) {
            InnerClassDescriptor descriptor = InnerClassManager.get().getInnerClassDesc(superType);
            if (descriptor != null) {
                appendEnclosedInstance(null, synArgs, descriptor);
                appendCaptureSynArgs(synArgs, descriptor);
            }
        }

        /**
         * Creation of New Class Instances, step 3.
         * <p>
         * If this constructor is for
         * a class other than {@link Object}, then this constructor will begin with an explicit
         * or implicit invocation of a superclass constructor (using super)
         * </p>
         */
        @SuppressWarnings("unchecked")
        private void genSuperInitCall() {
            // Object can never be built by this class
            JClass superClass = Objects.requireNonNull(jMethod.getDeclaringClass().getSuperClass());
            ITypeBinding superClassBinding = getSuperClass(getDeclClass());
            if (anonymousParent != null) {
                if (anonymousParent instanceof EnumConstantDeclaration enumConst) {
                    assert isEnumCtor();
                    InvokeInstanceExp invoke = new InvokeSpecial(
                            getEnumInitRef(enumConst.resolveConstructorBinding(), true), getThisVar(),
                            params);
                    addStmt(new Invoke(jMethod, invoke));
                } else if (anonymousParent instanceof ClassInstanceCreation creation) {
                    Expression expression = creation.getExpression();
                    IMethodBinding binding = resolveSuperInitForAnonymous(
                            creation.resolveConstructorBinding());
                    // the super init call in the <init>() of an anonymous class
                    // Example:
                    //    a . new SuperClass(args) { }
                    // => <init>( this$0?, capturedByThis, args ) {
                    //        super(enclosedInstance, capturedBySuper, ctorArgs)
                    //    }
                    //    If `expression` is null, then `enclosedInstance` is `this$0`
                    //    Or else, `enclosedInstance` is `args[0]`
                    //    Use normal protocol to get `capturedBySuper`
                    //    `ctorArgs` can be retrieved from the args of current method
                    InnerClassDescriptor desc = InnerClassManager.get().getInnerClassDesc(
                            binding.getDeclaringClass());
                    List<Var> result = new ArrayList<>();
                    MethodRef ref = getInitRef(binding);
                    int synArgsLength = innerDesc.synParaNames().size();
                    List<Var> args = params.subList(synArgsLength, params.size());
                    int argsPointer = 0;
                    // first, supply the `enclosedInstance` and `capturedBySuper`
                    if (desc != null) {
                        if (expression == null) {
                            // `this$1` is `this$0`,
                            // or no need for `this$1` (this is handled in `appendEnclosedInstance`)
                            appendEnclosedInstance(null, result, desc);
                        } else {
                            // `this$1 is `args[0]`
                            result.add(args.get(argsPointer++));
                        }
                        appendCaptureSynArgs(result, desc);
                    }
                    // then, supply the `ctorArgs`
                    for (int i = argsPointer; i < args.size(); ++i) {
                        result.add(args.get(i));
                    }
                    addNewInvokeSpecial(getThisVar(), ref, result);
                } else {
                    throw new UnsupportedOperationException();
                }
            } else {
                if (superClass.getName().equals(ENUM)) {
                    assert params.size() >= 2;
                    List<Var> args = List.of(params.get(0), params.get(1));
                    MethodRef ref = MethodRef.get(getClassByName(ENUM).getJClass(),
                            MethodNames.INIT,
                            getEnumCtorType(), VoidType.VOID, false);
                    InvokeInstanceExp invoke = new InvokeSpecial(ref, getThisVar(), args);
                    addStmt(new Invoke(jMethod, invoke));
                } else {
                    List<Var> args = new ArrayList<>();
                    appendSynArgsForSuperInit(args, superClassBinding);
                    InvokeInstanceExp invoke = new InvokeSpecial(
                            getNonArgInitRef(superClass, superClassBinding), getThisVar(), args);
                    addStmt(new Invoke(jMethod, invoke));
                }
            }
        }

        /**
         * Creation of New Class Instances, step 4.
         * <p>
         * Execute the instance initializers and instance variable initializers for this class,
         * assigning the values of instance variable initializers to the corresponding
         * instance variables
         * </p>
         */
        private void buildInstanceInit() {
            for (JavaInit init : source.source().getInstanceInits()) {
                if (init instanceof BlockInit blockInit) {
                    stmtVisitor.visit(blockInit.init().getBody());
                } else if (init instanceof FieldInit fieldInit) {
                    buildFieldInit(fieldInit);
                }
            }
        }

        private void buildFieldInit(FieldInit fieldInit) {
            JField field = fieldInit.field();
            boolean isStatic = field.isStatic();
            Expression initExp = fieldInit.init();
            visitExp(initExp);
            Exp exp = context.popStack();
            newAssignmentMayConversion(isStatic ? new StaticFieldAccess(field.getRef())
                    : new InstanceFieldAccess(field.getRef(), getThisVar()), exp);
        }

        private void buildStmt() {
            targetMethod.getBody().accept(stmtVisitor);
        }

        private void visitStmt(Statement stmt) {
            stmt.accept(stmtVisitor);
        }

        private int nowPos() {
            return stmtManager.getTop();
        }

        private Stmt getStmt(int i) {
            return stmtManager.getStmt(i);
        }

        private Var getThisVar() {
            if (jMethod.isStatic()) {
                throw new UnsupportedOperationException();
            }
            return thisVar;
        }

        record ExceptionHandler(String target, Type type, String handler) {
        }

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
                var newFrame = new int[]{nowPos(), -1};
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

            /**
             * this function can only be called when stmt has built
             */
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

        static class BlockLabelGenerator {

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
             *
             * @return label, begin with '^'
             */
            public String getNewFinLabel() {
                return String.valueOf(FINALLY) + counter++;
            }

            public String getNewTryLabel() {
                return String.valueOf(TRY) + counter++;
            }

            public String getNewCatchLabel() {
                return String.valueOf(CATCH) + counter++;
            }

            public static boolean isFinallyBlock(String label) {
                return label.charAt(0) == FINALLY;
            }

            public static boolean isTryBlock(String label) {
                return label.charAt(0) == TRY;
            }

            public static boolean isCatchBlock(String label) {
                return label.charAt(0) == CATCH;
            }

            public static boolean isNormalBlock(String label) {
                return label.charAt(0) == NORMAL;
            }

            public static boolean isUserDefinedLabel(String label) {
                return !isNormalBlock(label) && !isCatchBlock(label) && !isFinallyBlock(label)
                        && !isTryBlock(label);
            }

        }


        class VisitorContext {
            /**
             * Tai-e Exp stack
             */
            private final Stack<Exp> expStack;

            /**
             * map a [label] to its continue and break target
             */
            private final Map<String, Pair<String, String>> brkContMap;

            /**
             * generator to generate new label
             */
            private final BlockLabelGenerator labelGenerator;

            /**
             * manager to manage exception entry
             */
            private final ExceptionEntryManager exceptionManager;

            /**
             * map a [label] to its block, this [label] need to be finally label
             */
            private final Map<String, Block> finallyBlockMap;

            /**
             * map a [label] to its finally label
             */
            private final Map<String, String> finallyLabelMap;

            /**
             * map a [IVariableBinding] to its corresponding tai-e Exp
             */
            private final Map<IVariableBinding, Exp> bindingVarMap;

            /**
             * a list record current control context (loop, try / catch)
             */
            private final LinkedList<String> contextCtrlList;

            /**
             * manager to manager binding for generated JDT ASTNode
             * <p> note: for now, only {@code ArrayCreation} and
             * {@code ArrayInitializer} could be generated  </p>
             */
            private final BindingManager bm;

            /**
             * a stack for control flow,
             * top of stack indicates if child Statement could complete normally
             */
            private final Stack<Boolean> flowStack;

            /**
             * a map to record literal for temporary const variable
             * <p>when need a new Literal, first refer to this map
             * to check if this literal is already in const Region</p>
             */
            private final Map<Literal, Var> constMap;

            public VisitorContext() {
                this.brkContMap = Maps.newMap();
                this.expStack = new Stack<>();
                this.labelGenerator = new BlockLabelGenerator();
                this.exceptionManager = new ExceptionEntryManager();
                this.contextCtrlList = new LinkedList<>();
                this.finallyBlockMap = Maps.newMap();
                this.finallyLabelMap = Maps.newMap();
                this.bindingVarMap = Maps.newMap();
                this.flowStack = new Stack<>();
                this.bm = new BindingManager();
                this.constMap = Maps.newMap();
            }

            public void pushStack(Exp exp) {
                expStack.add(exp);
            }

            public Exp popStack() {
                return expStack.pop();
            }

            public void assocLabel(String label) {
                getStmtManager().assocLabel(label);
            }

            public void pushFlow(boolean b) {
                this.flowStack.push(b);
            }

            public boolean popFlow() {
                return this.flowStack.pop();
            }

            public boolean popFlow(int n) {
                boolean res = false;
                for (var i = 0; i < n; ++i) {
                    res |= popFlow();
                }
                return res;
            }

            public void resolveFlow(int n) {
                pushFlow(popFlow(n));
            }

            /**
             * there may by no return statement in a method
             * it will cause some label detaching from ir
             * e.g.
             * <pre>
             * {@code
             * public void f(int x) {
             *     if (x > 0) {
             *        return;
             *     }
             *     +------------+ there is a [goto] pointed to here, but here contains nothing
             * }
             * }
             * </pre>
             * this function will add the return
             */
            private void confirmNoDetach() {
                if (popFlow()) {
                    getStmtManager().addStmt(-1, new Return());
                }
            }

            public String getNewLabel() {
                return labelGenerator.getNewLabel();
            }

            public String getNewFinLabel() {
                return labelGenerator.getNewFinLabel();
            }

            public void assocBreakAndContinue(String nowLabel, String breakLabel, String continueLabel) {
                this.brkContMap.put(nowLabel, new Pair<>(breakLabel, continueLabel));
            }

            public String getBreakLabel(String label) {
                var p = this.brkContMap.get(label);
                if (p != null) {
                    return p.first();
                } else {
                    throw new NewFrontendException("label: " + label + " don't exist in current context, illegal state");
                }
            }

            public String getContinueLabel(String label) {
                var p = this.brkContMap.get(label);
                if (p != null) {
                    return p.second();
                } else {
                    throw new NewFrontendException("label: " + label + " don't exist in current context, illegal state");
                }
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

            public ExceptionEntryManager getExceptionManager() {
                return this.exceptionManager;
            }

            public BlockLabelGenerator getLabelGenerator() {
                return this.labelGenerator;
            }

            public void handleUserLabel(String breakLabel, String contLabel) {
                if (!contextCtrlList.isEmpty()) {
                    if (BlockLabelGenerator.isUserDefinedLabel(this.contextCtrlList.getFirst())) {
                        assert (getStmtManager().assocList.contains(this.contextCtrlList.getFirst()));
                        assocBreakAndContinue(this.contextCtrlList.getFirst(), breakLabel, contLabel);
                    }
                }
            }


            public Map<IVariableBinding, Exp> getBindingVarMap() {
                return this.bindingVarMap;
            }

            public void putBinding(IVariableBinding binding, Var v) {
                bindingVarMap.put(binding, v);
            }

            public BindingManager getBm() {
                return bm;
            }

            public Map<Literal, Var> getConstMap() {
                return constMap;
            }
        }

        private int lineno;

        class LinenoASTVisitor extends ASTVisitor {

            protected final VisitorContext context;

            public LinenoASTVisitor(VisitorContext context) {
                this.context = context;
            }

            @Override
            public void preVisit(ASTNode node) {
                lineno = linenoManger.getLineno(node);
            }

        }

        public Var getInt(int i) {
            return expToVar(IntLiteral.get(i));
        }

        /**
         * Evaluate {@code expression} and push the result {@link Exp} to stack
         */
        protected void visitExp(Expression expression) {
            expression.accept(expVisitor);
        }

        protected void addStmt(Stmt stmt) {
            getStmtManager().addStmt(lineno, stmt);
        }

        protected void addConstStmt(Stmt stmt) {
            getStmtManager().addConst(stmt);
        }

        protected void addGoto(String label) {
            getStmtManager().addGoto(lineno, label);
        }

        protected void addIf(ConditionExp exp, String label) {
            getStmtManager().addIf(lineno, exp, label);
        }

        protected void addTableSwitch(Var v,
                                      int lowIdx,
                                      int highIdx,
                                      List<String> label,
                                      String defaultLabel) {
            getStmtManager().addTableSwitch(lineno, v, lowIdx, highIdx, label, defaultLabel);
        }

        protected void addLookupSwitch(Var v,
                                       List<Integer> values,
                                       List<String> label,
                                       String defaultLabel) {
            getStmtManager().addLookupSwitch(lineno, v, values, label, defaultLabel);
        }

        /**
         * some JDT method will always return a raw Type List
         *
         * @param exp an Expression List returned by some JDT method
         */
        protected Exp listCompute(List<Expression> exp, List<Type> types, Function<List<Var>, Exp> f) {
            List<Var> list = new ArrayList<>();
            for (var i = 0; i < exp.size(); ++i) {
                visitExp(exp.get(i));
                list.add(popVar(types.get(i)));
            }
            return f.apply(list);
        }

        protected Var genNewObject1(MethodRef init, List<Var> args, ClassType declClassType) {
            Var temp = newTempVar(declClassType);
            addStmt(new New(jMethod, temp, new NewInstance(declClassType)));
            assert args.size() == init.getParameterTypes().size();
            addNewInvokeSpecial(temp, init, args);
            return temp;
        }

        protected Exp genNewCast(Exp exp, Type t) {
            Var v = expToVar(exp);
            return new CastExp(v, t);
        }

        protected void addNewInvokeSpecial(Var v, MethodRef ref, List<Var> args) {
            InvokeSpecial special = new InvokeSpecial(ref, v, args);
            // perform type checking, check if args is compatible with ref
            for (int i = 0; i < args.size(); ++i) {
                Var arg = args.get(i);
                Type argType = arg.getType();
                Type refType = ref.getParameterTypes().get(i);
                if (!isSubTypeOf(refType, argType)) {
                    throw new NewFrontendException(
                            "type mismatch: " + argType + " is not subtype of " + refType);
                }
                try {
                    // can be statically checked
                    ref.resolve();
                } catch (Exception e) {
                    throw new NewFrontendException("can't resolve method: " + ref);
                }
            }
            addStmt(new Invoke(jMethod, special));
        }

        private List<Var> makeSynArgs(Expression enclosedInstance, ClassType declClassType) {
            String binaryName = declClassType.getName();
            List<Var> synArgs = new ArrayList<>();
            if (InnerClassManager.get().isInnerClass(binaryName)) {
                InnerClassDescriptor descriptor = InnerClassManager.get().getInnerClassDesc(binaryName);
                appendEnclosedInstance(enclosedInstance, synArgs, descriptor);
                appendCaptureSynArgs(synArgs, descriptor);
            }
            return synArgs;
        }

        private void appendEnclosedInstance(Expression enclosedInstance,
                                            List<Var> synArgs, InnerClassDescriptor descriptor) {
            if (! descriptor.isStatic()) {
                Var arg;
                ITypeBinding outerType = descriptor.synParaTypes().get(0);
                if (enclosedInstance != null) {
                    visitExp(enclosedInstance);
                    arg = popVar();
                } else {
                    arg = getOuterClassOrThis(outerType);
                }
                synArgs.add(arg);
            }
        }

        private void appendCaptureSynArgs(List<Var> synArgs, InnerClassDescriptor descriptor) {
            // find captured variable
            // maybe a field in this scope, e.g. local class in another local class
            int start = descriptor.isStatic() ? 0 : 1;
            for (int i = start; i < descriptor.synParaNames().size(); ++i) {
                String capName = descriptor.synParaNames().get(i);
                String origName = InnerClassManager.getOrigName(capName);
                IVariableBinding varBinding = descriptor.varBindingMap().get(origName);
                assert varBinding != null;
                Exp exp = getSimpleNameBinding(varBinding);
                Var arg = expToVar(exp);
                synArgs.add(arg);
            }
        }

        /**
         * See JLS17 ch 5.6, pp. 141 for detail
         *
         * @param contextChoose <p>0  for  numeric arithmetic context</p>
         *                      <p>1  for  numeric choice context</p>
         *                      <p>2  for  numeric array context</p>
         * @param operands      operands for Numeric Promotion
         * @return type that operands will promote to
         */
        protected Type resolveNumericPromotion(int contextChoose, List<Expression> operands) {
            var typeList = operands.stream()
                    .map(k -> getIndexOfPrimitive(JDTTypeToTaieType(k.resolveTypeBinding())))
                    .max(Integer::compareTo);
            assert typeList.isPresent();
            int maxType = typeList.get();
            if (maxType >= getIndexOfPrimitive(PrimitiveType.LONG)) {
                return getPrimitiveByIndex(maxType);
            } else if (contextChoose == 0 || contextChoose == 2) {
                return PrimitiveType.INT;
            } else {
                return getPrimitiveByIndex(maxType);
            }
        }

        private Exp unboxingConversion(Exp exp, ReferenceType expType, PrimitiveType targetType) {
            Var v = expToVar(exp, expType);
            Type t = getPrimitiveByRef(expType);
            return new InvokeVirtual(
                    getSimpleJREMethod(expType.getName(), t + "Value"),
                    v,
                    new ArrayList<>());
        }

        private Exp boxingConversion(Exp exp, PrimitiveType expType, ReferenceType targetType) {
            Var v = expToVar(exp, expType);
            ClassType boxedType = getClassByName(TypeUtils.getRefNameOfPrimitive(expType.getName()));
            assert targetType instanceof ClassType;
            // TODO: choose correct actions
            return new InvokeStatic(
                    getJREMethod(boxedType.getName(), "valueOf",
                            List.of(expType), boxedType),
                    List.of(v));
        }

        private Exp convertPrimitiveType(Exp exp, PrimitiveType expType, PrimitiveType targetType) {
            // JDT Should check correctness for us
            assert expType != targetType;
            if (getWidenType(expType).equals(targetType) ||
                    computeIntWiden(expType, targetType)) {
                return exp;
            } else {
                return genNewCast(exp, targetType);
            }
        }

        private Exp convertReferenceType(Exp exp, ReferenceType expType, ReferenceType targetType) {
            if (World.get().getTypeSystem().isSubtype(targetType, expType)) {
                return exp;
            } else {
                return genNewCast(exp, targetType);
            }
        }

        protected Exp preformTypeConversion(Exp exp, Type type) {
            if (exp instanceof Literal l && type instanceof PrimitiveType p) {
                return getLiteral(l, p);
            }

            Type expType = exp.getType();
            if (expType.equals(type)) {
                return exp;
            }
            // 1. if [type] and [expType] both reference type,
            //    then there's no need for type conversion
            if (expType instanceof ReferenceType r1 &&
                    type instanceof ReferenceType r2) {
                return convertReferenceType(exp, r1, r2);
            }
            // 2. if [type] is reference, and [expType] is primitive,
            //    then try to perform boxing conversion
            else if (expType instanceof PrimitiveType p
                    && type instanceof ReferenceType r) {
                return boxingConversion(exp, p, r);
            } else {
                // 3. if [type] is primitive, and [expType] is reference,
                //    then try to perform unboxing conversion
                if (expType instanceof ReferenceType r &&
                        type instanceof PrimitiveType p) {
                    exp = unboxingConversion(exp, r, p);
                    expType = exp.getType();
                    if (expType == p) {
                        return exp;
                    }
                }
                // 4. if [type] is primitive, and [expType] is primitive,
                //    then try to perform widening primitive conversion
                if (expType instanceof PrimitiveType e &&
                        type instanceof PrimitiveType t) {
                    return convertPrimitiveType(exp, e, t);
                }
                throw new NewFrontendException("illegal state, expType is " + expType + ", type is " + type);
            }
        }

        protected void newAssignment(LValue left, Exp right) {
            assert Utils.isAssignable(left.getType(), right.getType());
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
                } else if (right instanceof UnaryExp u) {
                    addStmt(new Unary(v, u));
                } else if (right instanceof InstanceOfExp i) {
                    addStmt(new InstanceOf(v, i));
                } else {
                    throw new NewFrontendException(right + " is not implemented");
                }
            } else if (left instanceof FieldAccess s) {
                addStmt(new StoreField(s, expToVar(right, left.getType())));
            } else if (left instanceof ArrayAccess a) {
                addStmt(new StoreArray(a, expToVar(right, left.getType())));
            } else {
                throw new NewFrontendException(left + " is not implemented");
            }
        }

        protected void handleAssignment(Expression lExp, Expression rExp) {
            visitExp(rExp);
            Exp r = context.popStack();
            LValue v;
            visitExp(lExp);
            v = popLValue();
            newAssignmentMayConversion(v, r);
            context.pushStack(v);
        }

        private void newAssignmentMayConversion(LValue lValue, Exp rValue) {
            Exp rValueConv = preformTypeConversion(rValue, lValue.getType());
            newAssignment(lValue, rValueConv);
        }

        protected Var expToVar(Exp exp, Type varType) {
            if (!varType.equals(exp.getType())) {
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
                    || exp instanceof CastExp
                    || exp instanceof InstanceOfExp
                    || exp instanceof UnaryExp) {
                var v = newTempVar(exp.getType());
                newAssignment(v, exp);
                return v;
            } else if (exp instanceof Literal l) {
                return context.getConstMap().computeIfAbsent(l, l1 -> {
                    var v = newTempConstantVar(l1);
                    addConstStmt(new AssignLiteral(v, l1));
                    return v;
                });
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
            String trueLabel = context.getNewLabel();
            String falseLabel = context.getNewLabel();
            String endLabel = context.getNewLabel();
            Type t = JDTTypeToTaieType(ce.resolveTypeBinding());
            Var v = newTempVar(t);
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
                cond = new ConditionExp(ConditionExp.Op.EQ, v, getInt(1));
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

        protected LValue popLValue() {
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
            return new Var[]{expToVar(v1, t1), expToVar(v2, t2)};
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

        protected void genBlock(List<Statement> l,
                                String label,
                                String continuation,
                                boolean needGoto) {
            context.assocLabel(label);
            genAnonBlock(l);
            boolean next = context.popFlow();
            if (next && needGoto) {
                addGoto(continuation);
            }
            context.pushFlow(next);
        }

        protected void genBlockWithRecording(List<Statement> l,
                                             String label,
                                             String continuation,
                                             boolean needGoto) {
            genBlockWithRecording(() -> genAnonBlock(l), label, continuation, needGoto);
        }

        protected void genBlockWithRecording(Runnable r,
                                             String label,
                                             String continuation,
                                             boolean needGoto) {
            context.getContextCtrlList().addFirst(label);
            context.getExceptionManager().beginRecording(label);
            context.assocLabel(label);
            r.run();
            context.getExceptionManager().endRecording(label);
            boolean next = context.popFlow();
            if (next && needGoto) {
                addGoto(continuation);
            }
            context.getContextCtrlList().removeFirst();
            context.pushFlow(next);
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

        protected InvokeExp genSimpleInterfaceInvoke(Var v,
                                                     String methodName,
                                                     List<Var> args,
                                                     List<Type> paraType,
                                                     Type retType) {
            ClassType t = (ClassType) v.getType();
            MethodRef r = MethodRef.get(t.getJClass(), methodName, paraType, retType, false);
            return new InvokeInterface(r, v, args);
        }

        protected InvokeExp genInterfaceInvoke(Var v,
                                               IMethodBinding binding,
                                               List<Var> args) {
            MethodRef r = getMethodRef(binding);
            return new InvokeInterface(r, v, args);
        }

        protected InvokeExp genEquals(Var v1, Var v2) {
            Type objType = getType(ClassNames.OBJECT);
            return genSimpleInvoke(v1, "equals",
                    List.of(v2), List.of(objType), PrimitiveType.BOOLEAN);
        }

        protected void genAnonBlock(List<Statement> stmts1) {
            boolean nextOuter = true;
            for (var i : stmts1) {
                visitStmt(i);
                boolean next = context.popFlow();
                nextOuter &= next;
            }
            context.pushFlow(nextOuter);
        }

        protected Exp getNewSimpleNameBinding(IVariableBinding binding) {
            // first, we check if it's a field
            if (binding.isField()) {
                if (Modifier.isStatic(binding.getModifiers())) {
                    // 1. if this variable is a static field
                    return getSimpleField(binding, null);
                } else if (isSuperClass(binding.getDeclaringClass())) {
                    // 2. this variable is an instance field, and this field belongs to this class instance
                    return getSimpleField(binding, getThisVar());
                } else {
                    // 3. if reach here, this variable must be an instance field of outer class
                    return getOuterClassField(binding, getThisVar());
                }
            } else {
                if (getTargetMethod() != null && isSameMethod(
                        getTargetMethod().resolveBinding(),
                        binding.getDeclaringMethod())) {
                    // 4. this is a variable defined in local scope
                    return newVar(binding.getName(), JDTTypeToTaieType(binding.getType()));
                } else {
                    // 5. this is a variable captured by local class
                    //    1) find where this variable is captured,
                    //       during this process, a series outer this loading is generated
                    //    2) push the captured variable field access to stack
                    //
                    //    note: no matter how this binding is captured,
                    //    by traversing the outer class, we will find the binding, or this is a bug
                    Exp directlyCapturedInstance = getOuterClassUntil(
                            (nowClass) -> {
                                InnerClassDescriptor desc1 =
                                        InnerClassManager.get().getInnerClassDesc(nowClass.getName());
                                assert desc1 != null;
                                return desc1.isDirectlyCaptured(binding);
                            },
                            getThisVar());
                    context.pushStack(directlyCapturedInstance);
                    Var v = popVar();
                    ClassType tCapClass = (ClassType) v.getType();
                    Type declType = JDTTypeToTaieType(binding.getType());
                    JClass klass = tCapClass.getJClass();
                    String capturedName = InnerClassManager.getCaptureName(binding.getName());
                    FieldRef ref = FieldRef.get(klass, capturedName, declType, false);
                    try {
                        ref.resolve();
                    } catch (Exception e) {
                        throw new NewFrontendException("failed to resolve capture field");
                    }
                    return new InstanceFieldAccess(ref, v);
                }
            }
        }

        /**
         * get the binding for [SimpleName]
         */
        protected Exp getSimpleNameBinding(IVariableBinding binding) {
            return context.getBindingVarMap().computeIfAbsent(binding,
                    this::getNewSimpleNameBinding);
        }

        protected FieldAccess getSimpleField(IVariableBinding binding, Var object) {
            ITypeBinding declClass = binding.getDeclaringClass();
            JClass jClass = getTaieClass(declClass);
            ITypeBinding fieldType = binding.getType();
            Type taieType = JDTTypeToTaieType(fieldType);
            boolean isStatic = Modifier.isStatic(binding.getModifiers());
            FieldRef ref = FieldRef.get(jClass, binding.getName(), taieType, isStatic);

            if (isStatic) {
                return new StaticFieldAccess(ref);
            } else {
                return new InstanceFieldAccess(ref, object);
            }
        }

        private Exp getOuterClassUntil(
                Function<JClass, Boolean> predicate, Var thisVar) {
            JClass nowClass = targetClass;
            FieldRef ref;
            context.pushStack(thisVar);
            while (nowClass != null && !predicate.apply(nowClass)){
                thisVar = popVar();
                ref = InnerClassManager.get().getOuterClassRef(nowClass);
                nowClass = nowClass.getOuterClass();
                context.pushStack(new InstanceFieldAccess(ref, thisVar));
            }
            // if nowClass is null, this top of the stack is an invalid field access
            if (nowClass == null) {
                throw new NewFrontendException("failed to resolve outer class");
            }
            return context.popStack();
        }

        protected FieldAccess getOuterClass(ITypeBinding outerClass, Var thisVar, boolean allowSuper) {
            JClass targetOuterClass = getTaieClass(outerClass);
            assert targetClass != null && targetOuterClass != null;
            return (InstanceFieldAccess) getOuterClassUntil((nowClass) -> {
                if (!allowSuper) {
                    return targetOuterClass.getType().equals(nowClass.getType());
                } else {
                    return isSubType(targetOuterClass.getType(), nowClass.getType());
                }
            }, thisVar);
        }

        protected FieldAccess getOuterClassField(IVariableBinding binding, Var thisVar) {
            return getSimpleField(binding, expToVar(
                    getOuterClass(binding.getDeclaringClass(), thisVar, true)));
        }

        protected Var getOuterClassOrThis(ITypeBinding type) {
            if (isTargetClass(type) || isSuperClass(type)) {
                return getThisVar();
            } else {
                return expToVar(getOuterClass(type, getThisVar(), true));
            }
        }

        public Pair<List<Type>, List<Expression>> makeParamAndArgs(IMethodBinding binding, List<Expression> args) {
            if (binding.isVarargs()) {
                ITypeBinding[] types = binding.getParameterTypes();
                ITypeBinding varargType = types[types.length - 1];
                Type realType = JDTTypeToTaieType(varargType);
                int i = 0;
                List<Type> params = new ArrayList<>();
                for (; i < types.length - 1; ++i) {
                    params.add(JDTTypeToTaieType(types[i]));
                }
                params.add(realType);
                ArrayCreation ac;
                if (i < args.size()) {
                    AST ast = args.get(i).getAST();
                    List<Expression> varArgs = args.subList(i, args.size());
                    ac = makeArrCreate(ast, varArgs, realType);
                } else {
                    // no more args, make an empty array
                    ac = makeEmptyArrCreate(realType);
                }
                List<Expression> beforeArgs = args.subList(0, i);
                return new Pair<>(
                        params,
                        Stream.concat(beforeArgs.stream(),
                                        Stream.of(ac))
                                .toList());
            } else {
                return new Pair<>(
                        Arrays.stream(binding.getParameterTypes())
                                .map(TypeUtils::JDTTypeToTaieType)
                                .toList(),
                        args);
            }
        }

        private Exp makeInvoke(@Nullable Expression object, IMethodBinding binding, List<Expression> args,
                               boolean isSuper) {
            IMethodBinding decl = binding.getMethodDeclaration();
            assert decl != null;
            MethodRef ref = getMethodRef(decl);
            return makeInvoke(object, binding, args, ref, isSuper);
        }

        private Exp makeInvoke(@Nullable Expression object, IMethodBinding binding, List<Expression> args) {
            return makeInvoke(object, binding, args, false);
        }

        public Exp makeInvoke(@Nullable Expression object,
                              IMethodBinding binding,
                              List<Expression> args,
                              MethodRef ref,
                              boolean isSuper) {
            // <init> should not be handled by this function
            assert ! ref.getName().equals(MethodNames.INIT);
            IMethodBinding decl = binding.getMethodDeclaration();
            int modifier = decl.getModifiers();
            Exp exp;
            var paramsAndArgs = makeParamAndArgs(binding, args);
            List<Type> paramsType = paramsAndArgs.first();
            args = paramsAndArgs.second();
            // 1. if this method is [static], that means it has nothing to do with [object]
            //    JDT has resolved the binding of method
            if (Modifier.isStatic(modifier)) {
                exp = listCompute(args, paramsType, (l) -> new InvokeStatic(ref, l));
                return exp;
            }
            // here, if [object] is [null], we can confirm it's a call to [this]
            Var o;
            boolean checkInterface = binding.getDeclaringClass().isInterface();
            if (object == null) {
                o = getOuterClassOrThis(binding.getDeclaringClass());
            } else {
                object.accept(expVisitor);
                o = popVar();
            }
            // 2. if the type [object] is [Interface], then use [InvokeInterface]
            // TODO: implement [JEP 181](https://openjdk.java.net/jeps/181)
            // TODO: check [ArrayType::clone, ArrayType::length]
            if (checkInterface) {
                exp = listCompute(args, paramsType, l -> new InvokeInterface(ref, o, l));
            }
            // 3. if this method is [private]
            //    then use [InvokeSpecial]
            else if (Modifier.isPrivate(modifier) || isSuper) {
                exp = listCompute(args, paramsType, l -> new InvokeSpecial(ref, o, l));
            }
            // 4. otherwise, use [InvokeVirtual]
            else {
                exp = listCompute(args, paramsType, l -> new InvokeVirtual(ref, o, l));
            }
            return exp;
        }

        private InvokeInstanceExp getInitInvoke(Var target, List<Var> synArgs, List<Expression> args,
                                                IMethodBinding binding, MethodRef ref) {
            var paramsAndArgs = makeParamAndArgs(binding, args);
            List<Type> paramsType = paramsAndArgs.first();
            args = paramsAndArgs.second();
            return (InvokeInstanceExp)
                    listCompute(args, paramsType,
                            l -> new InvokeSpecial(ref, target, addList(synArgs, l)));
        }

        private InvokeInstanceExp getSuperInitInvoke(Expression enclosedInstance,
                                                     IMethodBinding binding,
                                                     List<Expression> args) {
            ITypeBinding declaringClass = binding.getDeclaringClass();
            ClassType type = (ClassType) JDTTypeToTaieType(declaringClass);
            List<Var> synArgs = makeSynArgs(enclosedInstance, type);
            return getInitInvoke(getThisVar(), synArgs, args,
                    binding, getInitRef(binding));
        }

        @SuppressWarnings("unchecked")
        private ArrayCreation makeArrCreate(AST ast, List<Expression> exps, Type arrType) {
            ArrayCreation ac = ast.newArrayCreation();
            ArrayInitializer ai = ast.newArrayInitializer();
            exps.stream().map(i -> context.getBm().copyNode(ast, i)).forEach(ai.expressions()::add);
            ac.setInitializer(ai);
            context.getBm().setType(ac, arrType);
            context.getBm().setType(ai, arrType);
            return ac;
        }

        @SuppressWarnings("unchecked")
        private ArrayCreation makeEmptyArrCreate(Type arrType) {
            ArrayCreation ac = rootAst.newArrayCreation();
            ac.dimensions().add(rootAst.newNumberLiteral("0"));
            context.getBm().setType(ac, arrType);
            return ac;
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
                genAnonBlock(stmts);
                return false;
            }

            public boolean needToProccessLabel(String i) {
               return (BlockLabelGenerator.isTryBlock(i) || BlockLabelGenerator.isCatchBlock(i)) &&
                       ! context.getExceptionManager().pausedList.contains(i);
            }

            private void genAllFinBlock(Runnable r) {
                boolean next = true;
                for (var i : context.getContextCtrlList()) {
                    if (needToProccessLabel(i)) {
                        Block fin = context.getFinallyBlockBy(i);
                        context.getExceptionManager().pauseRecording(i);
                        if (fin != null) {
                            fin.accept(this);
                            next &= context.popFlow();
                        }
                    }
                }
                if (next) {
                    r.run();
                }
                context.getExceptionManager().continueRecordAll();
            }

            @Override
            public boolean visit(ReturnStatement rs) {
                var exp = rs.getExpression();
                if (exp != null) {
                    genAllFinBlock(() -> {
                        visitExp(exp);
                        Var retVar = popVar(getReturnType());
                        addRetVar(retVar);
                        addStmt(new Return(retVar));
                    });
                } else {
                    genAllFinBlock(() -> addStmt(new Return()));
                }
                context.pushFlow(false);
                return false;
            }

            /**
             * If encounter here, then {@code ass} is a Expression Statement
             * e.g. {@code x = 10};
             */
            @Override
            public boolean visit(Assignment ass) {
                // let ExpVisitor handle this assignment
                visitExp(ass);
                // ignore the value of this assignment
                popSideEffect();
                context.pushFlow(true);
                return false;
            }

            @Override
            public boolean visit(SingleVariableDeclaration svd) {
                handleSingleVarDecl(svd);
                context.pushFlow(true);
                return false;
            }

            @Override
            public boolean visit(VariableDeclarationStatement vds) {
                var singleDecls = vds.fragments();
                for (var i : singleDecls) {
                    var singleDecl = (VariableDeclarationFragment) i;
                    handleSingleVarDecl(singleDecl);
                }
                context.pushFlow(true);
                return false;
            }

            // Generate code like :
            //            +--------------+
            //            |    Branch    |------+
            //            +--------------+      |
            //      +-----|  goto False  |      |
            //      |     +--------------+<-----+
            //      |     |    True      |
            //      |     +--------------+
            //      |     |  goto Next   |------+
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
                genBlock(List.of(thenStmt), trueLabel, endLabel, elseStmt != null);
                // note: when second branch don't exist,
                // we always concern this [if] to be able to complete normally
                genBlock(elseStmt == null ? List.of() : List.of(elseStmt), falseLabel, endLabel, false);
                context.assocLabel(endLabel);
                context.resolveFlow(elseStmt == null ? 1 : 2);
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
            //      |     |  goto Branch |----------+
            //      +---->+--------------+            (Break target)
            //            |    Break     |
            //            +--------------+
            //
            private void genNormalLoop(List<Expression> inits,
                                       Expression cond,
                                       Statement body,
                                       List<Expression> updates) {
                Runnable genInit = () -> {
                    for (var i : inits) {
                        visitExp(i);
                        popSideEffect();
                    }
                };
                BiConsumer<String, String> genCond = (bodyLabel, breakLabel) ->
                        transformCond(cond, bodyLabel, breakLabel, true);
                Runnable genBody = () -> body.accept(this);
                Runnable genUpdates = () -> {
                    for (var i : updates) {
                        visitExp(i);
                        popSideEffect();
                    }
                };
                genNormalLoop(genInit, genCond, genBody, genUpdates);
            }

            private void genNormalLoop(Runnable genInit,
                                       BiConsumer<String, String> genCond,
                                       Runnable genBody,
                                       Runnable genUpdates) {
                var contLabel = context.getNewLabel();
                var breakLabel = context.getNewLabel();
                var bodyLabel = context.getNewLabel();
                context.handleUserLabel(breakLabel, contLabel);
                genInit.run();
                context.assocLabel(contLabel);
                context.getContextCtrlList().addFirst(contLabel);
                context.assocBreakAndContinue(contLabel, breakLabel, contLabel);
                genCond.accept(bodyLabel, breakLabel);

                context.assocLabel(bodyLabel);
                genBody.run();
                // ignore this, we don't care about if loop body can fallthrough or not
                context.popFlow();
                context.getContextCtrlList().removeFirst();

                genUpdates.run();
                addGoto(contLabel);
                context.assocLabel(breakLabel);
                // note: we consider a loop to always reach next
                context.pushFlow(true);
            }

            @Override
            public boolean visit(WhileStatement ws) {
                genNormalLoop(List.of(), ws.getExpression(), ws.getBody(), List.of());
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
                context.popFlow();
                context.pushFlow(true);
                return false;
            }

            @Override
            public boolean visit(EnhancedForStatement efs) {
                SingleVariableDeclaration svd = efs.getParameter();
                SimpleName id = svd.getName();
                visitExp(id);
                Var idVar = popVar();
                visitExp(efs.getExpression());
                Exp exp = context.popStack();
                Var expVar = expToVar(exp);
                Statement body = efs.getBody();

                // see JLS7 pp. 391 to get transformation detail
                if (!efs.getExpression().resolveTypeBinding().isArray()) {
                    var preEleType = getIterableInner(efs.getExpression().resolveTypeBinding());
                    if (preEleType.isEmpty()) {
                        throw new NewFrontendException(exp + " is not array nor <: Iterable");
                    }
                    Type eleType = preEleType.get();
                    IMethodBinding iteratorMethod = searchMethod(efs.getExpression().resolveTypeBinding(), ITERATOR);
                    Type iteratorType = JDTTypeToTaieType(iteratorMethod.getReturnType());
                    Var v = newTempVar(iteratorType);

                    Runnable genInit = () -> {
                        Exp iter = genInterfaceInvoke(expVar,
                                iteratorMethod,
                                List.of());
                        newAssignment(v, iter);
                    };

                    BiConsumer<String, String> genCond = (bodyLabel, breakLabel) -> {
                        Exp hasNext = genSimpleInterfaceInvoke(v, HAS_NEXT,
                                List.of(), List.of(), PrimitiveType.BOOLEAN);
                        context.pushStack(hasNext);
                        popControlFlow(bodyLabel, breakLabel, true);
                    };

                    Runnable genBody = () -> {
                        Exp next = genSimpleInterfaceInvoke(v, "next",
                                List.of(), List.of(), getType(ClassNames.OBJECT));
                        context.pushStack(next);
                        Exp ass = expToVar(preformTypeConversion(popVar(), eleType), idVar.getType());
                        newAssignment(idVar, ass);
                        body.accept(this);
                    };

                    genNormalLoop(genInit, genCond, genBody, () -> {});

                } else {
                    assert expVar.getType() instanceof pascal.taie.language.type.ArrayType;
                    Var v = newTempVar(PrimitiveType.INT);
                    Runnable genInit = () -> newAssignment(v, IntLiteral.get(0));

                    BiConsumer<String, String> genCond = (bodyLabel, breakLabel) -> {
                        context.pushStack(new ArrayLengthExp(expVar));
                        context.pushStack(new ConditionExp(ConditionExp.Op.LT, v, popVar()));
                        popControlFlow(bodyLabel, breakLabel, true);
                    };

                    Runnable genBody = () -> {
                        Exp now = new ArrayAccess(expVar, v);
                        Exp ass = expToVar(now, idVar.getType());
                        newAssignment(idVar, ass);
                        body.accept(this);
                    };

                    Runnable genUpdates = () -> newAssignment(v, new ArithmeticExp(ArithmeticExp.Op.ADD, v, getInt(1)));

                    genNormalLoop(genInit, genCond, genBody, genUpdates);
                }
                return false;
            }

            /**
             * Generate break/continue statement.
             * Because of the existence of try-catch-finally,
             * this need to be carefully handled
             *
             * @param label     may be null, break/continue label
             * @param brkOrCont 0 for break, 1 for continue
             */
            @SuppressWarnings("unchecked")
            private void genContBrk(@Nullable SimpleName label, int brkOrCont) {
                for (var i : context.getContextCtrlList()) {
                    if (needToProccessLabel(i)) {
                        // First, pause the record of this [Try]/[Catch] Block
                        // Because the code to be generated is not belong to this [Try]/[Catch] Block
                        // It's belong to the [Finally] of the [Try]/[Catch] Block
                        context.getExceptionManager().pauseRecording(i);
                        // Generate this [Finally] Block
                        Block fin = context.getFinallyBlockBy(i);
                        if (fin != null) {
                            genAnonBlock(fin.statements());
                            context.popFlow();
                        }
                    } else {
                        if (label == null) {
                            // if [label] is null, then we have got the innermost loop block, generate goto here
                            if (brkOrCont == 0) {
                                addGoto(context.getBreakLabel(i));
                            } else {
                                addGoto(context.getContinueLabel(i));
                            }
                            context.pushFlow(false);
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
                            context.pushFlow(false);
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

            private void genFinallyBlock(Runnable genStmts,
                                         String finLabel1,
                                         String finLabel2,
                                         String tryLabel,
                                         List<String> catchLabels) {
                // add exception handler
                // ([Try Begin] [Try End])ₙ [Finally-Catch] [Any]
                context.getExceptionManager().registerHandler(tryLabel, TypeUtils.anyException(), finLabel1);
                // add exception handler
                // ([Catch Begin] [Catch End])ₙ [Finally-Catch] [Any]
                for (var i : catchLabels) {
                    context.getExceptionManager().registerHandler(i, TypeUtils.anyException(), finLabel1);
                }

                // generate finLabel1 block
                context.assocLabel(finLabel1);
                Var e = newTempVar(TypeUtils.anyException());
                addStmt(new Catch(e));
                genStmts.run();
                addStmt(new Throw(e));

                // generate finLabel2 block
                context.assocLabel(finLabel2);
                genStmts.run();
            }

            @SuppressWarnings("unchecked")
            private String handleCatch(CatchClause cc, String tryLabel, String finLabel2) {
                String nowLabel = context.labelGenerator.getNewCatchLabel();
                SingleVariableDeclaration decl = cc.getException();
                Block body = cc.getBody();
                ITypeBinding exceptionType = decl.getType().resolveBinding();
                Type taieType = TypeUtils.JDTTypeToTaieType(exceptionType);
                visitExp(decl.getName());
                Runnable r = () -> {
                    addStmt(new Catch(popVar()));
                    genAnonBlock(body.statements());
                };
                genBlockWithRecording(r, nowLabel, finLabel2, true);
                // add exception handler
                // ([Try Begin] [Try End])ₙ [Catch Begin] [Type]
                context.getExceptionManager().registerHandler(tryLabel, taieType, nowLabel);
                context.popFlow();
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
            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(TryStatement ts) {
                Block tryBlock = ts.getBody();
                List<CatchClause> catches = ts.catchClauses();
                Block finBlock = ts.getFinally();
                String tryLabel = context.labelGenerator.getNewTryLabel();
                String finLabel1 = context.getNewFinLabel();
                String finLabel2 = context.getNewFinLabel();
                context.assocFinallyLabel(tryLabel, finLabel2);

                if (finBlock != null) {
                    context.assocFinallyBlock(finLabel2, finBlock);
                }

                // generate try block
                genBlockWithRecording(tryBlock.statements(), tryLabel, finLabel2, true);

                List<String> catchLabel = new ArrayList<>();
                for (CatchClause i : catches) {
                    var l = handleCatch(i, tryLabel, finLabel2);
                    catchLabel.add(l);
                }

                if (finBlock != null) {
                    genFinallyBlock(() -> {
                                finBlock.accept(this);
                                context.popFlow();
                            },
                            finLabel1,
                            finLabel2,
                            tryLabel,
                            catchLabel);
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
                context.pushFlow(false);
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
            //        |  Default       |      |
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
                // TODO: may be not correct for fallthrough
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
                    while (s instanceof SwitchCase sc) {
                        if (sc.isDefault()) {
                            defaultLabel = nowLabel;
                        } else {
                            for (var exp : sc.expressions()) {
                                caseExps.add((Expression) exp);
                                targets.add(nowLabel);
                            }
                        }
                        if (!i.hasNext()) {
                            break;
                        }
                        s = (Statement) i.next();
                    }
                    List<Statement> nowStmt = new ArrayList<>();
                    while (!(s instanceof SwitchCase)) {
                        nowStmt.add(s);
                        if (!i.hasNext()) {
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
                int n = defaultStmts.size();
                blocks.remove(defaultLabel);
                genBlock(defaultStmts, defaultLabel, end, true);

                blocks.forEach((label, stmts) -> genBlock(stmts, label, end, true));

                context.resolveFlow(n);
                context.getContextCtrlList().removeFirst();
                context.assocLabel(end);
                return false;
            }

            @Override
            public boolean visit(ExpressionStatement es) {
                Expression exp = es.getExpression();
                visitExp(exp);
                popSideEffect();
                context.pushFlow(true);
                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(SynchronizedStatement ss) {
                Block body = ss.getBody();
                Expression exp = ss.getExpression();
                visitExp(exp);
                Var v = popVar();

                String tryLabel = context.getLabelGenerator().getNewTryLabel();
                String finLabel1 = context.getLabelGenerator().getNewFinLabel();
                String finLabel2 = context.getLabelGenerator().getNewFinLabel();

                addStmt(new Monitor(Monitor.Op.ENTER, v));
                genBlockWithRecording(body.statements(), tryLabel, finLabel2, true);
                Runnable genFin = () -> addStmt(new Monitor(Monitor.Op.EXIT, v));

                genFinallyBlock(genFin, finLabel1, finLabel2, tryLabel, List.of());
                return false;
            }

            @Override
            public boolean visit(TypeDeclaration typeDeclaration) {
                context.pushFlow(true);
                return false;
            }

            @Override
            public boolean visit(EnumDeclaration enumDeclaration) {
                context.pushFlow(true);
                return false;
            }

            @Override
            public boolean visit(AnnotationTypeDeclaration annotationTypeDeclaration) {
                context.pushFlow(true);
                return false;
            }

            @Override
            public boolean visit(AnonymousClassDeclaration anonymousClassDeclaration) {
                context.pushFlow(true);
                return false;
            }
        }

        class ExpVisitor extends LinenoASTVisitor {

            ExpVisitor(VisitorContext context) {
                super(context);
            }

            private void numericBinaryCompute(InfixExpression exp, Type t1, Type t2, BiFunction<Var, Var, Exp> f) {
                exp.getLeftOperand().accept(this);
                var lVar = popVar(t1);
                exp.getRightOperand().accept(this);
                var rVar = popVar(t2);
                context.pushStack(f.apply(lVar, rVar));
                var extOperands = exp.extendedOperands();
                for (var i : extOperands) {
                    var expNow = (Expression) i;
                    expNow.accept(this);
                    var lrVarNow = popVar2(t1, t2);
                    context.pushStack(f.apply(lrVarNow[0], lrVarNow[1]));
                }
            }

            private void binaryCompute2(InfixExpression exp, BiFunction<Var, Var, Exp> f) {
                Expression lExp = exp.getLeftOperand();
                Expression rExp = exp.getRightOperand();
                if (lExp.resolveTypeBinding().isPrimitive() ||
                        rExp.resolveTypeBinding().isPrimitive()) {
                    Type t = resolveNumericPromotion(0, List.of(lExp, rExp));
                    lExp.accept(this);
                    var lVar = popVar(t);
                    rExp.accept(this);
                    var rVar = popVar(t);
                    context.pushStack(f.apply(lVar, rVar));
                } else {
                    lExp.accept(this);
                    var lVar = popVar();
                    rExp.accept(this);
                    var rVar = popVar();
                    context.pushStack(f.apply(lVar, rVar));
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
                Var sbVar = genNewObject1(newSb, List.of(), sb);
                genStringBuilderAppend(sbVar, getAllOperands(exp));
                context.pushStack(new InvokeVirtual(TypeUtils.getToString(), sbVar, new ArrayList<>()));
            }

            // Ref: JLS17 6.5.6.1
            @Override
            public boolean visit(SimpleName name) {
                var binding = name.resolveBinding();
                if (binding instanceof IVariableBinding binding1) {
                    context.pushStack(getSimpleNameBinding(binding1));
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
                    context.pushStack(getSimpleField((IVariableBinding) name1.resolveBinding(), null));
                } else if (q instanceof IVariableBinding) {
                    qualifier.accept(this);
                    if (qualifier.resolveTypeBinding().isArray()) {
                        context.pushStack(new ArrayLengthExp(popVar()));
                    } else {
                        context.pushStack(getSimpleField((IVariableBinding) name1.resolveBinding(), popVar()));
                    }
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
                        Type t = JDTTypeToTaieType(exp.resolveTypeBinding());
                        numericBinaryCompute(exp, t, t, (l, r) ->
                                new ArithmeticExp(TypeUtils.getArithmeticOp(op), l, r));
                        return false;
                    }
                    case "<<", ">>", ">>>" -> {
                        Type leftType = resolveNumericPromotion(0,
                                List.of(exp.getLeftOperand()));
                        Type rightType = PrimitiveType.INT;
                        numericBinaryCompute(exp, leftType, rightType, (l, r) ->
                                new ShiftExp(TypeUtils.getShiftOp(op), l, r));
                        return false;
                    }
                    case "|", "^", "&" -> {
                        Type t = JDTTypeToTaieType(exp.resolveTypeBinding());
                        numericBinaryCompute(exp, t, t, (l, r) ->
                                new BitwiseExp(TypeUtils.getBitwiseOp(op), l, r));
                        return false;
                    }
                    case ">", ">=", "<=", "<" -> {
                        Type t = resolveNumericPromotion(0, getAllOperands(exp));
                        numericBinaryCompute(exp, t, t, (l, r) -> {
                            assert t instanceof PrimitiveType;
                            if (t == PrimitiveType.FLOAT || t == PrimitiveType.DOUBLE) {
                                ComparisonExp.Op cmpOp;
                                ConditionExp.Op condOp;
                                Var condOperand2 = getInt(0);
                                switch (opStr) {
                                    case ">":
                                        cmpOp = ComparisonExp.Op.CMPL;
                                        condOp = ConditionExp.Op.GT; break;
                                    case ">=":
                                        cmpOp = ComparisonExp.Op.CMPL;
                                        condOp = ConditionExp.Op.GE; break;
                                    case "<":
                                        cmpOp = ComparisonExp.Op.CMPG;
                                        condOp = ConditionExp.Op.LT; break;
                                    case "<=":
                                        cmpOp = ComparisonExp.Op.CMPG;
                                        condOp = ConditionExp.Op.LE; break;
                                    default: throw new UnsupportedOperationException();
                                }
                                context.pushStack(new ComparisonExp(cmpOp, l, r));
                                Var condOperand1 = popVar();
                                return new ConditionExp(condOp, condOperand1, condOperand2);
                            } else {
                                return new ConditionExp(TypeUtils.getConditionOp(op), l, r);
                            }
                        });
                        return false;
                    }
                    case "==", "!=" -> {
                        binaryCompute2(exp, (l, r) -> {
                            Type t = l.getType();
                            if (t == PrimitiveType.FLOAT || t == PrimitiveType.DOUBLE) {
                                ComparisonExp.Op cmpOp = ComparisonExp.Op.CMPL;
                                ConditionExp.Op condOp = TypeUtils.getConditionOp(op);
                                Var condOperand2 = getInt(0);
                                context.pushStack(new ComparisonExp(cmpOp, l, r));
                                Var condOperand1 = popVar();
                                return new ConditionExp(condOp, condOperand1, condOperand2);
                            } else {
                                return new ConditionExp(TypeUtils.getConditionOp(op), l, r);
                            }
                        });
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

            void genOtherAssign(Assignment ass, BiFunction<Var, Var, Exp> f) {
                ass.getLeftHandSide().accept(this);
                LValue l = popLValue();
                ass.getRightHandSide().accept(this);
                Var v1 = expToVar(l);
                Var v2 = popVar(JDTTypeToTaieType(ass.resolveTypeBinding()));
                newAssignment(l, f.apply(v1, v2));
                context.pushStack(l);
            }

            void genOtherAssign(Assignment ass) {
                switch (ass.getOperator().toString()) {
                    case "+=" -> genOtherAssign(ass, (v1, v2) ->
                            new ArithmeticExp(ArithmeticExp.Op.ADD, v1, v2));
                    case "-=" -> genOtherAssign(ass, (v1, v2) ->
                            new ArithmeticExp(ArithmeticExp.Op.SUB, v1, v2));
                    case "*=" -> genOtherAssign(ass, (v1, v2) ->
                            new ArithmeticExp(ArithmeticExp.Op.MUL, v1, v2));
                    case "/=" -> genOtherAssign(ass, (v1, v2) ->
                            new ArithmeticExp(ArithmeticExp.Op.DIV, v1, v2));
                    case "&=" -> genOtherAssign(ass, (v1, v2) ->
                            new BitwiseExp(BitwiseExp.Op.AND, v1, v2));
                    case "|=" -> genOtherAssign(ass, (v1, v2) ->
                            new BitwiseExp(BitwiseExp.Op.OR, v1, v2));
                    case "^=" -> genOtherAssign(ass, (v1, v2) ->
                            new BitwiseExp(BitwiseExp.Op.XOR, v1, v2));
                    case "%=" -> genOtherAssign(ass, (v1, v2) ->
                            new ArithmeticExp(ArithmeticExp.Op.REM, v1, v2));
                    case ">>=" -> genOtherAssign(ass, (v1, v2) ->
                            new ShiftExp(ShiftExp.Op.SHR, v1, v2));
                    case "<<=" -> genOtherAssign(ass, (v1, v2) ->
                            new ShiftExp(ShiftExp.Op.SHL, v1, v2));
                    case ">>>=" -> genOtherAssign(ass, (v1, v2) ->
                            new ShiftExp(ShiftExp.Op.USHR, v1, v2));
                }
            }

            @Override
            public boolean visit(Assignment ass) {
                var lExp = ass.getLeftHandSide();
                var rExp = ass.getRightHandSide();
                // if left-hand side is a SimpleName, and right-hand side is a literal
                // tai-e ir need to put literal into result var
                // so just visit left will not output the correct ir
                // this need to be carefully handled
                if ("=".equals(ass.getOperator().toString())) {
                    handleAssignment(lExp, rExp);
                } else {
                    genOtherAssign(ass);
                }
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

            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(MethodInvocation mi) {
                Expression object = mi.getExpression();
                IMethodBinding binding = mi.resolveMethodBinding();
                List<Expression> l = mi.arguments();
                Exp invoke = makeInvoke(object, binding, l);
                context.pushStack(invoke);
                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(SuperMethodInvocation smi) {
                Exp invoke = makeInvoke(null, smi.resolveMethodBinding(), smi.arguments(), true);
                context.pushStack(invoke);
                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(ClassInstanceCreation cic) {
                ITypeBinding binding = cic.resolveTypeBinding();
                Type type = TypeUtils.JDTTypeToTaieType(binding);
                IMethodBinding methodBinding = cic.resolveConstructorBinding();
                MethodRef init = getInitRef(methodBinding);
                Expression exp = cic.getExpression();
                List<Expression> args = cic.arguments();
                boolean isAnonymous = cic.getAnonymousClassDeclaration() != null;
                ClassType classType = (ClassType) type;
                // if this is anonymous, we first gen the `this$0`
                // if `exp` is not null, we then gen the `this$1`
                List<Var> synArgs = makeSynArgs(isAnonymous ? null : exp, classType);
                if (exp != null && isAnonymous) {
                    // gen `this$1`
                    exp.accept(this);
                    synArgs.add(popVar());
                }
                Pair<List<Type>, List<Expression>> argTypes = makeParamAndArgs(methodBinding, args);
                assert argTypes.second().size() + synArgs.size() == init.getParameterTypes().size();
                Var temp = (Var) listCompute(argTypes.second(), argTypes.first(),
                        l -> genNewObject1(init, addList(synArgs, l), classType));
                context.pushStack(temp);
                return false;
            }

            private void initArr(Expression exp, LValue access) {
                // note: exp may be generated, so use [getRealNode] to get the node with binding
                exp = (Expression) context.getBm().getRealNode(exp);
                if (exp instanceof ArrayInitializer init) {
                    var exps = init.expressions();
                    boolean isOneDim;
                    if (init.expressions().isEmpty()) {
                        isOneDim = true;
                    } else {
                        isOneDim = !(init.expressions().get(0) instanceof ArrayInitializer);
                    }
                    var type = (pascal.taie.language.type.ArrayType) context.getBm().getTypeOfExp(exp);
                    Var length = getInt(exps.size());
                    List<Var> lengths = new ArrayList<>();
                    lengths.add(length);
                    newAssignment(access, isOneDim ? new NewArray(type, length) :
                            new NewMultiArray(type, lengths));
                    for (var i = 0; i < exps.size(); ++i) {
                        context.pushStack(access);
                        initArr((Expression) exps.get(i),
                                new ArrayAccess(popVar(), getInt(i)));
                    }
                } else {
                    exp.accept(this);
                    newAssignment(access, popVar(access.getType()));
                }
            }

            private void handleArrInit(ArrayInitializer init) {
                var type = (pascal.taie.language.type.ArrayType) context.getBm().getTypeOfExp(init);
                Var temp = newTempVar(type);
                initArr(init, temp);
                context.pushStack(temp);
            }

            // Tai-e IR can represent both fix-sized and var-sized multiArray
            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(ArrayCreation ac) {
                // ac may be generated
                var taieType = (pascal.taie.language.type.ArrayType) context.getBm().getTypeOfExp(ac);
                ArrayInitializer arrInit = ac.getInitializer();
                if (taieType.dimensions() == 1 && arrInit == null) {
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
                context.pushStack(preformTypeConversion(exp,
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


            /**
             * this function only handles (++) (--)
             */
            private void genPrefixExp(Expression operand, ArithmeticExp.Op op, ITypeBinding t) {
                operand.accept(this);
                LValue l = popLValue();
                Type type = JDTTypeToTaieType(t);
                Var v = expToVar(l, type);
                newAssignment(l, new ArithmeticExp(op, v, getInt(1)));
                context.pushStack(v);
            }

            @Override
            public boolean visit(PrefixExpression pe) {
                switch (pe.getOperator().toString()) {
                    case "!" -> collectBoolEval(pe);
                    case "++" -> genPrefixExp(pe.getOperand(), ArithmeticExp.Op.ADD, pe.resolveTypeBinding());
                    case "--" -> genPrefixExp(pe.getOperand(), ArithmeticExp.Op.SUB, pe.resolveTypeBinding());
                    case "~" -> {
                        // JLS7 pp. 499: In all cases, ~x equals (-x)-1
                        pe.getOperand().accept(this);
                        Var v = popVar(JDTTypeToTaieType(pe.resolveTypeBinding()));
                        context.pushStack(new ArithmeticExp(ArithmeticExp.Op.SUB, v, getInt(1)));
                    }
                    case "+" -> // in this case, there's nothing to do
                            pe.getOperand().accept(this);
                    case "-" -> {
                        pe.getOperand().accept(this);
                        Var v = popVar(JDTTypeToTaieType(pe.resolveTypeBinding()));
                        context.pushStack(new NegExp(v));
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

            private void genPostExp(Expression operand, ArithmeticExp.Op op, ITypeBinding t) {
                operand.accept(this);
                // operand must be an Expression Name
                LValue l = popLValue();
                Type type = JDTTypeToTaieType(t);
                Var v = newTempVar(type);
                newAssignment(v, l);
                newAssignment(l, new ArithmeticExp(op, expToVar(l), getInt(1)));
                context.pushStack(v);
            }

            @Override
            public boolean visit(PostfixExpression pe) {
                switch (pe.getOperator().toString()) {
                    case "++" -> genPostExp(pe.getOperand(), ArithmeticExp.Op.ADD, pe.resolveTypeBinding());
                    case "--" -> genPostExp(pe.getOperand(), ArithmeticExp.Op.SUB, pe.resolveTypeBinding());
                }
                return false;
            }

            @Override
            public boolean visit(org.eclipse.jdt.core.dom.FieldAccess fa) {
                fa.getExpression().accept(this);
                Var obj = popVar();
                if (obj.getType() instanceof pascal.taie.language.type.ArrayType) {
                    context.pushStack(new ArrayLengthExp(obj));
                } else {
                    FieldAccess res = getSimpleField(fa.resolveFieldBinding(), obj);
                    context.pushStack(res);
                }
                return false;
            }

            @Override
            public boolean visit(SuperFieldAccess sfa) {
                IVariableBinding binding = sfa.resolveFieldBinding();
                JClass c = getTaieClass(binding.getDeclaringClass());
                assert c != null;
                JClass superClass = c.getSuperClass();
                FieldRef ref = FieldRef.get(superClass, binding.getName(),
                        JDTTypeToTaieType(binding.getType()), false);
                FieldAccess res = new InstanceFieldAccess(ref, getThisVar());
                context.pushStack(res);
                return false;
            }

            @Override
            public boolean visit(ThisExpression te) {
                if (te.getQualifier() == null) {
                    context.pushStack(getThisVar());
                } else {
                    context.pushStack(getOuterClass(
                            te.getQualifier().resolveTypeBinding(), getThisVar(), false));
                }
                return false;
            }

            @Override
            public boolean visit(InstanceofExpression ie) {
                ie.getLeftOperand().accept(this);
                Type t = JDTTypeToTaieType(ie.getRightOperand().resolveBinding());
                context.pushStack(new InstanceOfExp(popVar(), (ReferenceType) t));
                return false;
            }

            @Override
            public boolean visit(TypeLiteral tl) {
                ITypeBinding typeBinding = tl.getType().resolveBinding();
                Type taieType = JDTTypeToTaieType(typeBinding);
                context.pushStack(ClassLiteral.get(taieType));
                return false;
            }
        }
    }
}

