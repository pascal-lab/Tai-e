package bamboo.dataflow.lattice;

import soot.tagkit.Tag;

import java.util.Map;

/**
 * The tag that stores the result of data-flow analysis for each method.
 */
public class DataFlowTag<Node, Domain> implements Tag {

    private String name;

    private Map<Node, Domain> dataFlowMap;

    public DataFlowTag(String name, Map<Node, Domain> dataFlowMap) {
        this.name = name;
        this.dataFlowMap = dataFlowMap;
    }

    public Map<Node, Domain> getDataFlowMap() {
        return dataFlowMap;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] getValue() {
        throw new RuntimeException("DataFlowTag has no value for bytecode");
    }
}
