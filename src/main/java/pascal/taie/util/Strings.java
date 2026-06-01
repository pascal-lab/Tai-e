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

package pascal.taie.util;

/**
 * Static utility methods for {@link String}.
 */
public final class Strings {

    private Strings() {
    }

    /**
     * Capitalizes a {@link String}, changing the first letter to upper case
     * as per {@link Character#toUpperCase(char)}. No other letters are changed.
     *
     * @param str the {@link String} to capitalize
     * @return the capitalized {@link String}
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        char oldFirst = str.charAt(0);
        char newFirst = Character.toUpperCase(oldFirst);
        if (oldFirst == newFirst) {
            return str;
        }
        char[] chars = str.toCharArray();
        chars[0] = newFirst;
        return new String(chars);
    }

    /**
     * Escapes special characters in a given string by replacing them with
     * their corresponding escape sequences.
     * If the input string is null or empty, the method returns it unchanged.
     *
     * @param str the input string to be escaped
     * @return a string with special characters replaced by their corresponding
     *         escape sequences, or the original string if it is null or empty
     */
    public static String escape(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\\' -> buffer.append("\\\\");
                case '\"' -> buffer.append("\\\"");
                case '\b' -> buffer.append("\\b");
                case '\t' -> buffer.append("\\t");
                case '\n' -> buffer.append("\\n");
                case '\f' -> buffer.append("\\f");
                case '\r' -> buffer.append("\\r");
                default -> buffer.append(c);
            }
        }
        return buffer.toString();
    }
}
