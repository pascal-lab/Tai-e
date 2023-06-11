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


package pascal.taie.frontend.cache;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.config.Options;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRBuilder;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.SerializationUtils;

public class SerializationTest {

    private static World world1;

    @BeforeClass
    public static void setUp() {
        Main.buildWorld(
                "-java", "8",
                "-cp", "src/test/resources/pta/contextsensitivity",
                "-m", "LinkedQueue"
        );
        world1 = World.get();
        World.reset();
    }

    @After
    public void tearDownEach() {
        World.reset();
    }

    @Test
    public void compareOptions() {
        Options options1 = world1.getOptions();
        Options options2 = SerializationUtils.serializedCopy(options1);
        Assert.assertEquals(options1.getJavaVersion(), options2.getJavaVersion());
        Assert.assertEquals(options1.getClassPath(), options2.getClassPath());
        Assert.assertEquals(options1.getAppClassPath(), options2.getAppClassPath());
        Assert.assertEquals(options1.getAnalyses(), options2.getAnalyses());
        Assert.assertEquals(options1.getMainClass(), options2.getMainClass());
        Assert.assertEquals(options1.getInputClasses(), options2.getInputClasses());
        Assert.assertEquals(options1.getOutputDir(), options2.getOutputDir());
        Assert.assertEquals(options1.getScope(), options2.getScope());
        Assert.assertEquals(options1.getKeepResult(), options2.getKeepResult());
        Assert.assertEquals(options1.getWorldBuilderClass(), options2.getWorldBuilderClass());
    }

    @Test
    public void compareMainMethod() {
        JMethod m1 = world1.getMainMethod();
        JMethod m2 = SerializationUtils.serializedCopy(m1);
        Assert.assertEquals(m1.getName(), m2.getName());
        Assert.assertEquals(m1.getModifiers(), m2.getModifiers());
        Assert.assertEquals(m1.getSignature(), m2.getSignature());
        Assert.assertEquals(m1.getDeclaringClass().getName(), m2.getDeclaringClass().getName());
        Assert.assertEquals(m1.getSubsignature(), m2.getSubsignature());
        Assert.assertEquals(m1.getExceptions().size(), m2.getExceptions().size());
        Assert.assertEquals(m1.getParamCount(), m2.getParamCount());
        Assert.assertEquals(m1.getReturnType().getName(), m2.getReturnType().getName());
    }

    @Test
    public void compareMainClass() {
        JClass c1 = world1.getMainMethod().getDeclaringClass();
        JClass c2 = SerializationUtils.serializedCopy(c1);
        compareJClass(c1, c2);
    }

    @Test
    public void compareJavaLangObjectClass() {
        JClass c1 = world1.getClassHierarchy().getClass(ClassNames.OBJECT);
        Assert.assertNotNull(c1);
        JClass c2 = SerializationUtils.serializedCopy(c1);
        compareJClass(c1, c2);
    }

    @Test
    public void compareConstantPoolClass() {
        JClass c1 = world1.getClassHierarchy().getClass("sun.reflect.ConstantPool");
        Assert.assertNotNull(c1);
        JClass c2 = SerializationUtils.serializedCopy(c1);
        compareJClass(c1, c2);
    }

    private void compareJClass(JClass c1, JClass c2) {
        Assert.assertEquals(c1.getName(), c2.getName());
        Assert.assertEquals(c1.getSimpleName(), c2.getSimpleName());
        Assert.assertEquals(c1.getModuleName(), c2.getModuleName());
        Assert.assertEquals(c1.getModifiers(), c2.getModifiers());
        Assert.assertEquals(c1.getInterfaces().size(), c2.getInterfaces().size());
        Assert.assertEquals(c1.getDeclaredFields().size(), c2.getDeclaredFields().size());
        Assert.assertEquals(c1.getDeclaredMethods().size(), c2.getDeclaredMethods().size());
        Assert.assertEquals(c1.isPhantom(), c2.isPhantom());
    }

    @Test
    public void compareClassHierarchy() {
        ClassHierarchy hierarchy1 = world1.getClassHierarchy();
        ClassHierarchy hierarchy2 = SerializationUtils.serializedCopy(hierarchy1);
        Assert.assertEquals(hierarchy1.allClasses().count(),
                hierarchy2.allClasses().count());
        Assert.assertEquals(hierarchy1.applicationClasses().count(),
                hierarchy2.applicationClasses().count());
    }

    @Test
    public void compareTypeSystem() {
        TypeSystem typeSystem1 = world1.getTypeSystem();
        SerializationUtils.serializedCopy(typeSystem1);
    }

    @Test
    public void compareIRBuilder() {
        IRBuilder irBuilder1 = world1.getIRBuilder();
        SerializationUtils.serializedCopy(irBuilder1);
    }

    /**
     * This test contains multiple subtests because
     * build all IRs is time-consuming, so share a deserialized {@link World}.
     */
    @Test
    public void compareIR() {
        World.set(world1); // World.world should be set for building IRs
        World world2 = SerializationUtils.serializedCopy(world1);
        World.set(world2); // World.world should be set for getting IRs
        // test1: compare the size of IR, a simple and loose test
        IR ir1 = world1.getClassHierarchy()
                .getClass("java.util.concurrent.ConcurrentHashMap")
                .getDeclaredMethod("putVal")
                .getIR();
        IR ir2 = world2.getClassHierarchy()
                .getClass("java.util.concurrent.ConcurrentHashMap")
                .getDeclaredMethod("putVal")
                .getIR();
        Assert.assertNotNull(ir2);
        Assert.assertEquals(ir1.getVars().size(), ir2.getVars().size());
        Assert.assertEquals(ir1.getParams().size(), ir2.getParams().size());
        Assert.assertEquals(ir1.getStmts().size(), ir2.getStmts().size());
        Assert.assertEquals(ir1.getReturnVars().size(), ir2.getReturnVars().size());
        // test2: compare the Var.RelevantStmts.Empty
        Var v21 = world2.getClassHierarchy()
                .getClass("java.util.HashMap")
                .getDeclaredMethod("hash")
                .getIR()
                .getReturnVars()
                .get(0);
        Var v22 = world2.getClassHierarchy()
                .getClass("java.util.ArrayList")
                .getDeclaredMethod("lastIndexOf")
                .getIR()
                .getReturnVars()
                .get(0);
        Assert.assertEquals(0, v21.getInvokes().size());
        Assert.assertEquals(0, v22.getInvokes().size());
        v22.addInvoke(new Invoke(null, null, null));
        Assert.assertEquals(0, v21.getInvokes().size()); // v21 should be not changed
        Assert.assertEquals(1, v22.getInvokes().size());
    }

}
