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

package pascal.taie.analysis.dataflow.analysis;

import org.junit.Test;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.dataflow.analysis.availexp.AvailableExpression;

public class AvailExpTest {

    @Test
    public void test() {
        Tests.test("AvailExp", "src/test/resources/dataflow/",
                AvailableExpression.ID);
    }
}
