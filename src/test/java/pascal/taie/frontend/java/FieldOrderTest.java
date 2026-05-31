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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that the bytecode frontend preserves classfile field order when
 * exposing declared fields through {@link JClass#getDeclaredFields()}.
 */
public class FieldOrderTest {

    private static final int REPEATED_BUILDS = 5;

    private static final List<FieldType> FIELD_TYPES = List.of(
            FieldType.values());

    private static final int TYPE_CYCLE_REPETITIONS = 4;

    private static final int MANY_FIELDS =
            FIELD_TYPES.size() * TYPE_CYCLE_REPETITIONS;

    private static final String FIELD_NAME_PREFIX = "f";

    private static final String SAME_FIELD_NAME = "f";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        World.reset();
    }

    @Test
    void preservesOrderOfManyDeclaredFields() throws IOException {
        assertFieldOrder("ManyDeclaredFields",
                fields(i -> new FieldInfo(fieldName(i), FieldType.INT)),
                FieldInfo::name,
                JField::getName);
    }

    @Test
    void preservesOrderOfFieldsWithDifferentNamesAndTypes() throws IOException {
        assertFieldOrder("FieldsWithDifferentNamesAndTypes",
                fields(i -> new FieldInfo(fieldName(i), fieldType(i))),
                FieldInfo::nameAndType,
                FieldOrderTest::nameAndType);
    }

    @Test
    void preservesOrderOfManyFieldsWithSameName() throws IOException {
        assertFieldOrder("ManyFieldsWithSameName",
                FIELD_TYPES.stream()
                        .map(type -> new FieldInfo(SAME_FIELD_NAME, type))
                        .toList(),
                field -> field.type().typeName,
                field -> field.getType().getName());
    }

    private static String fieldName(int index) {
        return String.format("%s%02d", FIELD_NAME_PREFIX, index);
    }

    private static FieldType fieldType(int index) {
        return FIELD_TYPES.get(index % FIELD_TYPES.size());
    }

    private static List<FieldInfo> fields(IntFunction<FieldInfo> fieldInfo) {
        return IntStream.range(0, MANY_FIELDS)
                .mapToObj(fieldInfo)
                .toList();
    }

    private static String nameAndType(JField field) {
        return field.getName() + ":" + field.getType().getName();
    }

    private <T> void assertFieldOrder(
            String className,
            List<FieldInfo> fields,
            Function<FieldInfo, T> expectedView,
            Function<JField, T> actualView) throws IOException {
        writeClass(className, fields);
        List<T> expected = fields.stream()
                .map(expectedView)
                .toList();
        for (int i = 0; i < REPEATED_BUILDS; ++i) {
            List<T> actual = getDeclaredFields(className).stream()
                    .map(actualView)
                    .toList();
            assertEquals(expected, actual);
        }
    }

    private List<JField> getDeclaredFields(String className) {
        World.reset();
        Main.buildWorld(
                "-cp", tempDir.toString(),
                "--input-classes", className);
        JClass jclass = World.get().getClassHierarchy().getClass(className);
        assertNotNull(jclass);
        return jclass.getDeclaredFields().stream().toList();
    }

    private void writeClass(String className, List<FieldInfo> fields)
            throws IOException {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, className,
                null, "java/lang/Object", null);
        for (FieldInfo field : fields) {
            writer.visitField(Opcodes.ACC_PUBLIC, field.name(),
                    field.type().descriptor, null, null).visitEnd();
        }
        writer.visitEnd();
        Files.write(tempDir.resolve(className + ".class"), writer.toByteArray());
    }

    private record FieldInfo(String name, FieldType type) {

        String nameAndType() {
            return name + ":" + type.typeName;
        }
    }

    private enum FieldType {

        BOOLEAN("Z", "boolean"),
        BYTE("B", "byte"),
        CHAR("C", "char"),
        SHORT("S", "short"),
        INT("I", "int"),
        LONG("J", "long"),
        FLOAT("F", "float"),
        DOUBLE("D", "double"),
        STRING("Ljava/lang/String;", "java.lang.String"),
        OBJECT("Ljava/lang/Object;", "java.lang.Object"),
        INT_ARRAY("[I", "int[]"),
        STRING_ARRAY("[Ljava/lang/String;", "java.lang.String[]");

        private final String descriptor;

        private final String typeName;

        FieldType(String descriptor, String typeName) {
            this.descriptor = descriptor;
            this.typeName = typeName;
        }

    }
}
