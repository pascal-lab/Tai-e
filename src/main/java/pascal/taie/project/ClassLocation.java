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

package pascal.taie.project;

/**
 * A <em>ClassLocation</em> is a class location in the form of
 * <code>package1.package2.ClassName</code>.
 * It is used to represent the location of a class in the project.
 */
public class ClassLocation {

    private final String fullClassLocation;

    private int index;

    /**
     * Constructor for ClassLocation.
     * @param fullClassLocation the full class location in the form of
     *                          <code>package1.package2.ClassName</code>.
     */
    public ClassLocation(String fullClassLocation) {
        this.fullClassLocation = fullClassLocation;
        index = 0;
    }

    /**
     *
     * @return whether current location has next.
     * <br/>e.g. "pascal.taie.project.ClassLocation": true
     * <br/>     "ClassLocation": true
     * <br/>     "": false
     */
    boolean hasNext() {
        return index < fullClassLocation.length();
    }

    /**
     *
     * @return current level of location.
     * <br/>e.g. "pascal.taie.project.ClassLocation": "pascal"
     * <br/>     "ClassLocation": "ClassLocation"
     */
    String next() throws IndexOutOfBoundsException {
        if (index >= fullClassLocation.length()) {
            throw new IndexOutOfBoundsException();
        }
        int nextDot = fullClassLocation.indexOf('.', index);
        String result;
        if (nextDot != -1) {
            result = fullClassLocation.substring(index, nextDot);
            index = nextDot + 1;
        } else {
            result = fullClassLocation.substring(index);
            index = fullClassLocation.length();
        }
        return result;
    }
}
