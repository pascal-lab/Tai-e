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

package pascal.taie.frontend.java.main;

import pascal.taie.frontend.java.type.FrontendTypeSystem;

/**
 * Abstract base class for frontend components that require access to the type system.
 *
 * <p>A {@link NewFrontendComponent} is responsible for performing a specific task in the frontend,
 * such as building a closed world, class hierarchy, or intermediate representation (IR).
 * This class provides a common interface for accessing the type system.</p>
 */
public abstract class NewFrontendComponent {

    /**
     * The type system instance, which provides type-related services.
     */
    private final FrontendTypeSystem typeSystem;

    /**
     * Constructs a new instance of this component with the given type system.
     *
     * @param typeSystem the type system instance
     */
    protected NewFrontendComponent(FrontendTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    /**
     * Returns the type system instance.
     *
     * @return the type system instance
     */
    protected FrontendTypeSystem typeSystem() {
        return typeSystem;
    }
}
