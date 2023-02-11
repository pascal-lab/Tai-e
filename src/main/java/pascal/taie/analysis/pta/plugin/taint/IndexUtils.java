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

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

/**
 * Utility class to handle method indexes in taint analysis.
 */
final class IndexUtils {

    /**
     * Special number representing the base variable of an invocation.
     */
    static final int BASE = -1;

    /**
     * String representation of base variable.
     */
    private static final String BASE_STR = "base";

    /**
     * Special number representing the variable that receivers
     * the result of the invocation.
     */
    static final int RESULT = -2;

    /**
     * String representation of result variable.
     */
    private static final String RESULT_STR = "result";

    private IndexUtils() {
    }

    /**
     * Coverts string to index.
     */
    static int toInt(String s) {
        return switch (s.toLowerCase()) {
            case BASE_STR -> BASE;
            case RESULT_STR -> RESULT;
            default -> Integer.parseInt(s);
        };
    }

    /**
     * Converts index to string.
     */
    static String toString(int index) {
        return switch (index) {
            case BASE -> BASE_STR;
            case RESULT -> RESULT_STR;
            default -> Integer.toString(index);
        };
    }

    /**
     * Retrieves variable from a call site and index.
     */
    static Var getVar(Invoke callSite, int index) {
        InvokeExp invokeExp = callSite.getInvokeExp();
        return switch (index) {
            case BASE -> ((InvokeInstanceExp) invokeExp).getBase();
            case RESULT -> callSite.getResult();
            default -> invokeExp.getArg(index);
        };
    }
}
