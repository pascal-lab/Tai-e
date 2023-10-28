package pascal.taie.analysis.pta.plugin.taint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import pascal.taie.analysis.graph.flowgraph.*;
import pascal.taie.analysis.pta.plugin.taint.TFGDumperStruct.MetaData;
import pascal.taie.analysis.pta.plugin.taint.TFGDumperStruct.Relation;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class DumperStruct {

    public final MetaData metadata;

    /** represent the relations among packages, classes, methods, variables and fields */
    public final Relation relation;

    /** represent the taint flow path */
    public final Map<Long, List<Long>> graph;

    public DumperStruct(TaintFlowGraph tfg){
        Set<Node> nodes = tfg.getNodes();
//        Collection<FlowEdge> edges = tfg.getEdges();
        Collection<FlowEdge> edges = tfg.getNodes().stream()
                .map(tfg::getOutEdgesOf)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        List<JMethod> methodList = allMethodsFromNodes(nodes);
        List<JClass> classList = allClassFromNodes(nodes, methodList);
        this.metadata = new MetaData(nodes, methodList, classList);

        this.relation = new Relation(nodes, metadata);


        this.graph = Maps.newHybridMap();
        edges.forEach(edge->{
            Long key = this.metadata.indexOfVarAndField(edge.source().toString());
            Long value = this.metadata.indexOfVarAndField(edge.target().toString());
            if(!this.graph.containsKey(this.metadata.indexOfVarAndField(edge.source().toString()))){
                this.graph.put(key, new ArrayList<>());
            }
            this.graph.get(key).add(value);
        });
        this.graph.replaceAll((k, v) -> this.graph.get(k).stream().distinct().sorted().toList());
    }

    /** get all methods that contain at least one variable in nodes */
    private List<JMethod> allMethodsFromNodes(Set<Node> nodes){
        List<JMethod> methodList = new ArrayList<>();
        methodList.addAll(nodes.stream().filter(n -> (n instanceof VarNode))
                .map(n -> ((VarNode) n).getVar().getMethod())
                .toList());
        methodList.addAll(nodes.stream().filter(n -> (n instanceof ArrayIndexNode))
                .map(n -> {
                    ArrayIndexNode ain = ((ArrayIndexNode) n);
                    if(ain.getBase().getContainerMethod().isPresent()){
                        return ain.getBase().getContainerMethod().get();
                    }
                    throw new RuntimeException("Error occurs while finding container of an ArrayIndexNode ");
                })
                .toList());
        return methodList;
    }

    /** get all classes which:
     *  1.contain at least one field in nodes,
     *  2.contain methods that contain at least one variable in nodes
     */
    private List<JClass> allClassFromNodes(Set<Node> nodes, List<JMethod> methodList){
        /* add class containing taint field */
        List<JClass> classList = new ArrayList<>();
        classList.addAll(nodes.stream().filter(n -> (n instanceof InstanceFieldNode))
                .map(n -> ((InstanceFieldNode) n).getField().getDeclaringClass())
                .toList());
        classList.addAll(nodes.stream().filter(n -> (n instanceof StaticFieldNode))
                .map(n -> ((StaticFieldNode) n).getField().getDeclaringClass())
                .toList());

        /* add class containing methods that contain taint */
        classList.addAll(methodList.stream()
                .map(ClassMember::getDeclaringClass)
                .toList());
        return classList;
    }

    public void dump(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR));
        mapper.writeValue(file, this);
    }
}
