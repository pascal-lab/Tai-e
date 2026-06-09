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

import java.util.List;
import java.util.Objects;

/**
 * Metadata of a Spring bean definition.
 */
public abstract class BeanDefinition {

    /**
     * A bean can have multiple names; this field records all of them.
     */
    protected final List<String> beanNames;

    /**
     * The class used to reference this bean, i.e., {@code getBean(jClass)}.
     * <ol>
     *   <li>For a CommonBean, {@code jClass} is the concrete implementation type.</li>
     *   <li>For a FactoryBean, {@code jClass} is the declared type, which may be the
     *       concrete implementation type or a parent class / interface type.</li>
     * </ol>
     */
    protected final JClass jClass;

    /**
     * Whether the bean instance corresponding to this definition is a singleton.
     */
    protected final boolean isSingleton;

    protected BeanDefinition(List<String> beanNames, JClass jClass, boolean isSingleton) {
        this.beanNames = beanNames;
        this.isSingleton = isSingleton;
        this.jClass = jClass;
    }

    protected BeanDefinition(String beanName, JClass jClass, boolean isSingleton) {
        this.beanNames = List.of(beanName);
        this.isSingleton = isSingleton;
        this.jClass = jClass;
    }

    public JClass getJClass() {
        return jClass;
    }

    public boolean isSingleton() {
        return isSingleton;
    }

    public List<String> getBeanNames() {
        return beanNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BeanDefinition bean)) {
            return false;
        }
        return Objects.equals(beanNames, bean.beanNames)
                && Objects.equals(jClass, bean.jClass)
                && isSingleton == bean.isSingleton;
    }

    @Override
    public int hashCode() {
        int result = beanNames.hashCode();
        result = 31 * result + jClass.hashCode();
        result = 31 * result + Boolean.hashCode(isSingleton);
        return result;
    }

    @Override
    public String toString() {
        return "Bean{"
                + "beanName=" + beanNames
                + ",jClass=" + jClass
                + ",isSingleton=" + isSingleton
                + '}';
    }

}
