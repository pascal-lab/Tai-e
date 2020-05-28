/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.analysis.ci;

import bamboo.pta.element.Obj;
import soot.jimple.AssignStmt;

import java.util.Comparator;

/**
 * Converts various pointer analysis elements to String.
 */
class Stringify {

    static String pointerToString(Pointer pointer) {
        if (pointer instanceof InstanceField) {
            InstanceField f = (InstanceField) pointer;
            return objToString(f.getBase()) + "." + f.getField().getName();
        } else {
            return pointer.toString();
        }
    }

    static String objToString(Obj obj) {
        StringBuilder sb = new StringBuilder();
        if (obj.getContainerMethod() != null) {
            sb.append(obj.getContainerMethod()).append('/');
        }
        Object allocation = obj.getAllocationSite();
        if (allocation instanceof AssignStmt) {
            AssignStmt alloc = (AssignStmt) allocation;
            sb.append(alloc.getRightOp())
                    .append('/')
                    .append(alloc.getJavaSourceStartLineNumber());
        } else {
            sb.append(allocation);
        }
        return sb.toString();
    }

    static String pointsToSetToString(PointsToSet pts) {
        Iterable<String> objs = () -> pts.stream()
                .map(Stringify::objToString)
                .sorted()
                .iterator();
        return String.join(",", objs);
    }
}
