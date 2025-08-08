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

package pascal.taie.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceSamplerTest {

    @Test
    void unitTest() throws Exception {
        File outputDir = new File("output");
        File outputFile = new File(outputDir, PerformanceSampler.OUTPUT_FILE);
        outputFile.delete();
        PerformanceSampler sampler = new PerformanceSampler(outputDir);
        sampler.start();
        Thread.sleep(1000);
        sampler.stop();
        // check the output file
        assertTrue(outputFile.exists());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(outputFile);
        assertNotNull(json.get("startTime"));
        assertNotNull(json.get("finishTime"));
        assertFalse(json.get("version").asText().isBlank());
        assertFalse(json.get("commit").asText().isBlank());
        assertFalse(json.get("samples").isEmpty());
    }

    @Test
    void integrationTest() {
        pascal.taie.Main.main(
                "--performance-sampling",
                "-pp",
                "-cp", "src/test/resources/pta/basic",
                "-m", "New",
                "-a", "pta=implicit-entries:false;only-app:true;"
        );
    }

}
