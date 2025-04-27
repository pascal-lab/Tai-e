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

package pascal.taie.frontend.newfrontend.closedworld;

import org.objectweb.asm.ClassReader;
import pascal.taie.frontend.newfrontend.exception.CorruptClassFileException;
import pascal.taie.frontend.newfrontend.exception.FrontendException;
import pascal.taie.frontend.newfrontend.source.AsmSource;
import pascal.taie.frontend.newfrontend.source.ClassSource;
import pascal.taie.frontend.newfrontend.source.JavaSource;
import pascal.taie.frontend.newfrontend.java.JavaClassManager;
import pascal.taie.frontend.newfrontend.JavacSourceHandler;
import pascal.taie.frontend.newfrontend.source.PhantomClassSource;
import pascal.taie.project.ProgramFile;
import pascal.taie.project.DotClassFile;
import pascal.taie.project.DotJavaFile;
import pascal.taie.project.Project;
import pascal.taie.util.collection.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class DependencyResolver {
    static ResolveResult
    resolve(Project project, String binaryName, ProgramFile file)
            throws IOException, FrontendException {
        if (file instanceof DotJavaFile dotJavaFile) {
            // return getJavaDependenciesWithJDT(project, binaryName, javaSourceFile);
            return resolveWithJavac(project, binaryName, dotJavaFile);
        } else if (file instanceof DotClassFile dotClassFile) {
            return resolveClassFile(project, binaryName, dotClassFile);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static ResolveResult
    resolvePhantom(String binaryName) {
        return new ResolveResult(List.of(), List.of(new Pair<>(binaryName,
                new PhantomClassSource(binaryName.replace('/', '.'), false))));
    }

    private static ResolveResult
    resolveWithJDT(Project project, String binaryName, DotJavaFile dotJavaFile) {
        // DO NOT change the order of next 2 stmts
        List<String> deps = JavaClassManager.get().getImports(project, dotJavaFile);
        JavaSource[] javaSources = JavaClassManager.get().getJavaSources(dotJavaFile);
        List<Pair<String, ClassSource>> sources = new ArrayList<>();
        for (JavaSource s : javaSources) {
            String binaryNameOfFile = s.getClassName().replace('.', '/');
            sources.add(new Pair<>(binaryNameOfFile, s));
        }
        return new ResolveResult(deps, sources);
    }

    private static ResolveResult
    resolveWithJavac(Project project, String binaryName, DotJavaFile dotJavaFile)
            throws IOException, FrontendException {
        List<DotClassFile> dotClassFiles =
                new JavacSourceHandler().compile(project.getClassPath(),
                        dotJavaFile.resource().getPath().toString(),
                        project.getJavaVersion());
        boolean isApplication = project.isApp(dotJavaFile);
        List<String> deps = new ArrayList<>();
        List<Pair<String, ClassSource>> sources = new ArrayList<>();
        for (DotClassFile dotClassFile : dotClassFiles) {
             ResolveResult r =
                    resolveClassFile(project, dotClassFile.getInternalName(), dotClassFile, isApplication);
            deps.addAll(r.dependencies());
            sources.addAll(r.resolvedSource());
        }
        return new ResolveResult(deps, sources);
    }

    private static ResolveResult
    resolveClassFile(Project project, String binaryName, DotClassFile cFile, boolean isApplication)
            throws IOException, CorruptClassFileException {
        byte[] content = cFile.resource().getContent();
        cFile.resource().release();
        assert content != null;
        ClassReader reader = new ClassReader(content);
        int version = reader.readShort(6);
        List<String> deps = new ConstantTableReader(binaryName, cFile, content).read();
        return new ResolveResult(deps, List.of(
                new Pair<>(binaryName, new AsmSource(reader, isApplication, version, null))));
    }

    private static ResolveResult
    resolveClassFile(Project project, String binaryName, DotClassFile cFile)
            throws IOException, CorruptClassFileException {
        return resolveClassFile(project, binaryName, cFile, project.isApp(cFile));
    }
}
