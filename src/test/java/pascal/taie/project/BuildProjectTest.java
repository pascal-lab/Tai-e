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

package pascal.taie.project;


import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

public class BuildProjectTest {

    @Test
    void loadZip() throws IOException {
        FileContainer[] c = new FileContainer[1];
        FileLoader loader = FileLoader.get();
        String classes = "src/test/resources/world/classes.jar";
        loader.loadFile(Path.of(classes), null, (a) -> null, a -> {
            c[0] = a;
            return null;
        });

        assertNotNull(c[0]);
        for (var i : c[0].getFiles()) {
            if (i.getFileName().equals("Cards.class")) {
                assertSame(i.getRootContainer(), c[0]);
                String cards = "src/test/resources/world/Cards.class";
                assertArrayEquals(i.getResource().getContent(),
                        Files.readAllBytes(Path.of(cards)));
                return;
            }
        }
        fail();
    }
}
