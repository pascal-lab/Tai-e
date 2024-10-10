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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class BinaryNameExtractor extends ASTVisitor {
    private String binaryName;

    public String getBinaryName() {
        return binaryName;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        // TODO: check if name equal filename (maybe multiple class in a .java file)
        if (node.isPackageMemberTypeDeclaration()) {
            binaryName = node.resolveBinding().getBinaryName();
        }
        return false;
    }

    public static String getBinaryNameFromCompilationUnit(CompilationUnit compilationUnit) {
        BinaryNameExtractor extractor = new BinaryNameExtractor();
        compilationUnit.accept(extractor);
        String name = extractor.getBinaryName();
        assert name != null;
        return name;
    }
}
