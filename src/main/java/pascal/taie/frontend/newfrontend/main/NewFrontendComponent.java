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

package pascal.taie.frontend.newfrontend.main;

import pascal.taie.frontend.newfrontend.FrontendContext;
import pascal.taie.frontend.newfrontend.TypeContext;
import pascal.taie.language.type.TypeSystem;

/**
 * Abstract base class for frontend components that require access to the build context.
 *
 * <p>A {@link NewFrontendComponent} is responsible for performing a specific task in the frontend,
 * such as building a closed world, class hierarchy, or intermediate representation (IR).
 * This class provides a common interface for accessing the build context and related services.</p>
 *
 * @see FrontendContext
 */
public abstract class NewFrontendComponent {

    /**
     * The build context instance, which provides access to various services and data structures.
     */
    private final FrontendContext context;

    /**
     * Constructs a new instance of this component with the given build context.
     *
     * @param context the build context instance
     */
    protected NewFrontendComponent(FrontendContext context) {
        this.context = context;
    }

    /**
     * Returns the build context instance
     *
     * @return the build context instance
     */
    protected FrontendContext ctx() {
        return context;
    }

    protected TaiePhase getTaiePhase() {
        return null;
    }

    protected TypeContext tCtx() {
        return context.getTypeContext();
    }

    protected TypeSystem typeSystem() {
        return context.getTypeSystem();
    }
}
