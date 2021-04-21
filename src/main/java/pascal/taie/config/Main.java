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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File config = new File(classLoader.getResource("tai-e-analyses.yml").getFile());
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ConfigItem[] items = mapper.readValue(config, ConfigItem[].class);
        Arrays.stream(items).forEach(System.out::println);
        PassManager manager = new PassManager(items);
        manager.passes().forEach(System.out::println);
    }
}
