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

package pascal.taie.analysis.pta.core.cs.selector;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.ConfigException;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Strings;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Provides static factory methods for various context selectors.
 */
public class ContextSelectorFactory {

    /**
     * @return selector for context insensitivity.
     */
    public static ContextSelector makeCISelector() {
        return new ContextInsensitiveSelector();
    }

    /**
     * @return a context selector for given context sensitivity variant.
     * The returned selector applies the same variant for all methods.
     */
    public static ContextSelector makePlainSelector(String cs) {
        if (cs.equals("ci")) {
            return new ContextInsensitiveSelector();
        } else {
            try {
                // we expect that the argument of context-sensitivity variant
                // is of pattern k-kind, where k is limit of context length
                // and kind represents kind of context element (obj, type, etc.).
                String[] splits = cs.split("-");
                int k = Integer.parseInt(splits[0]);
                String kind = Strings.capitalize(splits[1]);
                int hk;
                if (splits.length < 3) { // if limit of heap contexts is not given,
                    // then we use k -1 as default limit
                    hk = k - 1;
                } else { // we expect that splits[2] is "k'h"
                    hk = Integer.parseInt(splits[2].replace("h", ""));
                }
                String selectorName = ContextSelectorFactory.class.getPackageName() +
                        ".K" + kind + "Selector";
                Class<?> c = Class.forName(selectorName);
                Constructor<?> ctor = c.getConstructor(int.class, int.class);
                return (ContextSelector) ctor.newInstance(k, hk);
            } catch (RuntimeException e) {
                throw new ConfigException("Unexpected context-sensitivity variants: " + cs, e);
            } catch (ClassNotFoundException | NoSuchMethodException |
                    InvocationTargetException | InstantiationException |
                    IllegalAccessException e) {
                throw new ConfigException("Failed to initialize context selector: " + cs, e);
            }
        }
    }

    /**
     * @return a selective context selector which applies given context sensitivity
     * variant (specified by cs) to set of methods (specified by csMethods),
     * and cs to all objects.
     */
    public static ContextSelector makeSelectiveSelector(
            String cs, Set<JMethod> csMethods) {
        return makeSelectiveSelector(cs, csMethods::contains, o -> true);
    }

    /**
     * @return a selective context selector which applies given context sensitivity
     * variant (specified by cs) to part of methods (specified by isCSMethod)
     * and part of objects (specified by isCSObj).
     */
    public static ContextSelector makeSelectiveSelector(
            String cs, Predicate<JMethod> isCSMethod, Predicate<Obj> isCSObj) {
        return new SelectiveSelector(makePlainSelector(cs), isCSMethod, isCSObj);
    }

    /**
     * @return a guided context selector which applies the context sensitivity
     * variants to the methods according to given map.
     */
    public static ContextSelector makeGuidedSelector(Map<JMethod, String> csMap) {
        return new GuidedSelector(csMap);
    }
}
