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

package pascal.taie.analysis.bugfinder.security.insecureapi;

import pascal.taie.analysis.bugfinder.BugType;
import pascal.taie.analysis.bugfinder.Severity;
import pascal.taie.analysis.bugfinder.security.SecurityBugInfo;

import java.util.Objects;
import java.util.Set;

class InsecureAPIBugInfo extends SecurityBugInfo {
    private final Set<InsecureAPI> insecureAPISet;

    InsecureAPIBugInfo(BugType bugType, Severity severity,
                       String description, Set<InsecureAPI> insecureAPISet) {
        super(bugType, severity, description, null);
        this.insecureAPISet = insecureAPISet;
    }

    public Set<InsecureAPI> getInsecureAPISet() {
        return insecureAPISet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof InsecureAPIBugInfo that)) {return false;}
        if (!super.equals(o)) {return false;}
        return Objects.equals(insecureAPISet, that.insecureAPISet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), insecureAPISet);
    }
}
