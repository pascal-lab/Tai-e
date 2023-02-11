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

package pascal.taie.analysis.misc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.ClassAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.Configs;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.type.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Dumps Tai-e IR for classes of input program.
 */
public class IRDumper extends ClassAnalysis<Void> {

    public static final String ID = "ir-dumper";

    private static final Logger logger = LogManager.getLogger(IRDumper.class);

    private static final String SUFFIX = ".tir";

    private static final String INDENT = "    ";

    /**
     * Directory to dump IR.
     */
    private final File dumpDir;

    public IRDumper(AnalysisConfig config) {
        super(config);
        String path = getOptions().getString("dump-dir");
        dumpDir = path != null ? new File(path) : Configs.getOutputDir();
        if (!dumpDir.exists()) {
            dumpDir.mkdirs();
        }
        try {
            logger.info("Dump directory: {}", dumpDir.getCanonicalPath());
        } catch (IOException e) { // would this happen after dumpDir.mkdirs()?
            logger.warn("Failed to get canonical path of dump-dir", e);
            logger.info("Dump directory: {}", dumpDir.getAbsolutePath());
        }
    }

    @Override
    public Void analyze(JClass jclass) {
        new Dumper(dumpDir, jclass).dump();
        return null;
    }

    private static class Dumper {

        private final File dumpDir;

        private final JClass jclass;

        private PrintStream out;

        private Dumper(File dumpDir, JClass jclass) {
            this.dumpDir = dumpDir;
            this.jclass = jclass;
        }

        private void dump() {
            String fileName = jclass.getName() + SUFFIX;
            try (PrintStream out = new PrintStream(new FileOutputStream(
                    new File(dumpDir, fileName)))) {
                this.out = out;
                dumpClassDeclaration();
                out.println(" {");
                out.println();
                if (!jclass.getDeclaredFields().isEmpty()) {
                    jclass.getDeclaredFields().forEach(this::dumpField);
                }
                jclass.getDeclaredMethods().forEach(this::dumpMethod);
                out.println("}");
            } catch (FileNotFoundException e) {
                logger.warn("Failed to dump class {}, caused by {}", jclass, e);
            }
        }

        private void dumpClassDeclaration() {
            // dump annotations
            jclass.getAnnotations().forEach(out::println);
            // dump class modifiers
            jclass.getModifiers()
                    .stream()
                    // if jclass is an interface, then don't dump modifiers
                    // "interface" and "abstract"
                    .filter(m -> !jclass.isInterface() ||
                            (m != Modifier.INTERFACE && m != Modifier.ABSTRACT))
                    .forEach(m -> out.print(m + " "));
            // dump class type
            if (jclass.isInterface()) {
                out.print("interface");
            } else {
                out.print("class");
            }
            out.print(' ');
            // dump class name
            out.print(jclass.getName());
            // dump super class
            JClass superClass = jclass.getSuperClass();
            if (superClass != null) {
                out.print(" extends ");
                out.print(superClass.getName());
            }
            // dump interfaces
            if (!jclass.getInterfaces().isEmpty()) {
                out.print(" implements ");
                out.print(jclass.getInterfaces()
                        .stream()
                        .map(JClass::getName)
                        .collect(Collectors.joining(", ")));
            }
        }

        private void dumpField(JField field) {
            for (Annotation annotation : field.getAnnotations()) {
                out.print(INDENT);
                out.println(annotation);
            }
            out.print(INDENT);
            dumpModifiers(field.getModifiers());
            out.printf("%s %s;%n%n", field.getType().getName(), field.getName());
        }

        private void dumpModifiers(Set<Modifier> mods) {
            mods.forEach(m -> out.print(m + " "));
        }

        private void dumpMethod(JMethod method) {
            for (Annotation annotation : method.getAnnotations()) {
                out.print(INDENT);
                out.println(annotation);
            }
            out.print(INDENT);
            dumpMethodDeclaration(method);
            if (hasIR(method)) {
                out.println(" {");
                IR ir = method.getIR();
                // dump variables
                dumpVariables(ir);
                // dump statements
                ir.forEach(s -> out.printf("%s%s%s%n",
                        INDENT, INDENT, IRPrinter.toString(s)));
                // dump exception entries
                if (!ir.getExceptionEntries().isEmpty()) {
                    out.println();
                    ir.getExceptionEntries().forEach(e ->
                            out.printf("%s%s%s%n", INDENT, INDENT, e));
                }
                out.printf("%s}%n", INDENT);
            } else {
                out.println(";");
            }
            out.println();
        }

        private void dumpMethodDeclaration(JMethod method) {
            dumpModifiers(method.getModifiers());
            out.printf("%s %s", method.getReturnType(), method.getName());
            // dump parameters
            StringJoiner paramsJoiner = new StringJoiner(", ", "(", ")");
            for (int i = 0; i < method.getParamCount(); ++i) {
                StringJoiner joiner = new StringJoiner(" ");
                method.getParamAnnotations(i)
                        .forEach(anno -> joiner.add(anno.toString()));
                joiner.add(method.getParamType(i).getName());
                // if the method has explicit parameter names
                // or the method has IR, then dump parameter names
                String paramName = method.getParamName(i);
                if (paramName != null) {
                    joiner.add(paramName);
                } else if (hasIR(method)) {
                    joiner.add(method.getIR().getParam(i).getName());
                }
                paramsJoiner.add(joiner.toString());
            }
            out.print(paramsJoiner);
        }

        private static boolean hasIR(JMethod method) {
            return !method.isAbstract();
        }

        private void dumpVariables(IR ir) {
            // group variables by their types;
            Map<Type, List<Var>> vars = new LinkedHashMap<>();
            ir.getVars().stream()
                    .filter(v -> v != ir.getThis())
                    .filter(v -> !ir.getParams().contains(v))
                    .forEach(v -> vars.computeIfAbsent(v.getType(),
                                    (unused) -> new ArrayList<>())
                            .add(v));
            vars.forEach((t, vs) -> {
                out.printf("%s%s%s ", INDENT, INDENT, t);
                out.print(vs.stream()
                        .map(Var::getName)
                        .collect(Collectors.joining(", ")));
                out.println(";");
            });
        }
    }
}
