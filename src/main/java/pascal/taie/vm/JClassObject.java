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

package pascal.taie.vm;

import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;

import java.util.List;
import java.util.Map;

public class JClassObject {

    ClassType type;

    private final Map<String, JValue> staticFields;

    public JClassObject(ClassType type) {
        this.type = type;
        staticFields = Maps.newMap();
    }

    @Override
    public String toString() {
        return "JClassObj: [" + this.type + "]";
    }

    public JValue getStaticField(VM vm, FieldRef ref) {
        return staticFields.get(ref.getName());
    }

    public void setStaticField(VM vm, FieldRef ref, JValue value) {
        staticFields.put(ref.getName(), value);
    }

    public JValue invokeStatic(VM vm, JMethod method, List<JValue> values) {
        Map<Var, JValue> args = Maps.newMap();
        IR ir = method.getIR();
        for (int i = 0; i < method.getParamCount(); ++i) {
            args.put(ir.getParam(i), values.get(i));
        }
        Frame newFrame = Frame.makeNewFrame(args);
        return vm.execIR(ir, newFrame);
    }
}
