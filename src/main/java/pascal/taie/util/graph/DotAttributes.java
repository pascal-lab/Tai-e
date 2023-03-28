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

package pascal.taie.util.graph;

import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Represents dot attributes.
 */
public class DotAttributes {

    private static final DotAttributes EMPTY = new DotAttributes(Maps.newMultiMap());

    /**
     * Stores attributes, i.e,. name-value pairs.
     */
    private final MultiMap<String, String> attrs;

    /**
     * The string representation of attributes of this object that Dot can recognize.
     */
    private final String attrsString;

    private DotAttributes(MultiMap<String, String> attrs) {
        this.attrs = attrs;
        this.attrsString = toString(attrs);
    }

    /**
     * Converts attributes in given multimap to Dot-recognizable string.
     */
    private static String toString(MultiMap<String, String> attrs) {
        StringJoiner joiner = new StringJoiner(",");
        attrs.keySet().forEach(name -> {
            Set<String> values = attrs.get(name);
            if (values.size() == 1) {
                joiner.add(name + '=' + CollectionUtils.getOne(values));
            } else {
                String value = values.stream()
                        .collect(Collectors.joining(",", "\"", "\""));
                joiner.add(name + '=' + value);
            }
        });
        return joiner.toString();
    }

    /**
     * @return a new {@link DotAttributes} with attributed updated by given input.
     */
    public DotAttributes update(String... input) {
        if ((input.length & 1) != 0) { // implicit nullcheck of input
            throw new IllegalArgumentException("input.length should be even");
        }
        MultiMap<String, String> newAttrs = Maps.newMultiMap();
        for (int i = 0; i < input.length; i += 2) {
            newAttrs.put(input[i], input[i + 1]);
        }
        attrs.keySet().forEach(name -> {
            if (!newAttrs.containsKey(name)) {
                newAttrs.putAll(name, attrs.get(name));
            }
        });
        return new DotAttributes(newAttrs);
    }

    /**
     * @return a new {@link DotAttributes} with attributed in given input added.
     */
    public DotAttributes add(String... input) {
        if ((input.length & 1) != 0) { // implicit nullcheck of input
            throw new IllegalArgumentException("input.length should be even");
        }
        MultiMap<String, String> newAttrs = Maps.newMultiMap();
        newAttrs.putAll(attrs);
        for (int i = 0; i < input.length; i += 2) {
            newAttrs.put(input[i], input[i + 1]);
        }
        return new DotAttributes(newAttrs);
    }

    /**
     * @return a {@link DotAttributes} containing attributes specified by input.
     */
    public static DotAttributes of(String... input) {
        return input.length == 0 ? EMPTY : EMPTY.add(input);
    }

    @Override
    public String toString() {
        return attrsString;
    }
}
