package pascal.taie.analysis.pta.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class MayFailCast extends ProgramAnalysis {

    public static final String ID = "fcast";

    private static final Logger logger = LogManager.getLogger(MayFailCast.class);

    private final String algorithm;

    private final boolean onlyApp;

    private final TypeManager typeManager;

    public MayFailCast(AnalysisConfig config) {
        super(config);
        algorithm = config.getOptions().getString("algorithm");
        onlyApp = config.getOptions().getBoolean("only-app");
        typeManager = World.get().getTypeManager();
    }

    @Override
    public Object analyze() {
        PointerAnalysisResult result = World.get().getResult(algorithm);
        Collection<Var> vars = result.getVars();
        CallGraph<Invoke, JMethod> callGraph = result.getCallGraph();
        List<Cast> mayFailCasts = new ArrayList<>();
        List<JMethod> methodList;
        if(onlyApp){
            methodList = callGraph.reachableMethods().
                    filter(m -> m.getDeclaringClass().isApplication()).
                    toList();
        }else{
            methodList = callGraph.reachableMethods().toList();
        }
        methodList.forEach(method -> {
            method.getIR().getStmts().stream().filter(stmt -> stmt instanceof Cast).forEach(stmt ->{
                Cast cast = (Cast) stmt;
                Var from = cast.getRValue().getValue();
                Type castType = cast.getRValue().getCastType();
                if(vars.contains(from)){
                    for(Obj obj : result.getPointsToSet(from)){
                        if(!typeManager.isSubtype(castType,obj.getType())){
                            mayFailCasts.add(cast);
                            break;
                        }
                    }
                }
            });
        });

        logStatistics(mayFailCasts);
        processOptions(mayFailCasts,getOptions());
        return null;
    }

    private void logStatistics(List<Cast> mayFailCasts){
        if(onlyApp){
            logger.info("#may fail cast(only-app) : {}", mayFailCasts.size());
        }else{
            logger.info("#may fail cast : {}", mayFailCasts.size());
        }
    }

    private static void processOptions(List<Cast> mayFailCasts,AnalysisOptions options){
        String action = options.getString("action");
        if(action ==null){
            return;
        }
        if(action.equals("dump")){
            String file = options.getString("file");
            //dump result
            if(file == null){
                logger.warn("To dump the result of may-fail-cast, file path needs to be specified");
            }else{
                try(PrintStream out = new PrintStream(new FileOutputStream(file))) {
                    logger.info("Dumping may-fail-cast to {} ...",file);
                    mayFailCasts.forEach(out::println);
                } catch (FileNotFoundException e) {
                   logger.warn("Failed to dump may-fail-cast to " + file, e);
                }
            }
        }
    }

}
