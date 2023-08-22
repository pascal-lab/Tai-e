package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.TransferGenerator;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeMatching implements TransInferStrategy {

    private double threshold;

    private Solver solver;

    private TransferGenerator generator;

    private Process pythonProc;

    private BufferedReader inputReader;

    private BufferedWriter outputWriter;

    private TwoKeyMap<String, String, Double> typeSimilarityMap;

    @Override
    public void setContext(InfererContext context) {
        solver = context.solver();
        generator = context.generator();
        typeSimilarityMap = Maps.newTwoKeyMap();
        try {
            File pyFile = new File("src/main/resources/word-similarity.py");
            pythonProc = Runtime.getRuntime().exec(new String[]{"python", "-u", pyFile.getAbsolutePath()});
            inputReader = pythonProc.inputReader();
            outputWriter = pythonProc.outputWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        threshold = switch (context.config().inferenceConfig().confidence()) {
            case DISABLE -> throw new AnalysisException();
            case LOW -> 0.4;
            case MEDIUM -> 0.6;
            case HIGH -> 0.8;
        };
    }

    @Override
    public Set<InferredTransfer> generate(CSCallSite csCallSite, int index) {
        Set<InferredTransfer> transfers = Sets.newSet();
        Set<Type> fromTypes = StrategyUtils.getArgType(solver, csCallSite, index);
        if(!fromTypes.isEmpty()) {
            // TODO: add other *to* index
            Set<Type> resultTypes = StrategyUtils.getArgType(solver, csCallSite, InvokeUtils.RESULT);
            for(Type from : fromTypes) {
                if(resultTypes.stream().anyMatch(to -> matchType(from, to))) {
                    transfers.addAll(generator.getTransfers(csCallSite, index, InvokeUtils.RESULT));
                    break;
                }
            }
            if (index != InvokeUtils.BASE && !csCallSite.getCallSite().isStatic()) {
                Set<Type> baseTypes = StrategyUtils.getArgType(solver, csCallSite, InvokeUtils.BASE);
                for(Type from : fromTypes) {
                    if(baseTypes.stream().anyMatch(to -> matchType(from, to))) {
                        transfers.addAll(generator.getTransfers(csCallSite, index, InvokeUtils.BASE));
                        break;
                    }
                }
            }
        }
        return transfers;
    }

    @Override
    public Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        Set<Type> fromTypes = StrategyUtils.getArgType(solver, csCallSite, index);
        return transfers.stream()
                .filter(tf -> fromTypes.stream().anyMatch(from -> matchType(from, tf.getType())))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void onFinish() {
        try {
            inputReader.close();
            outputWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        pythonProc.destroy();
    }

    private double getTypeSimilarity(String type1, String type2) {
        try {
            outputWriter.write(type1 + ' ' + type2 + '\n');
            outputWriter.flush();
            return Double.parseDouble(inputReader.readLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getSimpleTypeName(Type type) {
        String fullName = type.getName();
        int index = fullName.lastIndexOf('$');
        if(index != -1) {
            return fullName.substring(index + 1);
        }
        index = fullName.lastIndexOf('.');
        return index == -1 ? fullName : fullName.substring(index + 1);
    }

    private boolean matchType(Type type1, Type type2) {
        String s1 = getSimpleTypeName(type1);
        String s2 = getSimpleTypeName(type2);
        if(s1.equals("String") || s2.equals("String")) {
            return true;
        }
        Double similarity = typeSimilarityMap.get(s1, s2);
        if(similarity == null) {
            similarity = getTypeSimilarity(s1, s2);
            typeSimilarityMap.put(s1, s2, similarity);
        }
        return similarity >= threshold;
    }
}
