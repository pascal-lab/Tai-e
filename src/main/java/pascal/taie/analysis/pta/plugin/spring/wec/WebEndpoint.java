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

package pascal.taie.analysis.pta.plugin.spring.wec;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a concrete method that handlers HTTP requests.
 * @param containingClass
The class which contains {@link WebEndpoint#handlerMethod}.
This represents the concrete class that contains the method.
Note: the handler method may be implemented in the parent classes or this class.
 * @param handlerMethod
The concrete method that handles the HTTP request.
 * @param paths
The http request URIs (e.g. "/path/to/api") handled by this method.
Note: a handler method may correspond to multiple http URIs.
 * @param paramName2ParamIndex
Maps request parameter names (specified in annotations, e.g., @RequestParam) to their corresponding method parameter indices.
 */
public record WebEndpoint(JClass containingClass, JMethod handlerMethod, Set<String> paths,
                          Set<RequestMethod> requestMethods,
                          Map<String, Integer> paramName2ParamIndex) {

    public static String mergePath(String prefix, String path) {
        return Arrays.stream((prefix + "/" + path).split("/"))
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.joining("/", "/", ""));
    }

}
