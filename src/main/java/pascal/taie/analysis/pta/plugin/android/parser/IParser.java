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



package pascal.taie.analysis.pta.plugin.android.parser;

import soot.SootMethod;

import java.util.Set;

/**
 * Interface of parsering binary application or library
 *
 */
public interface IParser {

    /**
     * access analyze target application name
     * @return
     */
    String getName();

    /**
     * access analyze target application version
     * @return
     */
    String getVersion();

    /**
     * start parse target application with owner implementation
     * @return
     */
    void parse();

    /**
     * access analyze target application size
     * @return
     */
    long getSize();


    /**
     * access analyze target application entries
     * include direct entries and implicit entries
     * @return
     */
    Set<SootMethod> getEntries();
}
