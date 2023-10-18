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

package pascal.taie.analysis.bugfinder.security;

import pascal.taie.analysis.bugfinder.BugType;

public class SecurityBugType implements BugType {

    private final String bugDescription;

    public static SecurityBugType getBugType(String bugDescription) {
        return new SecurityBugType(bugDescription.intern());
    }

    public SecurityBugType(String bugDescription) {
        this.bugDescription = bugDescription;
    }

    @Override
    public String toString() {
        return bugDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof SecurityBugType that)) {return false;}
        return bugDescription.equals(that.bugDescription);
    }

    @Override
    public int hashCode() {
        return bugDescription.hashCode();
    }
}
