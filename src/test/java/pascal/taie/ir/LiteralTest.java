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

package pascal.taie.ir;

import org.junit.jupiter.api.Test;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.LongLiteral;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class LiteralTest {

    @Test
    void testNumericLiteral() {
        Random random = new Random();
        for (int i = 0; i < 100; ++i) {
            testIntLiteral(random.nextInt());
            testLongLiteral(random.nextLong());
            testFloatLiteral(random.nextFloat());
            testDoubleLiteral(random.nextDouble());
        }
    }

    private static void testIntLiteral(int i) {
        assertEquals(IntLiteral.get(i).getValue(), i);
    }

    private static void testLongLiteral(long l) {
        assertEquals(LongLiteral.get(l).getValue(), l);
    }

    private static void testFloatLiteral(float f) {
        assertEquals(FloatLiteral.get(f).getValue(), f, 0);
    }

    private static void testDoubleLiteral(double d) {
        assertEquals(DoubleLiteral.get(d).getValue(), d, 0);
    }
}
