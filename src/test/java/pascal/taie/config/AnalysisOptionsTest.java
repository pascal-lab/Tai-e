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

package pascal.taie.config;

import org.junit.jupiter.api.Test;
import pascal.taie.util.collection.Maps;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AnalysisOptionsTest {

    @Test
    void testNullOption() {
        AnalysisOptions options = new AnalysisOptions(getOptionValues());
        assertNull(options.get("z"));
    }

    @Test
    void testNonExistOption() {
        assertThrows(ConfigException.class, () -> {
            AnalysisOptions options = new AnalysisOptions(getOptionValues());
            options.get("non-exist-key");
        });
    }

    private Map<String, Object> getOptionValues() {
        Map<String, Object> values = Maps.newMap();
        values.put("x", 100);
        values.put("y", "666");
        values.put("z", null);
        return values;
    }
}
