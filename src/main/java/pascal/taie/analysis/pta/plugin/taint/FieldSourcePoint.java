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

import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.language.classes.JMethod;

import javax.annotation.Nonnull;
import java.util.Comparator;

public record FieldSourcePoint(JMethod container, LoadField loadField)
        implements SourcePoint {

    private static final Comparator<FieldSourcePoint> COMPARATOR =
            Comparator.comparing((FieldSourcePoint fsp) -> fsp.container.toString())
                    .thenComparingInt(fsp -> fsp.loadField().getIndex());

    @Override
    public int compareTo(@Nonnull SourcePoint sp) {
        if (sp instanceof FieldSourcePoint fsp) {
            return COMPARATOR.compare(this, fsp);
        }
        return SourcePoint.compare(this, sp);
    }

    @Override
    public JMethod getContainer() {
        return container;
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public String toString() {
        return container + " " + IRPrinter.position(loadField) + " " + loadField;
    }
}
