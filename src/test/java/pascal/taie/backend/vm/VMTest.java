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

package pascal.taie.backend.vm;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.util.MultiStringsSource;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VMTest {

    private static final String CP = "src/test/resources/vm";

    @Test
    void testExceptionDate() {
        Main.buildWorld("-cp", CP, "-m", "ExceptionDate");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        VM vm = new VM(World.get());
        vm.exec();
        String output = outputStream.toString();
        String current = new SimpleDateFormat("yyyy-MM-dd HH:mm")
                .format(Calendar.getInstance().getTime());
        assertEquals(current, output);
    }

    @ParameterizedTest
    @MultiStringsSource({"SwapExample", "0\n1\n"})
    @MultiStringsSource({"SwapExample2", "0\n1\n"})
    @MultiStringsSource({"SwapExample3", "0\n1\n"})
    void testCornerCases(String mainClass, String... expected) {
        Main.buildWorld("-cp", CP, "-m", mainClass);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        VM vm = new VM(World.get());
        vm.exec();
        // Normalize line endings across platforms
        String output = outputStream.toString()
                .replaceAll("\\r\\n|\\r|\\n", "\n");
        assertEquals(expected[0], output);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SSABranch",
            "SSALoop",
            "SSANestedBranch",
    })
    void testSSA(String mainClass) {
        Main.buildWorld("--ssa", "-cp", CP, "-m", mainClass);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(outputStream));
            VM vm = new VM(World.get());
            vm.exec();
        } finally {
            System.setOut(originalOut);
        }
        String output = outputStream.toString()
                .replaceAll("\\r\\n|\\r|\\n", "\n");
        assertEquals("OK\n", output);
    }

    @Test
    void testNotRunnable() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        try {
            Main.buildWorld("-cp", CP, "-m", "CornerCaseMayBeNotRunnable");
            VM vm = new VM(World.get());
            vm.exec();
        } catch (Exception ignored) {
        }
        String[] lines = outputStream.toString()
                .replaceAll("\\r\\n|\\r|\\n", "\n")
                .split("\n");
        String[] last2lines = Arrays.copyOfRange(lines, lines.length - 2, lines.length);
        assertArrayEquals(new String[]{"11", "6"}, last2lines);
    }
}
