/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.language.natives;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.InvokeSpecial;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.NewArray;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.StaticFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static pascal.taie.language.classes.StringReps.OBJECT;
import static pascal.taie.language.classes.StringReps.STRING;
import static pascal.taie.util.collection.CollectionUtils.newMap;

public class DefaultNativeModel extends AbstractNativeModel {

    private static final Logger logger = LogManager.getLogger(DefaultNativeModel.class);

    private final TypeManager typeManager;

    private final ClassHierarchy hierarchy;

    private final Map<JMethod, Function<JMethod, IR>> models = newMap();

    public DefaultNativeModel(TypeManager typeManager,
                              ClassHierarchy hierarchy) {
        super(typeManager, hierarchy);
        this.typeManager = typeManager;
        this.hierarchy = hierarchy;
        initModels();
    }

    @Override
    public IR buildNativeIR(JMethod method) {
        return models.getOrDefault(method,
                m -> new NativeIRBuilder(method).buildEmpty())
                .apply(method);
    }

    private void initModels() {
        // --------------------------------------------------------------------
        // java.lang.Object
        // --------------------------------------------------------------------
        // <java.lang.Object: java.lang.Object clone()>
        // TODO: could throw CloneNotSupportedException
        // TODO: should check if the object is Cloneable.
        // TODO: should return a clone of the heap allocation (not
        //  identity). The behaviour implemented here is based on Soot.
        register("<java.lang.Object: java.lang.Object clone()>", m -> {
            NativeIRBuilder builder = new NativeIRBuilder(m);
            List<Stmt> stmts = new ArrayList<>();
            stmts.add(new Copy(builder.getReturnVar(), builder.getThisVar()));
            stmts.add(builder.newReturn());
            return builder.build(stmts);
        });

        // --------------------------------------------------------------------
        // java.lang.ref.Finalizer
        // --------------------------------------------------------------------
        // <java.lang.ref.Finalizer: void invokeFinalizeMethod(java.lang.Object)>
        //
        // Indirect invocations of finalize methods from java.lang.ref.Finalizer.
        // Object.finalize is a protected method, so it cannot be directly
        // invoked. Finalizer uses an indirection via native code to
        // circumvent this. This rule implements this indirection.
        // This API is deprecated since Java 7.
        if (World.getOptions().getJavaVersion() <= 6) {
            register("<java.lang.ref.Finalizer: void invokeFinalizeMethod(java.lang.Object)>", m ->
                    invokeVirtualMethod(m, "<java.lang.Object: void finalize()>",
                            b -> b.getParam(0), b -> null)
            );
        }

        // --------------------------------------------------------------------
        // java.lang.System
        // --------------------------------------------------------------------
        // <java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>
        register("<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>", m -> {
            NativeIRBuilder builder = new NativeIRBuilder(m);
            Var src = builder.getParam(0);
            Var dest = builder.getParam(2);
            Type objType = typeManager.getClassType(OBJECT);
            Type arrayType = typeManager.getArrayType(objType, 1);
            Var srcArray = builder.newTempVar(arrayType);
            Var destArray = builder.newTempVar(arrayType);
            // Here the index is just a placeholder for array access.
            // It does not correctly model the semantics of arraycopy,
            // but it is sufficient for flow-insensitive pointer analysis.
            Var index = builder.getParam(1);
            Var temp = builder.newTempVar(objType);
            // src/dest may point to non-array objects due to imprecision
            // of pointer analysis, thus we add cast statements to filter
            // out load/store operations on non-array objects.
            List<Stmt> stmts = new ArrayList<>();
            stmts.add(new Cast(srcArray, new CastExp(src, arrayType)));
            stmts.add(new Cast(destArray, new CastExp(dest, arrayType)));
            stmts.add(new LoadArray(temp, new ArrayAccess(srcArray, index)));
            stmts.add(new StoreArray(new ArrayAccess(destArray, index), temp));
            stmts.add(builder.newReturn());
            return builder.build(stmts);
        });

        // <java.lang.System: void setIn0(java.io.InputStream)>
        register("<java.lang.System: void setIn0(java.io.InputStream)>", m ->
                storeStaticField(m,
                        "<java.lang.System: java.io.InputStream in>",
                        b -> b.getParam(0))
        );

        // <java.lang.System: void setOut0(java.io.PrintStream)>
        register("<java.lang.System: void setOut0(java.io.PrintStream)>", m ->
                storeStaticField(m,
                        "<java.lang.System:java.io.PrintStream out>",
                        b -> b.getParam(0))
        );

        // <java.lang.System: void setErr0(java.io.PrintStream)>
        register("<java.lang.System: void setErr0(java.io.PrintStream)>", m ->
                storeStaticField(m,
                        "<java.lang.System: java.io.PrintStream err>",
                        b -> b.getParam(0))
        );

        // --------------------------------------------------------------------
        // java.lang.Thread
        // --------------------------------------------------------------------
        // <java.lang.Thread: void start[0]()>
        // Redirect calls to Thread.start() to Thread.run().
        // Before Java 5, Thread.start() itself is native. Since Java 5,
        // start() is written in Java which calls native method start0().
        final String start = World.getOptions().getJavaVersion() <= 4
                ? "<java.lang.Thread: void start()>"
                : "<java.lang.Thread: void start0()>";
        register(start, m ->
                invokeVirtualMethod(m, "<java.lang.Thread: void run()>",
                        NativeIRBuilder::getThisVar, b -> null)
        );

        // --------------------------------------------------------------------
        // java.io.FileSystem
        // --------------------------------------------------------------------
        final List<String> concreteFileSystems = List.of(
                "java.io.UnixFileSystem",
                "java.io.WinNTFileSystem",
                "java.io.Win32FileSystem"
        );
        String fsName = concreteFileSystems.stream()
                .filter(s -> hierarchy.getJREClass(s) != null)
                .findFirst()
                .orElse(null);
        if (fsName != null) {
            // <java.io.FileSystem: java.io.FileSystem getFileSystem()>
            // This API is deprecated since Java 7.
            // TODO: ideally, this method should return the same EnvObj across
            //  multiple invocations. A possible solution is to use a flag
            //  to mark these native-related allocation sites, so that
            //  HeapModel can recognize them and convert them to EnvObj.
            if (World.getOptions().getJavaVersion() <= 6) {
                register("<java.io.FileSystem: java.io.FileSystem getFileSystem()>", m ->
                        allocateObject(m, "<" + fsName + ": void <init>()>",
                                b -> Collections.emptyList())
                );
            }

            // <java.io.*FileSystem: java.lang.String[] list(java.io.File)>
            register("<" + fsName + ": java.lang.String[] list(java.io.File)>", m -> {
                NativeIRBuilder builder = new NativeIRBuilder(m);
                ClassType string = typeManager.getClassType(STRING);
                ArrayType stringArray = typeManager.getArrayType(string, 1);
                Var str = builder.newTempVar(string);
                Var arr = builder.getReturnVar();
                // here n is just a placeholder of array-related statements,
                // thus is value is irrelevant.
                Var n = builder.newTempVar(PrimitiveType.INT);
                List<Stmt> stmts = new ArrayList<>();
                stmts.add(new New(m, str, new NewInstance(string)));
                stmts.add(new New(m, arr, new NewArray(stringArray, n)));
                stmts.add(new StoreArray(new ArrayAccess(arr, n), str));
                stmts.add(builder.newReturn());
                return builder.build(stmts);
            });
        } else {
            logger.warn("Cannot find implementation of FileSystem");
        }

        // --------------------------------------------------------------------
        // java.security.AccessController
        // --------------------------------------------------------------------
        // The run methods of privileged actions are invoked through the
        // AccessController.doPrivileged method. This introduces an
        // indirection via native code that needs to be simulated in a pointer
        // analysis.
        //
        // Call from an invocation of doPrivileged to an implementation of the
        // PrivilegedAction.run method that will be indirectly invoked.
        //
        // The first parameter of a doPrivileged invocation (a
        // PrivilegedAction) is assigned to the 'this' variable of 'run()'
        // method invocation.
        //
        // The return variable of the 'run()' method of a privileged action is
        // assigned to the return result of the doPrivileged method
        // invocation.
        //
        // TODO for PrivilegedExceptionAction, catch exceptions and wrap them
        //  in a PrivilegedActionException.
        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction)>
        register("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction)>", m ->
                invokeVirtualMethod(m,
                        "<java.security.PrivilegedAction: java.lang.Object run()>",
                        b -> b.getParam(0), NativeIRBuilder::getReturnVar)
        );

        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction,java.security.AccessControlContext)>
        register("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction,java.security.AccessControlContext)>", m ->
                invokeVirtualMethod(m,
                        "<java.security.PrivilegedAction: java.lang.Object run()>",
                        b -> b.getParam(0), NativeIRBuilder::getReturnVar)
        );

        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)>
        register("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)>", m ->
                invokeVirtualMethod(m,
                        "<java.security.PrivilegedExceptionAction: java.lang.Object run()>",
                        b -> b.getParam(0), NativeIRBuilder::getReturnVar)
        );

        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,java.security.AccessControlContext)>
        register("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,java.security.AccessControlContext)>", m ->
                invokeVirtualMethod(m,
                        "<java.security.PrivilegedExceptionAction: java.lang.Object run()>",
                        b -> b.getParam(0), NativeIRBuilder::getReturnVar)
        );

        // --------------------------------------------------------------------
        // sun.misc.Perf
        // --------------------------------------------------------------------
        // <sun.misc.Perf: java.nio.ByteBuffer createLong(java.lang.String,int,int,long)>
        register("<sun.misc.Perf: java.nio.ByteBuffer createLong(java.lang.String,int,int,long)>", m ->
                allocateObject(m, "<java.nio.DirectByteBuffer: void <init>(int)>",
                        b -> Collections.singletonList(b.getParam(2)))
        );
    }

    // --------------------------------------------------------------------
    // Convenient methods for helping create native model.
    // --------------------------------------------------------------------
    /**
     * Register models for specific native methods.
     */
    private void register(String methodSig, Function<JMethod, IR> model) {
        JMethod method = hierarchy.getJREMethod(methodSig);
        models.put(method, model);
    }

    /**
     * Create an IR which contains a store statement to the specified static field.
     */
    private IR storeStaticField(
            JMethod method, String fieldSig,
            Function<NativeIRBuilder, Var> getFrom) {
        NativeIRBuilder builder = new NativeIRBuilder(method);
        JField field = hierarchy.getJREField(fieldSig);
        List<Stmt> stmts = new ArrayList<>();
        stmts.add(new StoreField(new StaticFieldAccess(field.getRef()),
                getFrom.apply(builder)));
        stmts.add(builder.newReturn());
        return builder.build(stmts);
    }

    /**
     * Create an IR which contains a invoke statement to the specific virtual method.
     */
    private IR invokeVirtualMethod(
            JMethod method, String calleeSig,
            Function<NativeIRBuilder, Var> getRecv,
            Function<NativeIRBuilder, Var> getRet) {
        NativeIRBuilder builder = new NativeIRBuilder(method);
        JMethod callee = hierarchy.getJREMethod(calleeSig);
        List<Stmt> stmts = new ArrayList<>();
        InvokeVirtual callSite = new InvokeVirtual(callee.getRef(),
                getRecv.apply(builder), Collections.emptyList());
        stmts.add(new Invoke(method, callSite, getRet.apply(builder)));
        stmts.add(builder.newReturn());
        return builder.build(stmts);
    }

    /**
     * Create an IR which allocates a new object, invokes its specified
     * constructor, and returns it.
     */
    private IR allocateObject(
            JMethod method, String ctorSig,
            Function<NativeIRBuilder, List<Var>> getArgs) {
        NativeIRBuilder builder = new NativeIRBuilder(method);
        JMethod ctor = hierarchy.getJREMethod(ctorSig);
        ClassType type = ctor.getDeclaringClass().getType();
        Var base = builder.newTempVar(type);
        List<Stmt> stmts = new ArrayList<>();
        stmts.add(new New(method, base, new NewInstance(type)));
        stmts.add(new Invoke(method,
                new InvokeSpecial(ctor.getRef(), base, getArgs.apply(builder))));
        stmts.add(new Copy(builder.getReturnVar(), base));
        stmts.add(builder.newReturn());
        return builder.build(stmts);
    }
}
