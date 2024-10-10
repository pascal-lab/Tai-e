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


import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import java.util.Map;

public class BindingManager {
    private final Map<Expression, Type> innerMap;
    private final Map<ASTNode, ASTNode> copyMap;

    public BindingManager() {
        innerMap = Maps.newMap();
        copyMap  = Maps.newMap();
    }

    public Type getTypeOfExp(Expression exp) {
        ITypeBinding t = exp.resolveTypeBinding();
        if (t != null) {
            return TypeUtils.JDTTypeToTaieType(t);
        } else {
            Type type = innerMap.get(exp);
            if (type == null) {
                throw new NewFrontendException("Type of " + exp + " can't be resolved");
            }
            return type;
        }
    }

    public void setType(Expression exp, Type t) {
        innerMap.put(exp, t);
    }

    public ASTNode copyNode(AST ast, ASTNode node) {
        var newNode = ASTNode.copySubtree(ast, node);
        copyMap.put(newNode, node);
        return newNode;
    }

    public ASTNode getRealNode(ASTNode node) {
        return copyMap.getOrDefault(node, node);
    }
}
