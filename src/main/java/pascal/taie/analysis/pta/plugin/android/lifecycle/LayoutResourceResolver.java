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

package pascal.taie.analysis.pta.plugin.android.lifecycle;

import pascal.taie.android.info.ApkInfo;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.Sets;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolves Android layout- and resource-related metadata used by
 * {@link LayoutModel}.
 */
final class LayoutResourceResolver {

    private static final String DEFAULT_PACKAGE_RESOURCE_SEPARATOR = ".";

    private static final String BUTTON_RESOURCE_NAME = "button";

    private final ApkInfo apkInfo;

    private final ClassHierarchy hierarchy;

    LayoutResourceResolver(ApkInfo apkInfo, ClassHierarchy hierarchy) {
        this.apkInfo = apkInfo;
        this.hierarchy = hierarchy;
    }

    Integer getIntegerArgument(Invoke invoke, int index) {
        Var arg = invoke.getInvokeExp().getArg(index);
        if (arg.isConst() && arg.getConstValue() instanceof IntLiteral intLiteral) {
            return intLiteral.getValue();
        }
        return null;
    }

    String getStringResource(Integer id) {
        if (id == null) {
            return null;
        }

        ARSCFileParser.AbstractResource resource = apkInfo.findResource(id);
        return resource instanceof ARSCFileParser.StringResource stringResource
                ? stringResource.getValue()
                : null;
    }

    Set<String> resolveLayoutFileNames(Integer layoutId) {
        String layoutFileName = getStringResource(layoutId);
        if (layoutFileName != null) {
            return Set.of(layoutFileName);
        }

        return Stream.of(
                        apkInfo.layoutCallbacks().keySet(),
                        apkInfo.layoutFragments().keySet(),
                        apkInfo.layoutViews().keySet())
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(Sets::newSet));
    }

    LayoutLookup resolveComponentLookup(Integer resourceId) {
        if (resourceId == null) {
            return LayoutLookup.empty();
        }

        String className = getStringResource(resourceId);
        if (className != null) {
            return new LayoutLookup(hierarchy.getClass(className), false);
        }

        ARSCFileParser.AbstractResource resource = apkInfo.findResource(resourceId);
        if (resource == null) {
            return LayoutLookup.empty();
        }

        String resourceName = resource.getResourceName();
        String inferredClassName = apkInfo.getPackageName()
                + DEFAULT_PACKAGE_RESOURCE_SEPARATOR
                + toCamelCase(resourceName);
        return new LayoutLookup(
                hierarchy.getClass(inferredClassName),
                BUTTON_RESOURCE_NAME.equals(resourceName));
    }

    private static String toCamelCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean convertNext = true;
        for (char ch : input.toCharArray()) {
            if (ch == '_') {
                convertNext = true;
            } else if (convertNext) {
                result.append(Character.toUpperCase(ch));
                convertNext = false;
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    record LayoutLookup(JClass componentClass, boolean requiresSyntheticResult) {

        private static LayoutLookup empty() {
            return new LayoutLookup(null, false);
        }
    }
}
