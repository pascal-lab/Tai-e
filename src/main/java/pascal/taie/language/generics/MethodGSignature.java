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


package pascal.taie.language.generics;

import pascal.taie.util.Experimental;

import java.io.Serializable;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * In <a href="https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-MethodSignature">
 * JVM Spec. 4.7.9.1 MethodSignature</a>,
 * a <i>method signature</i> encodes type information about a (possibly generic) method declaration.
 * It describes any type parameters of the method; the (possibly parameterized) types of
 * any formal parameters; the (possibly parameterized) return type, if any;
 * and the types of any exceptions declared in the method's throws clause.
 */
public final class MethodGSignature implements Serializable {

    private final List<TypeParameter> typeParams;

    private final List<TypeGSignature> parameterSigs;

    private final TypeGSignature resultSignature;

    private final List<TypeGSignature> throwsSigs;

    MethodGSignature(List<TypeParameter> typeParams,
                     List<TypeGSignature> parameterSigs,
                     TypeGSignature resultSignature,
                     List<TypeGSignature> throwsSigs) {
        this.typeParams = List.copyOf(typeParams);
        this.parameterSigs = List.copyOf(parameterSigs);
        this.resultSignature = resultSignature;
        this.throwsSigs = List.copyOf(throwsSigs);
    }

    @Experimental
    public List<TypeParameter> getTypeParams() {
        return typeParams;
    }

    @Experimental
    public List<TypeGSignature> getParameterSigs() {
        return parameterSigs;
    }

    @Experimental
    public TypeGSignature getResultSignature() {
        return resultSignature;
    }

    @Experimental
    public List<TypeGSignature> getThrowsSigs() {
        return throwsSigs;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(" ");
        if (!typeParams.isEmpty()) {
            joiner.add(typeParams.stream().map(TypeParameter::toString)
                    .collect(Collectors.joining(", ", "<", ">")));
        }
        joiner.add(resultSignature.toString());
        joiner.add(parameterSigs.stream().map(TypeGSignature::toString)
                .collect(Collectors.joining(", ", "(", ")")));
        if (!throwsSigs.isEmpty()) {
            joiner.add("throws");
            joiner.add(throwsSigs.stream().map(TypeGSignature::toString)
                    .collect(java.util.stream.Collectors.joining(", ")));
        }
        return joiner.toString();
    }

}
