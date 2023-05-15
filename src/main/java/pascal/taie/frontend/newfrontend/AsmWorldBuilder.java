package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.AbstractWorldBuilder;
import pascal.taie.World;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.Options;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.project.OptionsProjectBuilder;
import pascal.taie.project.Project;
import pascal.taie.project.ProjectBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class AsmWorldBuilder extends AbstractWorldBuilder {

    private static final Logger logger = LogManager.getLogger(AsmWorldBuilder.class);

    /**
     * Path to the file which specifies the basic classes that should be
     * added to Scene in advance.
     */
    private static final String BASIC_CLASSES = "basic-classes.yml";

    @Override
    public void build(Options options, List<AnalysisConfig> analyses) {
        build(options);
    }

    private void build(Options options) {
        World.reset();
        World world = new World();
        World.set(world);


        // options will be used during World building, thus it should be
        // set at first.
        world.setOptions(options);

        ProjectBuilder projectBuilder = new OptionsProjectBuilder(options);
        Project project = projectBuilder.build();
        ClosedWorldBuilder closedWorldBuilder = new ClassInfoCWBuilder(); // Configurable
        closedWorldBuilder.build(project);
        Collection<ClassSource> closedWorld = closedWorldBuilder.getClosedWorld();
        ClassHierarchyBuilder hierarchyBuilder = new DefaultCHBuilder();
        ClassHierarchy hierarchy = hierarchyBuilder.build(closedWorld);
        world.setClassHierarchy(hierarchy);
        TypeSystem typeSystem = BuildContext.get().getTypeSystem(); // the singleton context was built in hierarchyBuilder.build
        world.setTypeSystem(typeSystem);

        // classes has been built in hierarchyBuilder.build()

        // set main method
        String mainClassName = options.getMainClass();
        if (mainClassName != null) {
            JClass mainClass = hierarchy.getClass(mainClassName);
            assert mainClass != null;
            JMethod mainMethod = mainClass.getDeclaredMethod(Subsignature.getMain()); // TODO: what is src_prec_dotnet in Scene:313?
            if (mainMethod != null) {
                world.setMainMethod(mainMethod);
            } else {
                logger.warn("Warning: main class '{}'" +
                                " does not have main(String[]) method!",
                        options.getMainClass());
            }
        } else {
            logger.warn("Warning: main class was not given!");
        }

        // set implicit entries
        world.setImplicitEntries(implicitEntries.stream()
                .map(hierarchy::getJREMethod)
                // some implicit entries may not exist in certain JDK version,
                // thus we filter out null
                .filter(Objects::nonNull)
                .toList());

        // initialize IR builder
        world.setNativeModel(getNativeModel(typeSystem, hierarchy));
        IRBuilder irBuilder = new IRBuilder();
        world.setIRBuilder(irBuilder);
        if (options.isPreBuildIR()) {
            irBuilder.buildAll(hierarchy);
        }
    }
}
