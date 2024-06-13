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

package pascal.taie.analysis.pta.plugin.natives;

import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.IRModelPlugin;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.IntType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

import java.util.ArrayList;
import java.util.List;

public class UnsafeModel extends IRModelPlugin {

    private int counter = 0;

    UnsafeModel(Solver solver) {
        super(solver);
    }

    @InvokeHandler(signature = "<sun.misc.Unsafe: boolean compareAndSwapObject(java.lang.Object,long,java.lang.Object,java.lang.Object)>")
    public List<Stmt> compareAndSwapObject(Invoke invoke) {
        // unsafe.compareAndSwapObject(o, offset, expected, x);
        List<Var> args = invoke.getInvokeExp().getArgs();
        List<Stmt> stmts = new ArrayList<>();
        Var o = args.get(0);
        Var x = args.get(3);
        if (o.getType() instanceof ArrayType) { // if o is of ArrayType
            // generate o[i] = x;
            Var i = new Var(invoke.getContainer(),
                    "%unsafe-index" + counter++, IntType.INT, -1);
            stmts.add(new StoreArray(new ArrayAccess(o, i), x));
        } else { // otherwise, o is of ClassType
            // generate o.f = x; for field f that has the same type of x.
            JClass clazz = ((ClassType) o.getType()).getJClass();
            Type xType = x.getType();
            if (xType instanceof ReferenceType) { // ignore primitive types
                clazz.getDeclaredFields()
                        .stream()
                        .filter(f -> f.getType().equals(xType))
                        .forEach(f -> stmts.add(new StoreField(
                                new InstanceFieldAccess(f.getRef(), o), x)));
            }
        }
        return stmts;
    }
}
