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

import pascal.taie.World;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.ByteType.BYTE;
import static pascal.taie.language.type.CharType.CHAR;
import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.FloatType.FLOAT;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;
import static pascal.taie.language.type.ShortType.SHORT;

/**
 * Utility class for the vm.
 * Mainly used to convert between JVM types/values and Tai-e types/values.
 */
class Utils {

    static final int INT_TRUE = 1;

    private static final int INT_FALSE = 0;

    private static final String GET_CLASS = "getClass";

    private static final String CLONE = "clone";

    private static final String EQUALS = "equals";

    private static final List<String> PRIMITIVE_TYPES = List.of(
            ClassNames.BOOLEAN, ClassNames.BYTE, ClassNames.CHARACTER,
            ClassNames.SHORT, ClassNames.INTEGER, ClassNames.FLOAT,
            ClassNames.LONG, ClassNames.DOUBLE);

    static boolean isGetClass(MethodRef ref) {
        return ref.getName().equals(GET_CLASS) && ref.getParameterTypes().isEmpty();
    }

    static boolean isClone(MethodRef ref) {
        return ref.getName().equals(CLONE) && ref.getParameterTypes().isEmpty();
    }

    static boolean isEquals(MethodRef ref) {
        return ref.getName().equals(EQUALS);
    }

    static Class<?> toJVMType(Type type) {
        if (type instanceof PrimitiveType) {
            if (type == BOOLEAN) {
                return boolean.class;
            } else if (type == BYTE) {
                return byte.class;
            } else if (type == CHAR) {
                return char.class;
            } else if (type == SHORT) {
                return short.class;
            } else if (type == INT) {
                return int.class;
            } else if (type == LONG) {
                return long.class;
            } else if (type == FLOAT) {
                return float.class;
            } else {
                return double.class;
            }
        } else if (type instanceof ClassType classType) {
            try {
                return Class.forName(classType.getName());
            } catch (ClassNotFoundException e) {
                throw new VMException(e);
            }
        } else if (type instanceof ArrayType arrayType) {
            Class<?> res = toJVMType(arrayType.baseType());
            for (int i = 0; i < arrayType.dimensions(); ++i) {
                res = res.arrayType();
            }
            return res;
        } else if (type == VoidType.VOID) {
            return void.class;
        } else {
            throw new VMException();
        }
    }

    static Class<?>[] toJVMTypeList(List<Type> typeList) {
        return typeList.stream().map(Utils::toJVMType)
                .toArray(Class[]::new);
    }

    static Object[] toJVMObjects(List<JValue> values, List<Type> targetType) {
        Object[] res = new Object[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            res[i] = typedToJVMObj(values.get(i), targetType.get(i));
        }
        return res;
    }

    static Object typedToJVMObj(JValue value, Type type) {
        if (value instanceof JPrimitive primitive) {
            if (primitive.toJVMObj() instanceof Integer i) {
                assert type instanceof PrimitiveType;
                return downCastInt(i, (PrimitiveType) type);
            }
        }
        assert World.get().getTypeSystem().isSubtype(type, value.getType());
        return value.toJVMObj();
    }

    static Object downCastInt(Integer i, PrimitiveType type) {
        if (type == BOOLEAN) {
            return toBoolean(i.byteValue());
        } else if (type == BYTE) {
            return i.byteValue();
        } else if (type == CHAR) {
            return (char) i.intValue();
        } else if (type == SHORT) {
            return i.shortValue();
        } else {
            return i;
        }
    }

    static int getIntValue(Object o) {
        if (o instanceof Boolean b) {
            return toInt(b);
        } else if (o instanceof Character c) {
            return c;
        } else if (o instanceof Short s) {
            return s;
        } else if (o instanceof Byte b) {
            return b;
        } else if (o instanceof Integer i) {
            return i;
        } else {
            throw new VMException();
        }
    }

    static Method toJVMMethod(JMethod method) {
        try {
            Method mtd = toJVMType(method.getDeclaringClass().getType())
                    .getDeclaredMethod(method.getName(), Utils.toJVMTypeList(method.getParamTypes()));
            mtd.setAccessible(true);
            return mtd;
        } catch (NoSuchMethodException e) {
            throw new VMException(e);
        }
    }

    static Field toJVMField(JField field) {
        Class<?> klass = Utils.toJVMType(field.getDeclaringClass().getType());
        try {
            Field f = klass.getDeclaredField(field.getName());
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new VMException(e);
        }
    }

