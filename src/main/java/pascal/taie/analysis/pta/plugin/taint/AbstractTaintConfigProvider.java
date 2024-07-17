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

import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.SignatureMatcher;
import pascal.taie.language.type.TypeSystem;

import java.util.List;

import static java.util.Collections.unmodifiableList;


public abstract class AbstractTaintConfigProvider implements TaintConfigProvider {

    protected static final IndexRef IDX_VAR_BASE = new IndexRef(IndexRef.Kind.VAR, InvokeUtils.BASE, null);

    protected static final IndexRef IDX_VAR_RESULT = new IndexRef(IndexRef.Kind.VAR, InvokeUtils.RESULT, null);

    protected static final IndexRef IDX_VAR_0 = new IndexRef(IndexRef.Kind.VAR, 0, null);

    protected static final IndexRef IDX_VAR_1 = new IndexRef(IndexRef.Kind.VAR, 1, null);

    protected static final IndexRef IDX_VAR_2 = new IndexRef(IndexRef.Kind.VAR, 2, null);

    protected ClassHierarchy hierarchy;

    protected TypeSystem typeSystem;

    protected SignatureMatcher matcher;

    @Override
    public void initilize(ClassHierarchy hierarchy, TypeSystem typeSystem) {
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

    public TaintConfig taintConfig() {
        return new TaintConfig(unmodifiableList(sources()),
                unmodifiableList(sinks()), unmodifiableList(transfers()),
                unmodifiableList(sanitizers()), callSiteMode());
    }

}
