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

package pascal.taie.language.type;

/**
 * This type means that the expression, e.g., a variable, is untyped (i.e., has no type).
 * Usually, it should not appear in IR, however, currently Tai-e uses Soot as front end
 * which fails to type some variables, thus it stays in IR for some cases.
 */
public enum BottomType implements Type {

    BOTTOM;

    @Override
    public String getName() {
        return "bottom-type";
    }

    @Override
    public String toString() {
        return getName();
    }
}
