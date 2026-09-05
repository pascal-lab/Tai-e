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

package pascal.taie.frontend.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static pascal.taie.frontend.java.LineNumberAssertions.assertAllLines;
import static pascal.taie.frontend.java.LineNumberAssertions.assertInferredCastLine;
import static pascal.taie.frontend.java.LineNumberAssertions.assertInstanceFieldCastLine;
import static pascal.taie.frontend.java.LineNumberAssertions.assertInvokeLine;
import static pascal.taie.frontend.java.LineNumberAssertions.assertLiteralDefinitionLine;
import static pascal.taie.frontend.java.LineNumberAssertions.assertReturnLine;
import static pascal.taie.frontend.java.LineNumberAssertions.assertReturnLiteralLines;
import static pascal.taie.frontend.java.LineNumberAssertions.assertStoreFieldLine;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FakeLineNumberTest {

    @TempDir
    static Path tempDir;

    private static JClass fakeClass;

    private static JClass getFakeClass() throws IOException {
        if (fakeClass == null) {
            Path fullDir = tempDir.resolve("full");
            Files.createDirectories(fullDir);
            byte[] fake = generateFakeClass();
            Files.write(tempDir.resolve("Fake.class"), fake);
            Files.write(fullDir.resolve("Fake.class"), fake);
            writePhantomBase(fullDir);
            writePhantomSub(fullDir);
            try (URLClassLoader loader = new URLClassLoader(
                    new URL[]{fullDir.toUri().toURL()}, null)) {
                Class.forName("Fake", false, loader);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Generated Fake class is not loadable", e);
            }
            Main.buildWorld("-cp", tempDir.toString(), "-m", "Fake");
            fakeClass = World.get().getClassHierarchy().getClass("Fake");
            for (JMethod method : fakeClass.getDeclaredMethods()) {
                if (!method.isAbstract() && !method.isNative()) {
                    method.getIR();
                }
            }
        }
        return fakeClass;
    }

    @Test
    void noDebugInfo() {
        Main.buildWorld("-cp", "src/test/resources/world",
                "-m", "NoDebug");
        JClass noDebug = World.get().getClassHierarchy().getClass("NoDebug");
        for (JMethod method : noDebug.getDeclaredMethods()) {
            if (!method.isAbstract() && !method.isNative()) {
                assertAllLines(method, -1);
            }
        }
    }

    @Test
    void foldedInvokeUsesProducerLine() throws IOException {
        JMethod main = getFakeClass().getDeclaredMethod("main");
        assertInvokeLine(main, "java.lang.System", "getProperties", 92);
    }

    @Test
    void inferredStoreAndReturnKeepLine() throws IOException {
        JClass fake = getFakeClass();
        JMethod store = fake.getDeclaredMethod("store");
        JMethod returnValue = fake.getDeclaredMethod("returnValue");
        assertInferredCastLine(store, 60);
        assertStoreFieldLine(store, "field", 60);
        assertInferredCastLine(returnValue, 70);
        assertReturnLine(returnValue, 70);
    }

    @Test
    void missingLineEntryUsesUnknownLine() throws IOException {
        JMethod method = getFakeClass().getDeclaredMethod("choose");
        assertReturnLiteralLines(method, 0, List.of(-1, 80));
        // The cached constant definition is synthetic even when an explicit
        // bytecode origin has a known line.
        assertLiteralDefinitionLine(method, 0, -1);
    }

    @Test
    void conflictingLineEntriesUseUnknownLine() throws IOException {
        JMethod method = getFakeClass().getDeclaredMethod("conflictingConst");
        assertReturnLiteralLines(method, 0, List.of(81, 82));
        assertLiteralDefinitionLine(method, 0, -1);
    }

    @Test
    void implicitConditionHasNoLiteralLine() throws IOException {
        JMethod method = getFakeClass().getDeclaredMethod("implicitConst");
        assertReturnLiteralLines(method, 1, List.of(121));
        assertReturnLiteralLines(method, 2, List.of(122));
        assertLiteralDefinitionLine(method, 0, -1);
        assertLiteralDefinitionLine(method, 1, -1);
        assertLiteralDefinitionLine(method, 2, -1);
    }

    @Test
    void mixedImplicitAndExplicitOriginsUseUnknownLine() throws IOException {
        JMethod method = getFakeClass().getDeclaredMethod("mixedConst");
        assertReturnLiteralLines(method, 0, List.of(132));
        assertLiteralDefinitionLine(method, 0, -1);
    }

    @Test
    void inferredInstanceFieldCastKeepsLine() throws IOException {
        JMethod method = getFakeClass().getDeclaredMethod("phantomField");
        assertInstanceFieldCastLine(method, "field", 90);
    }

    @Test
    void stackPhiAssignmentsKeepProducerLine() throws IOException {
        JMethod method = getFakeClass().getDeclaredMethod("stackPhi");
        List<Integer> actualLines = method.getIR().stmts()
                .filter(Copy.class::isInstance)
                .map(Copy.class::cast)
                .filter(stmt -> stmt.getRValue().isConst())
                .filter(stmt -> stmt.getRValue().getConstValue()
                        instanceof IntLiteral literal
                        && (literal.getValue() == 1 || literal.getValue() == 2))
                .map(Stmt::getLineNumber)
                .toList();
        assertEquals(List.of(100, 110), actualLines);
    }

    private static void writePhantomBase(Path directory) throws IOException {
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC,
                "PhantomBase", null, "java/lang/Object", null);
        classWriter.visitField(Opcodes.ACC_PUBLIC, "field", "I", null, null)
                .visitEnd();
        addConstructor(classWriter, "PhantomBase", "java/lang/Object");
        classWriter.visitEnd();
        Files.write(directory.resolve("PhantomBase.class"), classWriter.toByteArray());
    }

    private static void writePhantomSub(Path directory) throws IOException {
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC,
                "PhantomSub", null, "PhantomBase", null);
        addConstructor(classWriter, "PhantomSub", "PhantomBase");
        classWriter.visitEnd();
        Files.write(directory.resolve("PhantomSub.class"), classWriter.toByteArray());
    }

    private static void addConstructor(ClassWriter classWriter,
            String owner, String superName) {
        MethodVisitor method = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL,
                superName, "<init>", "()V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();
    }

    /**
     * Compact-IR policy: a folded Invoke uses the producer instruction's line.
     * The following store is deliberately assigned a different line.
     */
    private static byte[] generateFakeClass() {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "Fake", null, "java/lang/Object", null);
        MethodVisitor method = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "main", "([Ljava/lang/String;)V", null, null);
        method.visitCode();
        Label invokeLine = new Label();
        method.visitLabel(invokeLine);
        method.visitLineNumber(92, invokeLine);
        method.visitMethodInsn(Opcodes.INVOKESTATIC,
                "java/lang/System", "getProperties",
                "()Ljava/util/Properties;", false);
        Label storeLine = new Label();
        method.visitLabel(storeLine);
        method.visitLineNumber(95, storeLine);
        method.visitVarInsn(Opcodes.ASTORE, 1);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 2);
        method.visitEnd();
        addInferredCastMethods(classWriter);
        addPartialLineMethod(classWriter);
        addConflictingConstMethod(classWriter);
        addImplicitConstMethod(classWriter);
        addMixedConstMethod(classWriter);
        addPhantomFieldMethod(classWriter);
        addStackPhiMethod(classWriter);
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    /**
     * The methods are verifier-valid with PhantomSub extending PhantomBase.
     * The analysis classpath intentionally omits that hierarchy, so Tai-e
     * must insert casts for the unresolved reference types.
     */
    private static void addInferredCastMethods(ClassWriter classWriter) {
        classWriter.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "field", "LPhantomBase;", null, null).visitEnd();

        MethodVisitor store = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "store", "(LPhantomSub;)V", null, null);
        store.visitCode();
        Label storeLine = new Label();
        store.visitLabel(storeLine);
        store.visitLineNumber(60, storeLine);
        store.visitVarInsn(Opcodes.ALOAD, 0);
        store.visitFieldInsn(Opcodes.PUTSTATIC, "Fake", "field",
                "LPhantomBase;");
        store.visitInsn(Opcodes.RETURN);
        store.visitMaxs(1, 1);
        store.visitEnd();

        MethodVisitor returnValue = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "returnValue", "(LPhantomSub;)LPhantomBase;",
                null, null);
        returnValue.visitCode();
        Label returnLine = new Label();
        returnValue.visitLabel(returnLine);
        returnValue.visitLineNumber(70, returnLine);
        returnValue.visitVarInsn(Opcodes.ALOAD, 0);
        returnValue.visitInsn(Opcodes.ARETURN);
        returnValue.visitMaxs(1, 1);
        returnValue.visitEnd();
    }

    private static void addConflictingConstMethod(ClassWriter classWriter) {
        MethodVisitor method = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "conflictingConst", "(Z)I", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ILOAD, 0);
        Label secondLine = new Label();
        method.visitJumpInsn(Opcodes.IFEQ, secondLine);
        Label firstLine = new Label();
        method.visitLabel(firstLine);
        method.visitLineNumber(81, firstLine);
        method.visitInsn(Opcodes.ICONST_0);
        method.visitInsn(Opcodes.IRETURN);
        method.visitLabel(secondLine);
        method.visitLineNumber(82, secondLine);
        method.visitInsn(Opcodes.ICONST_0);
        method.visitInsn(Opcodes.IRETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();
    }

    private static void addImplicitConstMethod(ClassWriter classWriter) {
        MethodVisitor method = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "implicitConst", "(I)I", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ILOAD, 0);
        Label falseBranch = new Label();
        Label conditionLine = new Label();
        method.visitLabel(conditionLine);
        method.visitLineNumber(120, conditionLine);
        method.visitJumpInsn(Opcodes.IFEQ, falseBranch);
        Label trueLine = new Label();
        method.visitLabel(trueLine);
        method.visitLineNumber(121, trueLine);
        method.visitInsn(Opcodes.ICONST_1);
        method.visitInsn(Opcodes.IRETURN);
        method.visitLabel(falseBranch);
        Label falseLine = new Label();
        method.visitLabel(falseLine);
        method.visitLineNumber(122, falseLine);
        method.visitInsn(Opcodes.ICONST_2);
        method.visitInsn(Opcodes.IRETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();
    }

    private static void addMixedConstMethod(ClassWriter classWriter) {
        MethodVisitor method = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "mixedConst", "(Z)I", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ILOAD, 0);
        Label falseBranch = new Label();
        Label conditionLine = new Label();
        method.visitLabel(conditionLine);
        method.visitLineNumber(130, conditionLine);
        method.visitJumpInsn(Opcodes.IFEQ, falseBranch);
        Label trueLine = new Label();
        method.visitLabel(trueLine);
        method.visitLineNumber(131, trueLine);
        method.visitInsn(Opcodes.ICONST_1);
        method.visitInsn(Opcodes.IRETURN);
        method.visitLabel(falseBranch);
        Label falseLine = new Label();
        method.visitLabel(falseLine);
        method.visitLineNumber(132, falseLine);
        method.visitInsn(Opcodes.ICONST_0);
        method.visitInsn(Opcodes.IRETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();
    }

    private static void addPhantomFieldMethod(ClassWriter classWriter) {
        MethodVisitor method = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "phantomField", "()I", null, null);
        method.visitCode();
        Label line = new Label();
        method.visitLabel(line);
        method.visitLineNumber(90, line);
        method.visitTypeInsn(Opcodes.NEW, "PhantomSub");
        method.visitInsn(Opcodes.DUP);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "PhantomSub", "<init>", "()V", false);
        method.visitVarInsn(Opcodes.ASTORE, 0);
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD,
                "PhantomBase", "field", "I");
        method.visitInsn(Opcodes.IRETURN);
        method.visitMaxs(2, 1);
        method.visitEnd();
    }

    private static void addStackPhiMethod(ClassWriter classWriter) {
        MethodVisitor consume = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "consume", "(I)V", null, null);
        consume.visitCode();
        consume.visitInsn(Opcodes.RETURN);
        consume.visitMaxs(0, 1);
        consume.visitEnd();

        MethodVisitor method = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "stackPhi", "(Z)V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ILOAD, 0);
        Label elseBranch = new Label();
        Label join = new Label();
        method.visitJumpInsn(Opcodes.IFEQ, elseBranch);
        Label firstValue = new Label();
        method.visitLabel(firstValue);
        method.visitLineNumber(100, firstValue);
        method.visitInsn(Opcodes.ICONST_1);
        Label firstGoto = new Label();
        method.visitLabel(firstGoto);
        method.visitLineNumber(101, firstGoto);
        method.visitJumpInsn(Opcodes.GOTO, join);
        method.visitLabel(elseBranch);
        Label secondValue = new Label();
        method.visitLabel(secondValue);
        method.visitLineNumber(110, secondValue);
        method.visitInsn(Opcodes.ICONST_2);
        Label secondGoto = new Label();
        method.visitLabel(secondGoto);
        method.visitLineNumber(111, secondGoto);
        method.visitJumpInsn(Opcodes.GOTO, join);
        method.visitLabel(join);
        Label consumeLine = new Label();
        method.visitLabel(consumeLine);
        method.visitLineNumber(120, consumeLine);
        method.visitMethodInsn(Opcodes.INVOKESTATIC,
                "Fake", "consume", "(I)V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();
    }

    private static void addPartialLineMethod(ClassWriter classWriter) {
        MethodVisitor choose = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "choose", "(I)I", null, null);
        choose.visitCode();
        choose.visitVarInsn(Opcodes.ILOAD, 0);
        Label noLine = new Label();
        Label line = new Label();
        choose.visitJumpInsn(Opcodes.IFEQ, line);
        choose.visitLabel(noLine);
        choose.visitInsn(Opcodes.ICONST_0);
        choose.visitInsn(Opcodes.IRETURN);
        choose.visitLabel(line);
        choose.visitLineNumber(80, line);
        choose.visitInsn(Opcodes.ICONST_0);
        choose.visitInsn(Opcodes.IRETURN);
        choose.visitMaxs(1, 1);
        choose.visitEnd();
    }
}
