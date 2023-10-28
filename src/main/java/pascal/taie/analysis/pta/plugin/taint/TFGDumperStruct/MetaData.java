package pascal.taie.analysis.pta.plugin.taint.TFGDumperStruct;

import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * Map every node to a number,
 * The representation of node is String,
 * and what it maps to is its index in List.
 */
public class MetaData {
    public final List<String> packages;

    public final List<String> classes;

    public final List<String> methods;

    public final List<String> varsAndFields;

    public MetaData(Set<Node> nodes, List<JMethod> methodList, List<JClass> classList){
        /* todo: everything can be shortened */
        this.varsAndFields = nodes.stream().map(Objects::toString).sorted().toList();

        this.methods = methodList.stream().map(ClassMember::toString).distinct().sorted().toList();

        this.classes = classList.stream().map(JClass::toString).distinct().sorted().toList();

        this.packages = this.classes.stream()
                .map(s -> {
                    int index = s.lastIndexOf('.');
                    if(index != -1) {
                        return s.substring(0, s.lastIndexOf('.'));
                    }
                    else{
                        return s;
                    }
                })
                .distinct()
                .sorted()
                .toList();
    }

    public Long indexOfClass(String c){
        return (long) this.classes.indexOf(c);
    }

    public Long indexOfMethod(String m){
        return (long) this.methods.indexOf(m);
    }

    public Long indexOfVarAndField(String vf){
        return (long) this.varsAndFields.indexOf(vf);
    }

    /** get package from class */
    public Long packageFromClass(String className){
        int index = className.lastIndexOf('.');
        if(index == -1) {
            return (long) this.packages.indexOf(className);
        }
        return (long) this.packages.indexOf(className.substring(0, index));
    }

}
