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

package pascal.taie.analysis.pta.plugin.spring.wec;

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.analysis.pta.plugin.spring.util.AbstractResultProcessor;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class WecResultProcessor extends AbstractResultProcessor<WebEndpoint, WecResultProcessor.WecDto> {

    public static final String RESULT_FILE_NAME = "wec.json";

    static final WecResultProcessor INSTANCE = new WecResultProcessor();

    @Override
    protected String resultFileName() {
        return RESULT_FILE_NAME;
    }

    @Override
    protected String entityName() {
        return "web endpoints";
    }

    @Override
    protected WecDto convertToDto(WebEndpoint wec) {
        WecDto dto = new WecDto();
        dto.setContainingClass(wec.containingClass().getName());
        dto.setHandlerMethod(wec.handlerMethod().getSignature());
        Set<String> sortedPaths = Sets.newOrderedSet();
        sortedPaths.addAll(wec.paths());
        dto.setPaths(sortedPaths);
        Set<String> sortedRequestMethods = Sets.newOrderedSet();
        sortedRequestMethods.addAll(
                wec.requestMethods().stream().map(Enum::toString).collect(Collectors.toSet()));
        dto.setRequestMethod(sortedRequestMethods);
        Map<String, Integer> sortedParamMap = Maps.newOrderedMap();
        sortedParamMap.putAll(wec.paramName2ParamIndex());
        dto.setParamName2ParamIndex(sortedParamMap);
        return dto;
    }

    @Override
    protected String getSortKey(WecDto dto) {
        return dto.getContainingClass() + dto.getHandlerMethod();
    }

    // DTO

    static class WecDto {
        private String containingClass;
        private String handlerMethod;
        private Set<String> paths;
        private Set<String> requestMethod;
        private Map<String, Integer> paramName2ParamIndex;

        public String getContainingClass() {
            return containingClass;
        }

        public void setContainingClass(String s) {
            this.containingClass = s;
        }

        public String getHandlerMethod() {
            return handlerMethod;
        }

        public void setHandlerMethod(String s) {
            this.handlerMethod = s;
        }

        public Set<String> getPaths() {
            return paths;
        }

        public void setPaths(Set<String> s) {
            this.paths = s;
        }

        public Set<String> getRequestMethod() {
            return requestMethod;
        }

        public void setRequestMethod(Set<String> s) {
            this.requestMethod = s;
        }

        public Map<String, Integer> getParamName2ParamIndex() {
            return paramName2ParamIndex;
        }

        public void setParamName2ParamIndex(Map<String, Integer> m) {
            this.paramName2ParamIndex = m;
        }
    }

}
