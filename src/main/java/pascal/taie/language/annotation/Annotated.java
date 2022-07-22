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

package pascal.taie.language.annotation;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Represents objects that can be attached annotations.
 * <p>
 * Currently, only {@code JClass}, {@code JMethod}, and {@code JField}
 * implements this interface. Besides, annotations on parameters
 * are supported, and they are stored in {@code JMethod} instead of
 * parameters in IR.
 * <p>
 * TODO: let other program elements (e.g., {@code Var} implements
 *  this interface.
 */
public interface Annotated {

    /**
     * @return {@code true} if this annotated object has an annotation
     * of {@code annotationType}.
     */
    boolean hasAnnotation(String annotationType);

    /**
     * @return the {@link Annotation} of type {@code annotationType} if
     * it is present in this annotated; otherwise, {@code null} is returned.
     */
    @Nullable
    Annotation getAnnotation(String annotationType);

    /**
     * @return all annotations in this annotated object.
     */
    Collection<Annotation> getAnnotations();
}
