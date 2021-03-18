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

package pascal.taie.analysis.oldpta.plugin;

import pascal.taie.World;
import pascal.taie.analysis.pta.PTAOptions;

public class Preprocessor implements Plugin {

    @Override
    public void preprocess() {
        if (PTAOptions.get().isPreBuildIR()) {
            World.getIRBuilder()
                    .buildAllPTA(World.getClassHierarchy());
        }
    }
}
