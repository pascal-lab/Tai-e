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

package pascal.taie.analysis.pta.plugin.spring;

import org.junit.jupiter.api.Test;

class DiTests {


    @Test
    void testXml() {
        String testcaseName = "xml";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testClassAnno() {
        String testcaseName = "class-anno";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testDefaultNameAndScope() {
        String testcaseName = "default-name-scope";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testMethodAnno() {
        String testcaseName = "method-anno";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testStaticFactoryMethod() {
        String testcaseName = "static-factory-method";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testClassConstructorAnno() {
        String testcaseName = "class-constructor-anno";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testInjectedField() {
        String testcaseName = "injected-field";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testInjectedMethod() {
        String testcaseName = "injected-method";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testInjectedParent() {
        String testcaseName = "injected-parent";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testQualifierAnno() {
        String testcaseName = "qualifier-anno";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testComplexMultiContaining() {
        String testcaseName = "complex-multi-containing";
        TestMain.testDi(testcaseName, "2-obj");
    }

    @Test
    void testComplexFactoryBean() {
        String testcaseName = "complex-factory-bean";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testOverrideFactoryMethod() {
        String testcaseName = "override-factory-method";
        TestMain.testDi(testcaseName);
    }

    @Test
    void testInjectedInterface() {
        String testcaseName = "injected-interface";
        TestMain.testDi(testcaseName);
    }

}
