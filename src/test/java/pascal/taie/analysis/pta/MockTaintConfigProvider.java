/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta;

import pascal.taie.analysis.pta.plugin.taint.CallSource;
import pascal.taie.analysis.pta.plugin.taint.FieldSource;
import pascal.taie.analysis.pta.plugin.taint.IndexRef;
import pascal.taie.analysis.pta.plugin.taint.ParamSanitizer;
import pascal.taie.analysis.pta.plugin.taint.ParamSource;
import pascal.taie.analysis.pta.plugin.taint.Sink;
import pascal.taie.analysis.pta.plugin.taint.Source;
import pascal.taie.analysis.pta.plugin.taint.TaintConfigProvider;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a mock taint configuration by merging content of these files:
 * <ul>
 *     <li><code>src/test/resources/pta/taint/taint-config.yml</code></li>
 *     <li><code>src/test/resources/pta/taint/taint-config-call-source.yml</code></li>
 *     <li><code>src/test/resources/pta/taint/taint-config-param-source.yml</code></li>
 * </ul>
 */
public class MockTaintConfigProvider extends TaintConfigProvider {

    // commonly-used index references
    private static final IndexRef VAR_BASE = new IndexRef(IndexRef.Kind.VAR, InvokeUtils.BASE, null);

    private static final IndexRef VAR_RESULT = new IndexRef(IndexRef.Kind.VAR, InvokeUtils.RESULT, null);

    private static final IndexRef VAR_0 = new IndexRef(IndexRef.Kind.VAR, 0, null);

    private static final IndexRef VAR_1 = new IndexRef(IndexRef.Kind.VAR, 1, null);

    private static final IndexRef VAR_2 = new IndexRef(IndexRef.Kind.VAR, 2, null);

    public MockTaintConfigProvider(ClassHierarchy hierarchy, TypeSystem typeSystem) {
        super(hierarchy, typeSystem);
    }

    @Override
    protected List<Source> sources() {
        List<Source> sources = new ArrayList<>();
        // { kind: call, method: "<SourceSink: java.lang.String source*(*{0+})>", index: result }
        matcher.getMethods("<SourceSink: java.lang.String source*(*{0+})>").forEach(method -> {
            Type type = method.getReturnType();
            sources.add(new CallSource(method, VAR_RESULT, type));
        });
        // { kind: field, field: "<SourceSink: * tainted*>" }
        // do not use matcher.getFields here to demonstrate an alternative approach
        JClass clz = hierarchy.getClass("SourceSink");
        if (clz != null) {
            for (JField f : clz.getDeclaredFields()) {
                if (f.getName().startsWith("tainted")) {
                    Type type = f.getType();
                    sources.add(new FieldSource(f, type));
                }
            }
        }
        // { kind: call, method: "<TaintCall: void varArg(java.lang.String)>", index: "0" }
        JMethod m = hierarchy.getMethod("<TaintCall: void varArg(java.lang.String)>");
        if (m != null) {
            Type type = m.getParamType(0);
            sources.add(new CallSource(m, VAR_0, type));
        }
        // { kind: call, method: "<TaintCall: void arrayArg(java.lang.String[])>", index: "0[*]" }
        m = hierarchy.getMethod("<TaintCall: void arrayArg(java.lang.String[])>");
        if (m != null) {
            IndexRef indexRef = new IndexRef(IndexRef.Kind.ARRAY, 0, null);
            Type type = m.getParamType(0);
            sources.add(new CallSource(m, indexRef, type));
        }
        // { kind: call, method: "<TaintCall: void fieldArg(A)>", index: 0.f }
        m = hierarchy.getMethod("<TaintCall: void fieldArg(A)>");
        JField f = hierarchy.getField("<A: java.lang.String f>");
        if (m != null && f != null) {
            IndexRef indexRef = new IndexRef(IndexRef.Kind.FIELD, 0, f);
            Type type = f.getType();
            sources.add(new CallSource(m, indexRef, type));
        }
        // { kind: call, method: "<TaintCall: java.lang.String[] source()>", index: "result[*]" }
        m = hierarchy.getMethod("<TaintCall: java.lang.String[] source()>");
        if (m != null) {
            IndexRef indexRef = new IndexRef(IndexRef.Kind.ARRAY, InvokeUtils.RESULT, null);
            Type type = m.getReturnType();
            sources.add(new CallSource(m, indexRef, type));
        }
        // { kind: param, method: "<TaintParam: void varParam(java.lang.String[],java.lang.String[])>", index: 0 }
        m = hierarchy.getMethod("<TaintParam: void varParam(java.lang.String[],java.lang.String[])>");
        if (m != null) {
            Type type = m.getParamType(0);
            sources.add(new ParamSource(m, VAR_0, type));
        }
        // { kind: param, method: "<TaintParam: void arrayParam(java.lang.String[])>", index: "0[*]" }
        m = hierarchy.getMethod("<TaintParam: void arrayParam(java.lang.String[])>");
        if (m != null) {
            IndexRef indexRef = new IndexRef(IndexRef.Kind.ARRAY, 0, null);
            Type type = ((ArrayType) m.getParamType(0)).elementType();
            sources.add(new ParamSource(m, indexRef, type));
        }
        // { kind: param, method: "<TaintParam: void fieldParam(A)>", index: 0.f }
        m = hierarchy.getMethod("<TaintParam: void fieldParam(A)>");
        f = hierarchy.getField("<A: java.lang.String f>");
        if (m != null && f != null) {
            IndexRef indexRef = new IndexRef(IndexRef.Kind.FIELD, 0, f);
            Type type = f.getType();
            sources.add(new ParamSource(m, indexRef, type));
        }
        return sources;
    }

