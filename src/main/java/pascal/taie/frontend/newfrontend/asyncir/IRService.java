package pascal.taie.frontend.newfrontend.asyncir;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import pascal.taie.frontend.newfrontend.AsmIRBuilder;
import pascal.taie.frontend.newfrontend.AsmMethodSource;
import pascal.taie.frontend.newfrontend.AsmSource;
import pascal.taie.frontend.newfrontend.BuildContext;
import pascal.taie.ir.IR;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.Type;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IRService {

    private static final int NOT_LOADED = 0;
    private static final int LOADING_START = 1;
    private static final int LOADING_DONE = 2;

//    ExecutorService executorService = Executors.newFixedThreadPool(8);

    private final ConcurrentHashMap<JMethod, AtomicBoolean> methodStatusMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<JClass, AtomicInteger> classStatusMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<JClass, AsmSource> class2Node = new ConcurrentHashMap<>();

    private final ConcurrentMap<JMethod, AsmMethodSource> method2Source = new ConcurrentHashMap<>();

    public void getIRAsync(JMethod method) {
        AtomicBoolean status = methodStatusMap.computeIfAbsent(method, k -> new AtomicBoolean(false));
        if (status.compareAndSet(false, true)) {
//            executorService.submit(() -> {
//                method.getIR();
//            });
        }
    }

    public IR loadingAndGetIR(JMethod method) {
        loadClassSourceSync(method.getDeclaringClass());
        AsmMethodSource source = method2Source.remove(method);
        if (source == null) {
            throw new IllegalStateException("""
                    Cannot find method source for %s,
                    most likely the method is built twice by mistake.
                    """.formatted(method));
        }
        AsmIRBuilder builder = new AsmIRBuilder(method, source);
        builder.build();
        IR ir = builder.getIr();
        if (ir == null) {
            throw new IllegalStateException("IR is null for method %s".formatted(method));
        }
        return ir;
    }

    public void loadClassSourceSync(JClass clazz) {
        // TODO: current sync method is correct, but may need some optimization
        AtomicInteger status = classStatusMap.computeIfAbsent(clazz, k -> new AtomicInteger(NOT_LOADED));
        if (status.get() == LOADING_DONE) {
            return;
        } else if (status.compareAndSet(NOT_LOADED, LOADING_START)) {
            try {
                loadClassSourceImpl(clazz);
                status.set(LOADING_DONE);
            } finally {
                // put this in finally block to avoid deadlock
                synchronized (status) { // notifyAll() must be called in synchronized block
                    status.notifyAll();
                }
            }
        } else {
            synchronized (status) { // wait() must be called in synchronized block
                // now we have to wait for the class to be loaded
                while (status.get() == LOADING_START) {
                    try {
                        status.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    public void loadClassSourceImpl(JClass clazz) {
        // use remove to release memory
        AsmSource source = class2Node.remove(clazz);
        assert source != null;
        int version = source.getClassFileVersion();
        source.r().accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                JSRInlinerAdapter adapter = new JSRInlinerAdapter(null, access, name, descriptor, signature, exceptions);
                var paramAndRet = BuildContext.get().fromAsmMethodType(descriptor);
                List<Type> paramTypes = paramAndRet.first();
                Type retType = paramAndRet.second();
                JMethod method1 = clazz.getDeclaredMethod(Subsignature.get(name, paramTypes, retType));
                if (method1 == null) {
                    throw new IllegalStateException("Cannot find method %s in class %s".formatted(name, clazz));
                }
                if (method2Source.containsKey(method1)) {
                    throw new IllegalStateException("Method %s is built twice".formatted(method1));
                }
                if (method1.getDeclaringClass() != clazz) {
                    throw new IllegalStateException("Method %s is not declared in class %s".formatted(method1, clazz));
                }
                method2Source.put(method1, new AsmMethodSource(adapter, version));
                return adapter;
            }
        }, ClassReader.EXPAND_FRAMES);
    }

    public void noticeMethodCall(MethodRef ref) {
        getIRAsync(ref.resolve());
    }

    public void putClassSource(JClass clazz, AsmSource source) {
        class2Node.put(clazz, source);
    }
}
