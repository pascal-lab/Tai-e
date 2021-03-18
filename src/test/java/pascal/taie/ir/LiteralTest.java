/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.ir;

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.LongLiteral;

import java.util.Random;

public class LiteralTest {

    @Test
    public void testNumericLiteral() {
        Random random = new Random();
        for (int i = 0; i < 100; ++i) {
            testIntLiteral(random.nextInt());
            testLongLiteral(random.nextLong());
            testFloatLiteral(random.nextFloat());
            testDoubleLiteral(random.nextDouble());
        }
    }

    private static void testIntLiteral(int i) {
        Assert.assertEquals(IntLiteral.get(i).getValue(), i);
    }

    private static void testLongLiteral(long l) {
        Assert.assertEquals(LongLiteral.get(l).getValue(), l);
    }

    private static void testFloatLiteral(float f) {
        Assert.assertEquals(FloatLiteral.get(f).getValue(), f, 0);
    }

    private static void testDoubleLiteral(double d) {
        Assert.assertEquals(DoubleLiteral.get(d).getValue(), d, 0);
    }
}
