/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.dataflow.lattice;

import soot.tagkit.Tag;

import java.util.Map;

/**
 * The tag that stores the result of data-flow analysis for each method.
 */
public class DataFlowTag<Node, Domain> implements Tag {

    private final String name;

    private final Map<Node, Domain> dataFlowMap;

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
