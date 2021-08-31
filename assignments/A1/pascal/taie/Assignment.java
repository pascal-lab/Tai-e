/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main class for assignments.
 */
public class Assignment {

    public static void main(String[] args) {
        List<String> argList = new ArrayList<>();
        Collections.addAll(argList, "-pp", "-p", "plan.yml");
        Collections.addAll(argList, args);
        Main.main(argList.toArray(new String[0]));
    }
}
