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

package pascal.taie.android.config;

import pascal.taie.config.AnalysisOptions;
import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class AndroidPTAOptions extends AnalysisOptions {

    public AndroidPTAOptions() {
        super(createOptions());
    }

    private static Map<String, Object> createOptions() {
        Map<String, Object> options = Maps.newMap();
        options.put("implicit-entries", false);
        options.put("distinguish-string-constants", "app");
        options.put("propagate-types", new ArrayList<>(
                Arrays.asList("reference,int,long,double,char,float"
                        .split(","))));
        return options;
    }

    public void apply(AnalysisOptions options) {
        options.update(this);
    }
}
