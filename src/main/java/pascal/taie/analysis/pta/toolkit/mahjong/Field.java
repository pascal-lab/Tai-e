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

package pascal.taie.analysis.pta.toolkit.mahjong;

import pascal.taie.language.classes.JField;
import pascal.taie.util.collection.Maps;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents edge labels of a field points-to graph, i.e., a JField or
 * a mock field that represents all array indexes.
 */
class Field {

    /**
     * When this is {@code null}, this Field represents array index.
     */
    @Nullable
    private final JField field;

    private Field(@Nullable JField field) {
        this.field = field;
    }

    @Override public String toString() {
        return field != null ? field.toString() : "@ARRAY-INDEX";
    }

    static class Factory {

        private static final Field ARRAY_INDEX = new Field(null);

        private final ConcurrentMap<JField, Field> fields = Maps.newConcurrentMap();

        Field get(JField field) {
            Objects.requireNonNull(field);
            return fields.computeIfAbsent(field, Field::new);
        }

        Field getArrayIndex() {
            return ARRAY_INDEX;
        }
    }
}
