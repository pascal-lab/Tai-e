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

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectionUtils {

    private ReflectionUtils() {
    }

    static Stream<JMethod> getConstructors(JClass jclass) {
        return jclass.getDeclaredMethods()
                .stream()
                .filter(JMethod::isConstructor);
    }

    static Stream<JMethod> getPublicConstructors(JClass jclass) {
        return getConstructors(jclass).filter(ClassMember::isPublic);
    }
}
