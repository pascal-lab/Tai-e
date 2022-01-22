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

package pascal.taie.frontend.soot;

import pascal.taie.language.annotation.Annotated;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.io.PrintStream;
import java.util.stream.Collectors;

/**
 * Prints all annotations of all annotated objects in JClass.
 */
class AnnotationPrinter {

    private static final int CLASS_INDENT = 0;

    private static final int MEMBER_INDENT = 2;

    private static final int PARAM_INDENT = MEMBER_INDENT;

    private static final PrintStream out = System.out;

    static void print(JClass jclass) {
        out.println("Annotations in " + jclass + ":");
        print(jclass, CLASS_INDENT);
        jclass.getDeclaredFields().forEach(f ->
                print(f, MEMBER_INDENT));
        jclass.getDeclaredMethods().forEach(m ->
                print(m, MEMBER_INDENT));
    }

    private static void print(Annotated o, int indent) {
        boolean printed = false;
        if (!o.getAnnotations().isEmpty()) {
            o.getAnnotations().forEach(a ->
                    out.print(a.toString().indent(indent)));
            out.print(o.toString().indent(indent));
            printed = true;
        }
        if (o instanceof JMethod m) {
            printed |= printParams(m, printed);
        }
        if (printed) {
            out.println();
        }
    }

    private static boolean printParams(JMethod method, boolean printed) {
        for (int i = 0; i < method.getParamCount(); ++i) {
            var annotations = method.getParamAnnotations(i);
            if (!annotations.isEmpty()) {
                if (!printed) {
                    out.print(method.toString().indent(MEMBER_INDENT));
                    printed = true;
                }
                out.print(("Parameter " + i + ": " +
                        annotations.stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(",")))
                        .indent(PARAM_INDENT));
            }
        }
        return printed;
    }
}