    @Override
    protected List<Sink> sinks() {
        List<Sink> sinks = new ArrayList<>();
        // { method: "<SourceSink: void sink(java.lang.String)>", index: 0 }
        JMethod m = hierarchy.getMethod("<SourceSink: void sink(java.lang.String)>");
        if (m != null) {
            sinks.add(new Sink(m, VAR_0));
        }
        // { method: "<SourceSink: void sink(java.lang.String,int)>", index: 0 }
        m = hierarchy.getMethod("<SourceSink: void sink(java.lang.String,int)>");
        if (m != null) {
            sinks.add(new Sink(m, VAR_0));
        }
        // { method: "<SourceSink: void sink(java.lang.String,java.lang.String)>", index: 1 }
        m = hierarchy.getMethod("<SourceSink: void sink(java.lang.String,java.lang.String)>");
        if (m != null) {
            sinks.add(new Sink(m, VAR_1));
        }
        // { method: "<SourceSink: java.lang.String sourceAndSink(java.lang.String,java.lang.String)>", index: 0 }
        m = hierarchy.getMethod("<SourceSink: java.lang.String sourceAndSink(java.lang.String,java.lang.String)>");
        if (m != null) {
            sinks.add(new Sink(m, VAR_0));
        }
        // { method: "<TaintCall: void sink(java.lang.String)>", index: 0 }
        m = hierarchy.getMethod("<TaintCall: void sink(java.lang.String)>");
        if (m != null) {
            sinks.add(new Sink(m, VAR_0));
        }
        // { method: "<TaintParam: void sink(java.lang.String)>", index: 0 }
        m = hierarchy.getMethod("<TaintParam: void sink(java.lang.String)>");
        if (m != null) {
            sinks.add(new Sink(m, VAR_0));
        }
        // { method: "<TaintParam: void sink(java.lang.String[])>", index: 0 }
        m = hierarchy.getMethod("<TaintParam: void sink(java.lang.String[])>");
        if (m != null) {
            sinks.add(new Sink(m, VAR_0));
        }
        // { method: "<TaintParam: void sink(java.lang.String[])>", index: "0[*]" }
        m = hierarchy.getMethod("<TaintParam: void sink(java.lang.String[])>");
        if (m != null) {
            IndexRef indexRef = new IndexRef(IndexRef.Kind.ARRAY, 0, null);
            sinks.add(new Sink(m, indexRef));
        }
        // { method: "<TaintParam: void sink(A)>", index: 0.f }
        m = hierarchy.getMethod("<TaintParam: void sink(A)>");
        JField f = hierarchy.getField("<A: java.lang.String f>");
        if (m != null && f != null) {
            IndexRef indexRef = new IndexRef(IndexRef.Kind.FIELD, 0, f);
            sinks.add(new Sink(m, indexRef));
        }
        return sinks;
    }

