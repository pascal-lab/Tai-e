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

package pascal.taie.language;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PhantomTest {
    @Test
    void testPhantomField() {
        Main.buildWorld("-cp", "src/test/resources/world",
                "--main-class", "PhantomField",
                "--allow-phantom");
        JMethod main = World.get().getMainMethod();
        for (Stmt stmt : main.getIR()) {
            if (stmt instanceof LoadField loadField) {
                FieldRef fieldRef = loadField.getFieldRef();
                assertEquals(fieldRef.isStatic(), fieldRef.resolve().isStatic());
            }
        }
    }
}
