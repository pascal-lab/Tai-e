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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Map;

public class OptionsTest {

    @Test
    public void testHelp() {
        Options options = Options.parse("--help");
        if (options.isPrintHelp()) {
            options.printHelp();
        }
    }

    @Test
    public void testVersion() {
        Options options = Options.parse("-v");
        if (options.isPrintVersion()) {
            options.printVersion();
        }
    }

    @Test
    public void testJavaVersion() {
        Options options = Options.parse("-java=8");
        Assert.assertEquals(options.getJavaVersion(), 8);
    }

    @Test
    public void testPrependJVM() {
        Options options = Options.parse("-pp");
        Assert.assertEquals(Options.getCurrentJavaVersion(),
                options.getJavaVersion());
    }

    @Test
    public void testOptions() {
        Options options = Options.parse("--no-implicit-entries", "-cs", "2-object");
        Assert.assertFalse(options.analyzeImplicitEntries());
        Assert.assertTrue(options.isMergeStringBuilders());
        Assert.assertEquals("2-object", options.getContextSensitivity());
        options = Options.parse("--no-merge-string-builders");
        Assert.assertFalse(options.isMergeStringBuilders());
    }

    @Test
    public void testOutputFile() {
        // Well, README will never be output file,
        // this is just for testing ...
        Options options = Options.parse("-f", "README");
        Assert.assertEquals(new File("README"),
                options.getOutputFile());
    }

    @Test
    public void testMainClass() {
        Options options = Options.parse("-cp", "path/to/cp", "-m", "Main");
        Assert.assertEquals("Main", options.getMainClass());
    }

    @Test
    public void testOptions2() throws JsonProcessingException {
        Options2 options = Options2.parse(
                "-a", "cfg=exception:true,scope:inter",
                "-a", "pta=timeout:1800,merge-string-objects:true,cs:2-obj");
        System.out.println(options.getAnalyses());
        String pta = options.getAnalyses().get("pta");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType type = mapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> args = mapper.readValue(pta, type);
        System.out.println(args);
    }
}
