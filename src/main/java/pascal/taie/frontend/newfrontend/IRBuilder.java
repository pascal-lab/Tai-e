package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRBuildHelper;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class IRBuilder implements pascal.taie.ir.IRBuilder {

    private static final Logger logger = LogManager.getLogger(IRBuilder.class);

    @Override
    public IR buildIR(JMethod method) {
        try {
            AsmMethodSource source = (AsmMethodSource) method.getMethodSource();
            AsmIRBuilder builder = new AsmIRBuilder(method, source);
            builder.build();
            return builder.getIr();
        } catch (RuntimeException e) {
            if (e.getStackTrace()[0].getClassName().startsWith("Asm")) {
                logger.warn("ASM bytecode front failed to build method body for {}," +
                        " constructs an empty IR instead", method);
                return new IRBuildHelper(method).buildEmpty();
            } else {
                throw e;
            }
        }
    }

    /**
     * Builds IR for all methods in given class hierarchy.
     * TODO: currently copied from soot.IRBuilder
     * Considering a abstract class to be the common supertype.
     */
    @Override
    public void buildAll(ClassHierarchy hierarchy) {
        Timer timer = new Timer("Build IR for all methods");
        timer.start();
        int nThreads = Runtime.getRuntime().availableProcessors();
        // Group all methods by number of threads
        List<List<JMethod>> groups = new ArrayList<>();
        for (int i = 0; i < nThreads; ++i) {
            groups.add(new ArrayList<>());
        }
        List<JClass> classes = hierarchy.allClasses().toList();
        int i = 0;
        for (JClass c : classes) {
            for (JMethod m : c.getDeclaredMethods()) {
                if (!m.isAbstract() || m.isNative()) {
                    groups.get(i++ % nThreads).add(m);
                }
            }
        }
        // Build IR for all methods in parallel
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        for (List<JMethod> group : groups) {
            service.execute(() -> group.forEach(JMethod::getIR));
        }
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        timer.stop();
        logger.info(timer);
    }
}
