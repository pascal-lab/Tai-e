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

package pascal.taie.analysis.pta.plugin.spring.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pascal.taie.World;
import pascal.taie.analysis.pta.plugin.spring.SpringAnalysis;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.util.AnalysisException;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for analysis result processors that dump results to JSON.
 *
 * @param <S> source domain object type (e.g., BeanDefinition, WebEndpoint)
 * @param <D> DTO type for serialization
 */
public abstract class AbstractResultProcessor<S, D> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    protected abstract String resultFileName();

    protected abstract String entityName();

    protected abstract D convertToDto(S source);

    protected abstract String getSortKey(D dto);

    public void process(AnalysisOptions options, List<S> sources) {
        if (SpringAnalysis.shouldDumpResults()) {
            dumpToFile(sources, World.get().getOptions().getOutputDir());
        }
    }

    public void dumpToFile(List<S> sources, File outputDir) {
        List<D> dtos = sources.stream()
                .map(this::convertToDto)
                .sorted(Comparator.comparing(this::getSortKey))
                .toList();

        File outputFile = new File(outputDir, resultFileName());

        try {
            objectMapper.writeValue(outputFile, dtos);
            logger.info("Successfully dumped {} {} to: {}",
                    sources.size(), entityName(), outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to dump output {}", outputFile, e);
            throw new AnalysisException("Failed to dump " + entityName()
                    + " to " + outputFile, e);
        }
    }

}
