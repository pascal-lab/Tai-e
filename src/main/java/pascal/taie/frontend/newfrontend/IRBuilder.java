package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRBuildHelper;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Timer;

class IRBuilder implements pascal.taie.ir.IRBuilder {

    private static final Logger logger = LogManager.getLogger(IRBuilder.class);

    @Override
    public IR buildIR(JMethod method) {
        try {
            // TODO: Add more IRBuilder for different types of source.
            Object source = method.getMethodSource();
            if (source instanceof AsmMethodSource asmMethodSource) {
                AsmIRBuilder builder = new AsmIRBuilder(method, asmMethodSource);
                builder.build();
                return builder.getIr();
            } else if (source == null) {
                AsmIRBuilder builder = new AsmIRBuilder(method, BuildContext.get().getSource(method));
                builder.build();
                return builder.getIr();
            } else {
                throw new FrontendException("NewFrontend currently does not support " + source.getClass().getName());
            }
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
        hierarchy.allClasses().parallel().forEach(c -> {
            for (JMethod m : c.getDeclaredMethods()) {
                if (! m.isAbstract() && ! m.isNative()) {
                    m.getIR();
                }
            }
        });
        timer.stop();
        logger.info(timer);
    }
}
