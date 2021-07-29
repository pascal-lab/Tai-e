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

package pascal.taie.analysis.pta;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import pascal.taie.analysis.pta.core.cs.context.ContextTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ContextTest.class,
        CSPTATest.class,
        ExceptionTest.class,
        LambdaTest.class,
        ReflectionTest.class,
        TaintTest.class,
})
public class PTATestSuite {
}
