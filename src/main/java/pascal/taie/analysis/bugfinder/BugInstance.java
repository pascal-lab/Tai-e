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

package pascal.taie.analysis.bugfinder;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.Objects;

// TODO: refactor it with more precise context information.
public class BugInstance implements Comparable<BugInstance> {

    private final BugType type;

    private final Severity severity;

    private final JClass jClass;

    private final JMethod jMethod;

    private int sourceLineStart = -1;

    private int sourceLineEnd = -1;

    public BugInstance(BugType type, Severity severity, JClass jClass) {
        this.type = type;
        this.severity = severity;
        this.jClass = jClass;
        this.jMethod = null;
    }

    public BugInstance(BugType type, Severity severity, JMethod jMethod) {
        this.type = type;
        this.severity = severity;
        this.jClass = jMethod.getDeclaringClass();
        this.jMethod = jMethod;
    }

    public BugType getType() {
        return type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public BugInstance setSourceLine(int start, int end) {
        sourceLineStart = start;
        sourceLineEnd = end;
        return this;
    }

    public BugInstance setSourceLine(int num) {
        return setSourceLine(num, num);
    }

    public int getSourceLineStart() {
        return sourceLineStart;
    }

    public int getSourceLineEnd() {
        return sourceLineEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BugInstance bugInstance)) {
            return false;
        }
        return type.equals(bugInstance.type)
                && Objects.equals(jClass, bugInstance.jClass)
                && Objects.equals(jMethod, bugInstance.jMethod)
                && sourceLineStart == bugInstance.sourceLineStart
                && sourceLineEnd == bugInstance.sourceLineEnd;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, jClass, jMethod, sourceLineStart, sourceLineEnd);
    }

    @Override
    public String toString() {
        String sourceLineRange = "null";
        if (sourceLineStart >= 0) {
            sourceLineRange = sourceLineStart == sourceLineEnd
                    ? String.valueOf(sourceLineStart)
                    : sourceLineStart + "---" + sourceLineEnd;
        }
        return String.format("Class: %s, Method: %s, LineNumber: %s, BugType: %s, Severity: %s",
                jClass, jMethod, sourceLineRange, type, severity);
    }

    @Override
    public int compareTo(BugInstance o) {
        if (jClass.equals(o.jClass)) {
            return Integer.compare(sourceLineStart, o.sourceLineStart);
        } else {
            return jClass.toString().compareTo(o.jClass.toString());
        }
    }
}
