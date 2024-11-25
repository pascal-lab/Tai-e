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

package pascal.taie.frontend.newfrontend.java;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.Optional;

import static pascal.taie.frontend.newfrontend.java.JDTStringReps.getBinaryName;

/**
 * Locate a method by signature in a class file({@code CompilationUnit})
 */
public class MethodLocator {
    MethodDeclaration methodDeclaration;
    Logger logger = LogManager.getLogger(MethodLocator.class);
    Optional<MethodDeclaration> getMethodBySig(
            String className,
            String signature, CompilationUnit cu) {
        LocateVisitor lv = new LocateVisitor(signature);
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                // if class name is not equal, visit child to check if target is inner class
                var binName = getBinaryName(node.resolveBinding());
                if (! binName.equals(className)) {
                    return true;
                }

                // if class name is equal, visit all methods to locate.
                // DO NOT visit child after this method
                for (var i : node.getMethods()) {
                    i.accept(lv);
                    if (lv.res != null) {
                        methodDeclaration = lv.res;
                        break;
                    }
                }
                return false;
            }

            @Override
            public boolean visit(AnonymousClassDeclaration acd) {
                var binName = getBinaryName(acd.resolveBinding());
                if (! binName.equals(className)) {
                    return true;
                }
                for (var i : acd.bodyDeclarations()) {
                    BodyDeclaration bd = (BodyDeclaration) i;
                    bd.accept(lv);
                    if (lv.success()) {
                        break;
                    }
                }
                return false;
            }
        });
        return Optional.ofNullable(methodDeclaration);
    }
}

