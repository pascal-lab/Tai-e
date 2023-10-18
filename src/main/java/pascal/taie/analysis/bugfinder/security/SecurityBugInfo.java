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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import pascal.taie.analysis.bugfinder.BugType;
import pascal.taie.analysis.bugfinder.Severity;

import javax.annotation.Nullable;
import java.util.Objects;

public class SecurityBugInfo {
    private final BugType bugType;
    private final Severity severity;
    private final String description;
    private final String configPath;

    public SecurityBugInfo(BugType bugType, Severity severity, String description, @Nullable String configPath) {
        this.bugType = bugType;
        this.severity = severity;
        this.description = description;
        this.configPath = configPath;
    }

    @JsonCreator
    public SecurityBugInfo(@JsonProperty("bugType") String bugType,
                           @JsonProperty("severity") String severity,
                           @JsonProperty("description") String description,
                           @JsonProperty("configPath") String configPath) {
        this.bugType = SecurityBugType.getBugType(bugType);
        this.severity = Severity.valueOf(severity);
        this.description = description;
        this.configPath = configPath;
    }

    public BugType getBugType() {
        return bugType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    @Nullable
    public String getConfigPath() {
        return configPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        SecurityBugInfo bugInfo = (SecurityBugInfo) o;
        return bugType.equals(bugInfo.bugType)
                && severity == bugInfo.severity
                && description.equals(bugInfo.description)
                && Objects.equals(configPath, bugInfo.configPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bugType, severity, description, configPath);
    }
}
