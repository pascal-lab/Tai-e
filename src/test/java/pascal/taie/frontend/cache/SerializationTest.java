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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.generics.ClassGSignature;
import pascal.taie.language.generics.MethodGSignature;
import pascal.taie.language.generics.ReferenceTypeGSignature;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.SerializationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class SerializationTest {

    private static World world1;

    @BeforeAll
    public static void setUp() {
        Main.buildWorld(
                "-java", "8",
                "-cp", "src/test/resources/pta/contextsensitivity",
                "-m", "LinkedQueue"
        );
        world1 = World.get();
        World.reset();
    }

    @AfterEach
    void tearDownEach() {
        World.reset();
    }

    @Test
    void compareOptions() {
        Options options1 = world1.getOptions();
        Options options2 = SerializationUtils.serializedCopy(options1);
        assertEquals(options1.getJavaVersion(), options2.getJavaVersion());
        assertEquals(options1.getClassPath(), options2.getClassPath());
        assertEquals(options1.getAppClassPath(), options2.getAppClassPath());
        assertEquals(options1.getAnalyses(), options2.getAnalyses());
        assertEquals(options1.getMainClass(), options2.getMainClass());
        assertEquals(options1.getInputClasses(), options2.getInputClasses());
        assertEquals(options1.getOutputDir(), options2.getOutputDir());
        assertEquals(options1.getScope(), options2.getScope());
        assertEquals(options1.getKeepResult(), options2.getKeepResult());
        assertEquals(options1.getWorldBuilderClass(), options2.getWorldBuilderClass());
    }

    @Test
    void compareMainMethod() {
        JMethod m1 = world1.getMainMethod();
        JMethod m2 = SerializationUtils.serializedCopy(m1);
        assertEquals(m1.getName(), m2.getName());
        assertEquals(m1.getModifiers(), m2.getModifiers());
        assertEquals(m1.getSignature(), m2.getSignature());
        assertEquals(m1.getDeclaringClass().getName(), m2.getDeclaringClass().getName());
        assertEquals(m1.getSubsignature(), m2.getSubsignature());
        assertEquals(m1.getExceptions().size(), m2.getExceptions().size());
        assertEquals(m1.getParamCount(), m2.getParamCount());
        assertEquals(m1.getReturnType().getName(), m2.getReturnType().getName());
    }

    @Test
    void compareMainClass() {
        JClass c1 = world1.getMainMethod().getDeclaringClass();
        JClass c2 = SerializationUtils.serializedCopy(c1);
        compareJClass(c1, c2);
    }

    @Test
    void compareJavaLangObjectClass() {
        JClass c1 = world1.getClassHierarchy().getClass(ClassNames.OBJECT);
        assertNotNull(c1);
        JClass c2 = SerializationUtils.serializedCopy(c1);
        compareJClass(c1, c2);
    }

    @Test
    void compareConstantPoolClass() {
        JClass c1 = world1.getClassHierarchy().getClass("sun.reflect.ConstantPool");
        assertNotNull(c1);
        JClass c2 = SerializationUtils.serializedCopy(c1);
        compareJClass(c1, c2);
    }

    private void compareJClass(JClass c1, JClass c2) {
        assertEquals(c1.getName(), c2.getName());
        assertEquals(c1.getSimpleName(), c2.getSimpleName());
        assertEquals(c1.getModuleName(), c2.getModuleName());
        assertEquals(c1.getModifiers(), c2.getModifiers());
        assertEquals(c1.getInterfaces().size(), c2.getInterfaces().size());
        assertEquals(c1.getDeclaredFields().size(), c2.getDeclaredFields().size());
        assertEquals(c1.getDeclaredMethods().size(), c2.getDeclaredMethods().size());
        assertEquals(c1.isPhantom(), c2.isPhantom());
    }

    @Test
    void compareClassHierarchy() {
        ClassHierarchy hierarchy1 = world1.getClassHierarchy();
        ClassHierarchy hierarchy2 = SerializationUtils.serializedCopy(hierarchy1);
        assertEquals(hierarchy1.allClasses().count(),
                hierarchy2.allClasses().count());
        assertEquals(hierarchy1.applicationClasses().count(),
                hierarchy2.applicationClasses().count());
    }

    @Test
    void compareTypeSystem() {
        TypeSystem typeSystem1 = world1.getTypeSystem();
        SerializationUtils.serializedCopy(typeSystem1);
    }

    @Test
    void compareIRBuilder() {
        IRBuilder irBuilder1 = world1.getIRBuilder();
        SerializationUtils.serializedCopy(irBuilder1);
    }

    /**
     * This test contains multiple subtests because
     * build all IRs is time-consuming, so share a deserialized {@link World}.
     */
    @Test
    void compareIR() {
        World.set(world1); // World.world should be set for building IRs
        World world2 = SerializationUtils.serializedCopy(world1);
        World.set(world2); // World.world should be set for getting IRs
        // test1: compare the size of IR, a simple and loose test
        JClass concurrentHashMapClz1 = world1.getClassHierarchy()
                .getClass("java.util.concurrent.ConcurrentHashMap");
        JClass concurrentHashMapClz2 = world2.getClassHierarchy()
                .getClass("java.util.concurrent.ConcurrentHashMap");
        IR ir1 = concurrentHashMapClz1
                .getDeclaredMethod("putVal")
                .getIR();
        IR ir2 = concurrentHashMapClz2
                .getDeclaredMethod("putVal")
                .getIR();
        assertNotNull(ir2);
        assertEquals(ir1.getVars().size(), ir2.getVars().size());
        assertEquals(ir1.getParams().size(), ir2.getParams().size());
        assertEquals(ir1.getStmts().size(), ir2.getStmts().size());
        assertEquals(ir1.getReturnVars().size(), ir2.getReturnVars().size());
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
        assertEquals(0, v21.getInvokes().size());
        assertEquals(0, v22.getInvokes().size());
        v22.addInvoke(new Invoke(null, null, null));
        assertEquals(0, v21.getInvokes().size()); // v21 should be not changed
        assertEquals(1, v22.getInvokes().size());
        // test3: compare generics signature
        JClass enumClz1 = world1.getClassHierarchy().getClass("java.lang.Enum");
        JClass enumClz2 = world2.getClassHierarchy().getClass("java.lang.Enum");
        ClassGSignature cSig1 = enumClz1.getGSignature();
        ClassGSignature cSig2 = enumClz2.getGSignature();
        if (cSig1 != null && cSig2 != null) {
            assertEquals(cSig1.toString(), cSig2.toString());
        }
        MethodGSignature mSig1 = enumClz1.getDeclaredMethod("valueOf").getGSignature();
        MethodGSignature mSig2 = enumClz2.getDeclaredMethod("valueOf").getGSignature();
        if (mSig1 != null && mSig2 != null) {
            assertEquals(mSig1.toString(), mSig2.toString());
        }
        JField tableField1 = concurrentHashMapClz1.getDeclaredField("table");
        JField tableField2 = concurrentHashMapClz2.getDeclaredField("table");
        ReferenceTypeGSignature fSig1 = tableField1.getGSignature();
        ReferenceTypeGSignature fSig2 = tableField2.getGSignature();
        if (fSig1 != null && fSig2 != null) {
            assertEquals(fSig1.toString(), fSig2.toString());
        }
    }

}
