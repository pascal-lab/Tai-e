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

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import pascal.taie.util.collection.Sets;

import java.util.List;
import java.util.Set;

public class ImportExtractor extends ASTVisitor {

    Set<String> names = Sets.newSet();

    @Override
    public boolean visit(QualifiedType node) {
        ITypeBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        ITypeBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }

    @Override
    public boolean visit(ArrayType node) {
        ITypeBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }


    @Override
    public boolean visit(ImportDeclaration node) {
        IBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        IBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        IBinding binding = node.resolveBinding();
        extractBinding(binding);
        return false;
    }

    private void extractBinding(IBinding binding) {
        if (binding == null) {
            throw new RuntimeException(
                    "Binding resolving failure, following the JDT error message to fix it");
        }
        if (binding instanceof ITypeBinding typeBinding) {
            String binaryTypeName;
            if (typeBinding.isArray()) {
                binaryTypeName = typeBinding.getElementType().getBinaryName();
            } else {
                binaryTypeName = typeBinding.getBinaryName();
            }
            names.add(binaryTypeName);
        } else if (binding instanceof IMethodBinding methodBinding) {
            names.add(methodBinding.getDeclaringClass().getBinaryName());
        }
        // TODO: 1. handle Annotation (IMemberValuePairBinding)
        //       2. handle wildcard import (IPackageBinding)
    }

    public List<String> getDependencies() {
        return names.stream().toList();
    }
}