    @Override
    protected List<TaintTransfer> transfers() {
        List<TaintTransfer> transfers = new ArrayList<>();
        // { method: "<java.lang.String: java.lang.String concat(java.lang.String)>", from: base, to: result }
        JMethod m = hierarchy.getMethod("<java.lang.String: java.lang.String concat(java.lang.String)>");
        if (m != null) {
            Type type = m.getReturnType();
            transfers.add(new TaintTransfer(m, VAR_BASE, VAR_RESULT, type));
        }
        // { method: "<java.lang.String: java.lang.String concat(java.lang.String)>", from: 0, to: result }
        m = hierarchy.getMethod("<java.lang.String: java.lang.String concat(java.lang.String)>");
        if (m != null) {
            Type type = m.getReturnType();
            transfers.add(new TaintTransfer(m, VAR_0, VAR_RESULT, type));
        }
        // { method: "<java.lang.String: char[] toCharArray()>", from: base, to: result }
        m = hierarchy.getMethod("<java.lang.String: char[] toCharArray()>");
        if (m != null) {
            Type type = m.getReturnType();
            transfers.add(new TaintTransfer(m, VAR_BASE, VAR_RESULT, type));
        }
        // { method: "<java.lang.String: void <init>(char[])>", from: 0, to: base }
        m = hierarchy.getMethod("<java.lang.String: void <init>(char[])>");
        if (m != null) {
            Type type = m.getDeclaringClass().getType();
            transfers.add(new TaintTransfer(m, VAR_0, VAR_BASE, type));
        }
        // { method: "<java.lang.String: void getChars(int,int,char[],int)>", from: base, to: 2 }
        m = hierarchy.getMethod("<java.lang.String: void getChars(int,int,char[],int)>");
        if (m != null) {
            Type type = m.getParamType(2);
            transfers.add(new TaintTransfer(m, VAR_BASE, VAR_2, type));
        }
        // { method: "<java.lang.AbstractStringBuilder^: * append(java.lang.Object^)>", from: 0, to: base }
        matcher.getMethods("<java.lang.AbstractStringBuilder^: * append(java.lang.Object^)>").forEach(method -> {
            Type type = method.getDeclaringClass().getType();
            transfers.add(new TaintTransfer(method, VAR_0, VAR_BASE, type));
        });
        // { method: "<java.lang.AbstractStringBuilder^: * toString()>", from: base, to: result }
        // do not use matcher.getMethods here to demonstrate an alternative approach
        for (JClass clz : hierarchy.getAllSubclassesOf(hierarchy
                .getClass("java.lang.AbstractStringBuilder"))) {
            for (JMethod method : clz.getDeclaredMethods()) {
                if (method.getName().equals("toString")
                        && method.getParamCount() == 0) {
                    Type type = method.getReturnType();
                    transfers.add(new TaintTransfer(method,
                            VAR_BASE, VAR_RESULT, type));
                }
            }
        }
        return transfers;
    }

    @Override
    protected List<ParamSanitizer> sanitizers() {
        List<ParamSanitizer> sanitizers = new ArrayList<>();
        // { kind: param, method: "<Sanitizer: java.lang.String sanitize(java.lang.String)>", index: 0 }
        JMethod m = hierarchy.getMethod("<Sanitizer: java.lang.String sanitize(java.lang.String)>");
        if (m != null) {
            sanitizers.add(new ParamSanitizer(m, 0));
        }
        // { kind: param, method: "<Sanitizer: Sanitizer sanitize()>", index: base }
        m = hierarchy.getMethod("<Sanitizer: Sanitizer sanitize()>");
        if (m != null) {
            sanitizers.add(new ParamSanitizer(m, InvokeUtils.BASE));
        }
        return sanitizers;
    }

}
