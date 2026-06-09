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

import java.util.Objects;

/**
 * Metadata of a common Spring bean (a bean automatically created by the Spring framework).
 */
public class CommonBeanDefinition extends BeanDefinition {

    /**
     * The constructor invoked to create this bean.
     */
    private final JMethod constructor;

    CommonBeanDefinition(String beanName, JClass jClass, boolean isSingleton, JMethod constructor) {
        super(beanName, jClass, isSingleton);
        this.constructor = constructor;
    }

    public JMethod getConstructor() {
        return constructor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommonBeanDefinition bean)) {
            return false;
        }
        return super.equals(o)
                && Objects.equals(constructor, bean.constructor);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + constructor.hashCode();
    }

    @Override
    public String toString() {
        return "CommonBeanDefinition{"
                + "beanName=" + beanNames
                + ",jClass=" + jClass
                + ",isSingleton=" + isSingleton
                + ",constructor=" + constructor
                + '}';
    }

}
