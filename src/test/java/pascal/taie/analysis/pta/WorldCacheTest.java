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


package pascal.taie.analysis.pta;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.frontend.cache.CachedIRBuilder;
import pascal.taie.frontend.cache.CachedWorldBuilder;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorldCacheTest {

    @Test
    void testWorldCache() {
        String[] args = {
                "-wc",
                "-java", "8",
                "-cp", "src/test/resources/pta/contextsensitivity",
                "-m", "LinkedQueue",
                "-a", """
                        pta=
                        cs:2-obj;
                        implicit-entries:false;
                        expected-file:src/test/resources/pta/contextsensitivity/LinkedQueue-pta-expected.txt;
                        only-app:true
                        """
        };
        Main.main(args);
        Main.main(args);
        World world2 = World.get();
        CachedWorldBuilder.getWorldCacheFile(world2.getOptions()).delete();
        assertTrue(world2.getIRBuilder() instanceof CachedIRBuilder);
    }

}
