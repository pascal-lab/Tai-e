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

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.SignatureMatcher;
import pascal.taie.language.type.TypeSystem;

import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * Provides a configuration for taint analysis.
 * Subclasses should implement the constructor {@link #TaintConfigProvider(ClassHierarchy, TypeSystem)}
 * and override the necessary methods to provide specific configurations.
 */
public abstract class TaintConfigProvider {

    protected final ClassHierarchy hierarchy;

    protected final TypeSystem typeSystem;

    protected final SignatureMatcher matcher;

    protected TaintConfigProvider(ClassHierarchy hierarchy, TypeSystem typeSystem) {
        this.hierarchy = hierarchy;
        this.typeSystem = typeSystem;
        this.matcher = new SignatureMatcher(hierarchy);
    }

    protected List<Source> sources() {
        return List.of();
    }

    protected List<Sink> sinks() {
        return List.of();
    }

    protected List<TaintTransfer> transfers() {
        return List.of();
    }

    protected List<ParamSanitizer> sanitizers() {
        return List.of();
    }

    protected boolean callSiteMode() {
        return false;
    }

    public TaintConfig get() {
        return new TaintConfig(unmodifiableList(sources()),
                unmodifiableList(sinks()), unmodifiableList(transfers()),
                unmodifiableList(sanitizers()), callSiteMode());
    }

}
