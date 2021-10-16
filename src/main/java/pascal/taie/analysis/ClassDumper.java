/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.Configs;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.Modifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dumps out classes.
 */
public class ClassDumper extends InterproceduralAnalysis {

    private static final Logger logger = LogManager.getLogger(ClassDumper.class);

    public static final String ID = "class-dumper";

    private static final String SUFFIX = ".tir";

    private static final String INDENT = "    ";

    public ClassDumper(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Object analyze() {
        List<JClass> classes = World.getClassHierarchy()
                .applicationClasses()
                .collect(Collectors.toUnmodifiableList());
        logger.info("Dumping {} classes to {} ...",
                classes.size(), Configs.getOutputDir());
        classes.parallelStream()
                .forEach(c -> new Dumper(c).dump());
        return null;
    }

    private static class Dumper {

        private final JClass jclass;

        private PrintStream out;

        private Dumper(JClass jclass) {
            this.jclass = jclass;
        }

        private void dump() {
            String fileName = jclass.getName() + SUFFIX;
            try (PrintStream out = new PrintStream(new FileOutputStream(
                    new File(Configs.getOutputDir(), fileName)))) {
                this.out = out;
                dumpClassDeclaration();
                out.println(" {");
                out.println();
                jclass.getDeclaredFields().forEach(this::dumpField);
                out.println();
                out.println("}");
            } catch (FileNotFoundException e) {
                logger.warn("Failed to dump class {}, caused by {}", jclass, e);
            }
        }

        private void dumpClassDeclaration() {
            // dump class modifiers
            jclass.getModifiers()
                    .stream()
                    // if jclass is an interface, then don't dump modifieres
                    // "interface" and "abstract"
                    .filter(m -> !jclass.isInterface() ||
                            (m != Modifier.INTERFACE && m != Modifier.ABSTRACT))
                    .forEach(m -> out.print(m + " "));
            if (jclass.isInterface()) {
                out.print("interface");
            } else {
                out.print("class");
            }
            out.print(' ');
            out.print(jclass.getName());
            JClass superClass = jclass.getSuperClass();
            if (superClass != null) {
                out.print(" extends ");
                out.print(superClass.getName());
            }
        }

        private void dumpField(JField field) {
            out.print(INDENT);
            dumpModifiers(field.getModifiers());
            out.printf("%s %s;%n", field.getType().getName(), field.getName());
            // TODO: handle field initialization (constant expression)
        }

        private void dumpModifiers(Set<Modifier> mods) {
            mods.forEach(m -> out.print(m + " "));
        }
    }
}
