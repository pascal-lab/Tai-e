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

package pascal.taie.frontend.newfrontend.source;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import pascal.taie.frontend.newfrontend.java.ClassExtractor;
import pascal.taie.frontend.newfrontend.java.JDTStringReps;
import pascal.taie.frontend.newfrontend.java.JavaClassInit;
import pascal.taie.frontend.newfrontend.java.JavaInit;

import java.util.ArrayList;
import java.util.List;

public class JavaSource implements ClassSource {

    private final CompilationUnit unit;

    private final String binaryName;

    private final List<JavaInit> instanceInits;

    private final List<JavaClassInit> classInits;

    public ASTNode getTypeDeclaration() {
        return typeDeclaration;
    }

    private final ASTNode typeDeclaration;

    public JavaSource(CompilationUnit unit, ASTNode typeDeclaration) {
        this.unit = unit;
        this.typeDeclaration = typeDeclaration;
        ITypeBinding binding = ClassExtractor.getBinding(typeDeclaration);
        binaryName = JDTStringReps.getBinaryName(binding);
        instanceInits = new ArrayList<>();
        classInits = new ArrayList<>();
    }

    @Override
    public String getClassName() {
        return binaryName;
    }

    @Override
    public boolean isApplication() {
        return true;
    }

    public CompilationUnit getUnit() {
        return unit;
    }

    public void addNewInit(JavaInit init) {
        this.instanceInits.add(init);
    }

    public List<JavaInit> getInstanceInits() {
        return instanceInits;
    }

    public void addNewCinit(JavaClassInit classInit) {
        classInits.add(classInit);
    }

    public List<JavaClassInit> getClassInits() {
        return classInits;
    }
}
