/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.util;

import soot.LabeledUnitPrinter;
import soot.Unit;
import soot.jimple.GotoStmt;

public class JimpleUtils {

    private JimpleUtils() {}

    /**
     * Convert an Unit to its String representation.
     */
    public static String unitToString(LabeledUnitPrinter up, Unit unit) {
        StringBuilder sb = new StringBuilder();
        sb.append("L" + unit.getJavaSourceStartLineNumber()).append("{");
        String label = up.labels().get(unit);
        if (label != null) {
            sb.append(label).append(": ");
        }
        if (unit instanceof GotoStmt) {
            sb.append("goto ")
                    .append(up.labels().get(((GotoStmt) unit).getTarget()));
        } else {
            sb.append(unit);
        }
        sb.append("}");
        return sb.toString();
    }
}
