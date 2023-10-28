package pascal.taie.analysis.pta.plugin.taint.TFGDumperStruct;

import pascal.taie.analysis.graph.flowgraph.*;
import pascal.taie.analysis.pta.plugin.taint.TFGDumperStruct.MetaData;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Relation {

    public final Map<Long, List<Long>> packageToClasses = Maps.newHybridMap();

    public final Map<Long, List<Long>> classToMethods = Maps.newHybridMap();

    public final Map<Long, List<Long>> classToFields = Maps.newHybridMap();

    public final Map<Long, List<Long>> methodToVars = Maps.newHybridMap();

    public Relation(Set<Node> nodes, MetaData md){
        MultiMap<Long, Long> p2c = Maps.newMultiMap();
        MultiMap<Long, Long> c2m = Maps.newMultiMap();
        MultiMap<Long, Long> c2f = Maps.newMultiMap();
        MultiMap<Long, Long> m2v = Maps.newMultiMap();

        for(Node n : nodes){
            Long packageIndex;
            Long classIndex;
            Long methodIndex = -1L;
            Long varOrFieldIndex = md.indexOfVarAndField(n.toString());

            if(n instanceof VarNode vn){
                methodIndex = md.indexOfMethod(vn.getVar().getMethod().toString());
                classIndex = md.indexOfClass(vn.getVar().getMethod().getDeclaringClass().toString());
                packageIndex = md.packageFromClass(vn.getVar().getMethod().getDeclaringClass().toString());
            }
            else if(n instanceof InstanceFieldNode ifn){
                classIndex = md.indexOfClass(ifn.getField().getDeclaringClass().toString());
                packageIndex = md.packageFromClass(ifn.getField().getDeclaringClass().toString());
            }
            else if(n instanceof StaticFieldNode sfn){
                classIndex = md.indexOfClass(sfn.getField().getDeclaringClass().toString());
                packageIndex = md.packageFromClass(sfn.getField().getDeclaringClass().toString());
            }
            else if(n instanceof ArrayIndexNode ain){
                if(ain.getBase().getContainerMethod().isPresent()) {
                    methodIndex = md.indexOfMethod(ain.getBase().getContainerMethod().get().toString());
                    classIndex = md.indexOfClass(ain.getBase().getContainerMethod().get().getDeclaringClass().toString());
                    packageIndex = md.packageFromClass(ain.getBase().getContainerMethod().get().getDeclaringClass().toString());
                }
                else{
                    throw new RuntimeException("ArrayIndexNode not contained by a method");
                }
            }
            else{
                throw new RuntimeException("Can't process new Node");
            }

            if(methodIndex != -1){
                m2v.put(methodIndex, varOrFieldIndex);
                c2m.put(classIndex, methodIndex);
            }
            else{
                c2f.put(classIndex, varOrFieldIndex);
            }
            p2c.put(packageIndex, classIndex);
        }

        p2c.keySet().forEach(k->this.packageToClasses.put(k, p2c.get(k).stream().sorted().toList()));
        c2m.keySet().forEach(k->this.classToMethods.put(k, c2m.get(k).stream().sorted().toList()));
        c2f.keySet().forEach(k->this.classToFields.put(k, c2f.get(k).stream().sorted().toList()));
        m2v.keySet().forEach(k->this.methodToVars.put(k, m2v.get(k).stream().sorted().toList()));
    }
}
