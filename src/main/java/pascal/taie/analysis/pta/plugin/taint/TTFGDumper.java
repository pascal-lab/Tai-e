package pascal.taie.analysis.pta.plugin.taint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import pascal.taie.util.collection.Sets;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class TTFGDumper {

    public final Set<String> sourceNodes;

    public final Set<String> sinkNodes;

    public final Set<String> nodes;

    public Set<VisualEdge> edges = Sets.newHybridSet();

    public TTFGDumper(TTaintFlowGraph ttfg){
        this.sourceNodes = ttfg.getSourceNodes().stream()
                .map(Object::toString).collect(Collectors.toSet());
        this.sinkNodes = ttfg.getSinkNodes().stream()
                .map(Object::toString).collect(Collectors.toSet());
        this.nodes = ttfg.getNodes().stream()
                .map(Object::toString).collect(Collectors.toSet());
        edges.addAll(ttfg.getEdges().stream()
                .map(edge -> new VisualEdge(edge.source().toString(), edge.target().toString(), edge.from(), edge.to()))
                .collect(Collectors.toSet()));
    }

    public void dump(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR));
        mapper.writeValue(file, this);
    }

    record VisualEdge(String source, String target, long from, long to) {

    }
}


