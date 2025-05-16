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

package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.FrontendStmtVisitor;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.StmtVisitor;

/**
 * Representation of a phi statement in the front-end, e.g., a = phi(s1:b1, s2:b2).
 * <p>
 * This is used to represent the phi statement in the front-end, which is different from the
 * {@link pascal.taie.ir.stmt.PhiStmt} in output IR.
 * </p>
 */
public class FrontendPhiStmt extends AssignStmt<Var, FrontendPhiExp> {

    private final Var base;

    public FrontendPhiStmt(Var base, Var def, FrontendPhiExp phiExp) {
        super(def, phiExp);
        this.base = base;
    }

    /**
     * WARNING: this method and the field `base` is used only in the front-end,
     * and you should not use this method to testify may-have-same-base relationship.
     * Instead, check the segment before "#" in the variable name.
     * @return the base variable before SSA renaming.
     */
    public Var getBase() {
        return base;
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        if (visitor instanceof FrontendStmtVisitor<T> frontendVisitor) {
            return frontendVisitor.visit(this);
        }
        throw new UnsupportedOperationException("Illegal visitor type: " + visitor.getClass());
    }
}
