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

package pascal.taie.analysis.pta.core.heap;

import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Predicate for checking whether given string constants are
 * in application code.
 */
public class IsApplicationString implements Predicate<String> {

    private final Set<String> stringsInApp;

    public IsApplicationString() {
        stringsInApp = World.get().getClassHierarchy()
                .applicationClasses()
                .map(JClass::getDeclaredMethods)
                .flatMap(Collection::stream)
                .filter(Predicate.not(JMethod::isAbstract))
                .map(JMethod::getIR)
                .map(IR::getVars)
                .flatMap(Collection::stream)
                .filter(v -> v.isConst() && v.getConstValue() instanceof StringLiteral)
                .map(v -> ((StringLiteral) v.getConstValue()).getString())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean test(String s) {
        return stringsInApp.contains(s);
    }
}
