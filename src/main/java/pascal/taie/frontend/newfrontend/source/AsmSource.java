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

package pascal.taie.frontend.newfrontend.source;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public record AsmSource(
        ClassReader r,
        boolean isApplication,
        int version,
        ClassNode node
) implements ClassSource {

    @Override
    public String getClassName() {
        String name;
        if (r == null) {
            name = node.name;
        } else {
            name = r.getClassName();
        }
        return Type.getObjectType(name).getClassName();
    }

    /**
     * @return the class file version of current class file
     */
    public int getClassFileVersion() {
        // some hack here
        // 6 is the offset of classfile version
        return version;
    }
}
