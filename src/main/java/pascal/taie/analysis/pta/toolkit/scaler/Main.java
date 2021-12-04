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

package pascal.taie.analysis.pta.toolkit.scaler;

import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.util.Timer;

/**
 * Temporary main class of Scaler.
 * This class is for testing purpose, and will be removed after testing Scaler.
 */
@Deprecated
public class Main {

    public static void main(String[] args) {
        pascal.taie.Main.main(args);
        Timer.runAndCount(() -> {
            Scaler scaler = new Scaler(
                    World.getResult(PointerAnalysis.ID));
            scaler.selectContext()
                    .entrySet()
                    .stream()
                    .sorted((e1, e2) -> {
                        int cmp1 = e1.getValue().compareTo(e2.getValue());
                        if (cmp1 != 0) {
                            return cmp1;
                        } else {
                            return e1.getKey().toString()
                                    .compareTo(e2.getKey().toString());
                        }
                    })
                    .forEach(System.out::println);
        }, "Scaler");
    }
}
