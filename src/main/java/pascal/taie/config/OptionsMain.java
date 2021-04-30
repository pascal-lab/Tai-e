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

package pascal.taie.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class OptionsMain {

    public static void main(String[] args) throws IOException {
        File optFile = new File("output/options.yml");
        System.out.println(Options.readFromFile(optFile));
        Options opts2 = Options.parse("-cp", "a/b/c", "-v");
        Options.writeToFile(opts2, new File("output/options.yml"));
    }
}
