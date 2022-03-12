package pascal.taie.frontend.newfrontend.exposed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.frontend.newfrontend.NewMethodIRBuilder;
import pascal.taie.ir.IR;
import pascal.taie.language.classes.JMethod;
import soot.SootMethod;
import soot.tagkit.SourceFileTag;

import java.util.Optional;

record PathAndName(String path, String name) { }

public class IRBuilderHook {
    public static Logger logger = LogManager.getLogger(IRBuilderHook.class);
    public static Optional<IR> getMethodIRByNewFrontend(SootMethod method, JMethod jMethod) {
        return getJavaFileName(method).flatMap(
                pathAndName -> new NewMethodIRBuilder(
                        pathAndName.path(),
                        pathAndName.name(),
                        method,
                        jMethod).build());
    }

    public static Optional<PathAndName> getJavaFileName(SootMethod method) {
        var sootClass = method.getDeclaringClass();
        for (var tag : sootClass.getTags()) {
            if (tag instanceof SourceFileTag t) {
                if (t.getAbsolutePath() == null) {
                    logger.debug("sootClass [" + sootClass.getName() +
                            "] has sourceTag, but not source file found");
                    return Optional.empty();
                }
                return Optional.of(new PathAndName(t.getAbsolutePath(), t.getName()));
            }
        }
        return Optional.empty();
    }
}
