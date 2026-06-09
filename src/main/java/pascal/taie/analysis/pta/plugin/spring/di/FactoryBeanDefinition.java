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

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.List;
import java.util.Objects;

/**
 * Represents a bean constructed by a user-defined factory method (a method annotated
 * with {@code @Bean} or referenced via the {@code factory-method} attribute in XML).
 * The actual type of such a bean can only be determined through pointer analysis.
 */
public class FactoryBeanDefinition extends BeanDefinition {

    /**
     * The user-defined bean factory method used to manually create the bean.
     */
    private final JMethod factoryMethod;

    FactoryBeanDefinition(List<String> beanNames, JClass jClass,
                          boolean isSingleton, JMethod factoryMethod) {
        super(beanNames, jClass, isSingleton);
        this.factoryMethod = factoryMethod;
    }

    public JMethod getFactoryMethod() {
        return factoryMethod;
    }

    @Override
    public String toString() {
        return "FactoryBeanDefinition{"
                + "beanName=" + beanNames
                + ",jClass=" + jClass
                + ",isSingleton=" + isSingleton
                + ",factoryMethod=" + factoryMethod
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FactoryBeanDefinition bean)) {
            return false;
        }
        return Objects.equals(beanNames, bean.beanNames)
                && Objects.equals(jClass, bean.jClass)
                && isSingleton == bean.isSingleton
                && Objects.equals(factoryMethod, bean.factoryMethod);
    }

    @Override
    public int hashCode() {
        int result = beanNames.hashCode();
        result = 31 * result + jClass.hashCode();
        result = 31 * result + Boolean.hashCode(isSingleton);
        result = 31 * result + factoryMethod.hashCode();
        return result;
    }

}
