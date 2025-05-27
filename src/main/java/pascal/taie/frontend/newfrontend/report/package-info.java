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

/**
 * This package contains the classes and interfaces for generating reports for the taie frontend.
 * <p>
 * It's only meant to be used by frontend developers and is not intended for use by end users.
 * <p>
 * Basically, it provides a {@link pascal.taie.frontend.newfrontend.report.FrontendStats}
 * class that contains all the statistics.
 * And a {@link pascal.taie.frontend.newfrontend.report.FrontendStatsResult} singleton class
 * that contains the {@link pascal.taie.frontend.newfrontend.report.FrontendStats} instance.
 * It also provides various dumping methods to dump the statistics and info to files.
 * <p>
 * A typical usage would be:
 * <pre>
 * {@code
 * Main.main(args);
 * FrontendStats stats = FrontendStatsResult.getStats();
 *
 * // dump the stats to the frontend-stats.yml file
 * FrontendStatsResult.dumpStats();
 * // dump the casting infos to the casting directory
 * FrontendStatsResult.dumpCastingInfo();
 * }
 * </pre>
 */
package pascal.taie.frontend.newfrontend.report;
