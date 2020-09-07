/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.dataflow.lattice;

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
