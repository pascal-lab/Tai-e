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

package pascal.taie.analysis.pta.plugin.spring.di;

import pascal.taie.util.collection.Sets;
import pascal.taie.analysis.pta.plugin.spring.util.AbstractResultProcessor;

import javax.annotation.Nullable;
import java.util.Set;

public class DiResultProcessor extends AbstractResultProcessor<BeanDefinition, DiResultProcessor.BeanDefinitionDto> {

    public static final String RESULT_FILE_NAME = "di.json";

    static final DiResultProcessor INSTANCE = new DiResultProcessor();

    @Override
    protected String resultFileName() {
        return "di.json";
    }

    @Override
    protected String entityName() {
        return "bean definitions";
    }

    @Override
    protected BeanDefinitionDto convertToDto(BeanDefinition bean) {
        BeanDefinitionDto dto = new BeanDefinitionDto();
        Set<String> sortedBeanNames = Sets.newOrderedSet();
        sortedBeanNames.addAll(bean.getBeanNames());
        dto.setBeanNames(sortedBeanNames);
        dto.setJClassName(bean.getJClass().getName());
        dto.setSingleton(bean.isSingleton());

        if (bean instanceof CommonBeanDefinition commonBean) {
            dto.setType("CommonBean");
            dto.setConstructorSignature(commonBean.getConstructor().toString());
        } else if (bean instanceof FactoryBeanDefinition factoryBean) {
            dto.setType("FactoryBean");
            dto.setFactoryMethodSignature(factoryBean.getFactoryMethod().toString());
        }

        return dto;
    }

    @Override
    protected String getSortKey(BeanDefinitionDto dto) {
        return dto.getBeanNames().iterator().next();
    }

    // DTO

    static class BeanDefinitionDto {
        private String type;
        private Set<String> beanNames;
        private String jClassName;
        private boolean isSingleton;
        @Nullable private String constructorSignature = null;
        @Nullable private String factoryMethodSignature = null;

        public BeanDefinitionDto() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Set<String> getBeanNames() {
            return beanNames;
        }

        public void setBeanNames(Set<String> beanNames) {
            this.beanNames = beanNames;
        }

        public String getJClassName() {
            return jClassName;
        }

        public void setJClassName(String jClassName) {
            this.jClassName = jClassName;
        }

        public boolean isSingleton() {
            return isSingleton;
        }

        public void setSingleton(boolean singleton) {
            isSingleton = singleton;
        }

        @Nullable
        public String getConstructorSignature() {
            return constructorSignature;
        }

        public void setConstructorSignature(String s) {
            this.constructorSignature = s;
        }

        @Nullable
        public String getFactoryMethodSignature() {
            return factoryMethodSignature;
        }

        public void setFactoryMethodSignature(String s) {
            this.factoryMethodSignature = s;
        }

    }

}
