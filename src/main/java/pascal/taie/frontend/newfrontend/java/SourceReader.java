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

package pascal.taie.frontend.newfrontend.java;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SourceReader {
    private static final Logger logger = LogManager.getLogger(SourceReader.class);
    public static Optional<char[]> readJavaSourceFile(String path) {
        try (FileInputStream stream = new FileInputStream(path)) {
            byte[] bytes = stream.readAllBytes();
            String temp = new String(bytes, StandardCharsets.UTF_8);
            return Optional.of(temp.toCharArray());
        }  catch (UnsupportedEncodingException e) {
            logger.error("UTF-8 is not supported, inner:" + e.getMessage());
            return Optional.empty();
        } catch (IOException e) {
            logger.error("IOException Encounter, inner:" + e.getMessage());
            return Optional.empty();
        }
    }
}