    static boolean isJVMClass(Type type) {
        if (type instanceof ClassType ct) {
            JClass klass = ct.getJClass();
            assert klass != null;
            return klass.getName().startsWith("java.") ||
                    klass.getName().startsWith("org.junit.") ||
                    klass.getName().startsWith("org.gradle.") ||
                    isBoxedType(ct);
        } else if (type instanceof ArrayType at) {
            return isJVMClass(at.baseType());
        } else {
            return true;
        }
    }

    private static boolean isBoxedType(ClassType type) {
        return PRIMITIVE_TYPES.contains(type.getName());
    }

    static ClassType fromJVMClass(Class<?> klass) {
        return World.get().getTypeSystem().getClassType(klass.getName());
    }

    private static Type fromJVMType(Class<?> klass) {
        assert klass != null;
        if (klass == boolean.class) {
            return BOOLEAN;
        } else if (klass == char.class) {
            return CHAR;
        } else if (klass == byte.class) {
            return BYTE;
        } else if (klass == short.class) {
            return SHORT;
        } else if (klass == int.class) {
            return INT;
        } else if (klass == long.class) {
            return LONG;
        } else if (klass == float.class) {
            return FLOAT;
        } else if (klass == double.class) {
            return DOUBLE;
        } else if (klass.isArray()) {
            Class<?> arr = klass;
            int dimensionCount = 0;
            while (arr.isArray()) {
                arr = arr.getComponentType();
                dimensionCount++;
            }
            return World.get().getTypeSystem().getArrayType(fromJVMType(arr), dimensionCount);
        } else {
            return fromJVMClass(klass);
        }
    }

    private static JValue fromPrimitiveJVMObject(VM vm, Object o, Type type) {
        if (type instanceof PrimitiveType) {
            if (o instanceof Boolean b) {
                return JPrimitive.get(toInt(b));
            } else if (o instanceof Character c) {
                return JPrimitive.get(Integer.valueOf(c));
            } else if (o instanceof Byte b) {
                return JPrimitive.get(Integer.valueOf(b));
            } else if (o instanceof Short s) {
                return JPrimitive.get(Integer.valueOf(s));
            } else if (o instanceof Integer i) {
                return JPrimitive.get(i);
            } else if (o instanceof Float f) {
                return JPrimitive.get(f);
            } else if (o instanceof Long l) {
                return JPrimitive.get(l);
            } else if (o instanceof Double d) {
                return JPrimitive.get(d);
            }
        } else if (type instanceof ClassType) {
            return new JVMObject((JVMClassRep)
                    vm.loadClass(fromJVMClass(o.getClass())), o);
        }
        throw new VMException();
    }

    static JValue fromJVMObject(VM vm, Object o, Type type) {
        if (o == null) {
            return JNull.NULL;
        } else if (PRIMITIVE_TYPES.contains(o.getClass().getName())) {
            return fromPrimitiveJVMObject(vm, o, type);
        } else if (o instanceof JObject jObject) {
            return jObject;
        } else if (o.getClass().isArray()) {
            ArrayType at = (ArrayType) fromJVMType(o.getClass());
            int count = Array.getLength(o);
            JValue[] arr = new JValue[count];
            for (int i = 0; i < count; ++i) {
                Object oi = Array.get(o, i);
                arr[i] = fromJVMObject(vm, oi, at.elementType());
            }
            return new JArray(arr, at.baseType(), at.dimensions());
        } else if (type instanceof ClassType ct) {
            Class<?> klass = o.getClass();
            Class<?> klassDecl = toJVMType(ct);
            assert klassDecl.isAssignableFrom(klass);
            // TODO: fix this, check if JVM class uses correct type
            JClass jClass = World.get().getClassHierarchy().getClass(klass.getName());
            JClassRep klassObj;
            if (jClass != null) {
                klassObj = vm.loadJVMClass(klass);
            } else {
                klassObj = vm.loadClass(ct);
            }
            return new JVMObject((JVMClassRep) klassObj, o);
        }  else {
            throw new VMException();
        }
    }

    static MethodType toJVMMethodType(pascal.taie.ir.exp.MethodType methodType) {
        return java.lang.invoke.MethodType.methodType(
                Utils.toJVMType(methodType.getReturnType()),
                Utils.toJVMTypeList(methodType.getParamTypes()));
    }

    private static boolean toBoolean(byte b) {
        return (b & INT_TRUE) == INT_TRUE;
    }

    static int toInt(Boolean b) {
        return b ? INT_TRUE : INT_FALSE;
    }
}
