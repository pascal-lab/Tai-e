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

package pascal.taie.frontend.java.closedworld;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;

import pascal.taie.frontend.java.FrontendException;
import pascal.taie.frontend.java.classes.AsmClassSource;
import pascal.taie.frontend.java.classes.PhantomClassSource;
import pascal.taie.frontend.java.project.ClassFile;
import pascal.taie.frontend.java.project.DotClassFile;
import pascal.taie.frontend.java.project.DotJavaFile;
import pascal.taie.frontend.java.project.Project;
import pascal.taie.language.classes.ClassSource;

/**
 * Resolves all class dependencies (i.e., the referenced classes) of a class.
 */
class DependencyResolver {

    static ResolveResult resolve(Project project, ClassFile file)
            throws IOException, FrontendException {
        if (file instanceof DotJavaFile dotJavaFile) {
            return resolveWithJavac(project, dotJavaFile);
        } else if (file instanceof DotClassFile dotClassFile) {
            return resolveClassFile(project, dotClassFile);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static ResolveResult resolvePhantom(String className) {
        return new ResolveResult(List.of(),
                // phantom classes should not be jre classes, so we consider it as app classes
                List.of(new PhantomClassSource(className, true)));
    }

    private static ResolveResult resolveWithJavac(Project project, DotJavaFile dotJavaFile)
            throws IOException, FrontendException {
        List<DotClassFile> dotClassFiles = JavacSourceHandler.compile(
                project.classPath(),
                dotJavaFile.getResource().getPath().toString(),
                project.javaVersion());
        List<String> deps = new ArrayList<>();
        List<ClassSource> sources = new ArrayList<>();
        for (DotClassFile dotClassFile : dotClassFiles) {
            ResolveResult result = resolveClassFile(dotClassFile, project.isApp(dotJavaFile));
            deps.addAll(result.dependencies());
            sources.addAll(result.resolvedSource());
        }
        return new ResolveResult(deps, sources);
    }

    private static ResolveResult resolveClassFile(Project project, DotClassFile classFile)
            throws IOException, FrontendException {
        return resolveClassFile(classFile, project.isApp(classFile));
    }

    private static ResolveResult resolveClassFile(DotClassFile classFile, boolean isApp)
            throws IOException, FrontendException {
        byte[] content = classFile.getResource().getContent();
        classFile.getResource().release();
        ClassReader reader = new ClassReader(content);
        // 6 is the offset of class file version
        int version = reader.readShort(6);
        List<String> deps = new ConstantTableReader(classFile, content).readClassNames();
        return new ResolveResult(deps, List.of(new AsmClassSource(reader, isApp, version)));
    }
}
