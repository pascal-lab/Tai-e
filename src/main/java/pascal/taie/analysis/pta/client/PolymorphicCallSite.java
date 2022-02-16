package pascal.taie.analysis.pta.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class PolymorphicCallSite extends ProgramAnalysis {

    public static final String ID = "pcall";

    private static final Logger logger = LogManager.getLogger(MayFailCast.class);

    private final String algorithm;

    private final boolean onlyApp;

    public PolymorphicCallSite(AnalysisConfig config){
        super(config);
        algorithm = config.getOptions().getString("algorithm");
        onlyApp = config.getOptions().getBoolean("only-app");
    }

    @Override
    public Object analyze() {
        PointerAnalysisResult result = World.get().getResult(algorithm);
        CallGraph<Invoke, JMethod> callGraph = result.getCallGraph();
        List<Invoke> polymorphicCallSites = new ArrayList<>();
        List<JMethod> methodList;
        if(onlyApp){
            methodList = callGraph.reachableMethods().
                    filter(m -> m.getDeclaringClass().isApplication()).
                    toList();
        }else{
            methodList = callGraph.reachableMethods().toList();
        }
        methodList.forEach(method -> {
            method.getIR().getStmts().stream()
                    .filter(stmt -> stmt instanceof Invoke).map(stmt -> (Invoke)stmt)
                    .filter(invoke->invoke.isVirtual()||invoke.isInterface()).forEach(invoke -> {
                        if(callGraph.getCalleesOf(invoke).size()>1){
                            polymorphicCallSites.add(invoke);
                        }
                    });
        });
        logStatistics(polymorphicCallSites);
        processOptions(polymorphicCallSites,getOptions());
        return null;
    }

    private void logStatistics(List<Invoke> polymorphicCallSites){
        if(onlyApp){
            logger.info("#may fail cast(only-app) : {}", polymorphicCallSites.size());
        }else{
            logger.info("#may fail cast : {}", polymorphicCallSites.size());
        }
    }

    private static void processOptions(List<Invoke> polymorphicCallSites, AnalysisOptions options){
        String action = options.getString("action");
        if(action ==null){
            return;
        }
        if(action.equals("dump")){
            String file = options.getString("file");
            //dump result
            if(file == null){
                logger.warn("To dump the result of poly-call-site, file path needs to be specified");
            }else{
                try(PrintStream out = new PrintStream(new FileOutputStream(file))) {
                    logger.info("Dumping poly-call-site to {} ...",file);
                    polymorphicCallSites.forEach(out::println);
                } catch (FileNotFoundException e) {
                    logger.warn("Failed to dump poly-call-site to " + file, e);
                }
            }
        }
    }

}
