package pascal.taie.interp;

import pascal.taie.World;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

import java.lang.reflect.Method;
import java.util.List;

public class Utils {
    public static List<String> PRIMITIVE_TYPES = List.of(
            ClassNames.BOOLEAN, ClassNames.BYTE, ClassNames.CHARACTER,
            ClassNames.SHORT, ClassNames.INTEGER, ClassNames.FLOAT,
            ClassNames.LONG, ClassNames.DOUBLE);

    public static Class<?> toJVMType(Type t) {
        if (t instanceof PrimitiveType primitiveType) {
            return switch (primitiveType) {
                case BOOLEAN -> boolean.class;
                case BYTE -> byte.class;
                case CHAR -> char.class;
                case SHORT -> short.class;
                case INT -> int.class;
                case LONG -> long.class;
                case FLOAT -> float.class;
                case DOUBLE -> double.class;
            };
        } else if (t instanceof ReferenceType referenceType) {
            try {
                return Class.forName(referenceType.getName());
            } catch (ClassNotFoundException e) {
                throw new InterpreterException(e);
            }
        } else {
            throw new InterpreterException();
        }
    }

    public static Class<?>[] toJVMTypeList(List<Type> typeList) {
        return typeList.stream().map(Utils::toJVMType)
                .toArray(Class[]::new);
    }

    public static Object[] toJVMObjects(List<JValue> values) {
        return values.stream().map(JValue::toJVMObj)
                .toArray(Object[]::new);
    }

    public static Method toJVMMethod(JMethod method) {
        try {
            Method mtd = toJVMType(method.getDeclaringClass().getType())
                    .getDeclaredMethod(method.getName(), Utils.toJVMTypeList(method.getParamTypes()));
            mtd.setAccessible(true);
            return mtd;
        } catch (NoSuchMethodException e) {
            throw new InterpreterException(e);
        }
    }

    public static Class<?>[] toJVMTypeListFromValue(List<JValue> values) {
        return values.stream().map(JValue::getType)
                .map(Utils::toJVMType)
                .toArray(Class[]::new);
    }

    public static boolean isJVMClass(ClassType t) {
        JClass klass = t.getJClass();
        return klass.getName().startsWith("java.") ||
                isBoxedType(t);
    }

    public static boolean isBoxedType(ClassType t) {
        return PRIMITIVE_TYPES.contains(t.getName());
    }

    public static ClassType fromJVMClass(Class<?> klass) {
        return World.get().getTypeSystem().getClassType(klass.getName());
    }

    public static JValue fromPrimitiveJVMObject(VM vm, Object o, Type t) {
        if (t instanceof PrimitiveType) {
            if (o instanceof Boolean b) {
                return JPrimitive.get(b ? 0 : 1);
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
        } else if (t instanceof ClassType ct) {
            return new JVMObject((JVMClassObject) vm.loadClass(ct), o);
        }
        throw new InterpreterException();
    }

    public static JValue fromJVMObject(VM vm, Object o, Type t) {
        if (o == null) {
            return null;
        } else if (PRIMITIVE_TYPES.contains(o.getClass().getName())) {
            return fromPrimitiveJVMObject(vm, o, t);
        } else if (o instanceof JObject jObject) {
            return jObject;
        } else {
            assert t instanceof ClassType;
            ClassType ct = (ClassType) t;
            Class<?> klass = o.getClass();
            Class<?> klassDecl = toJVMType(ct);
            assert klassDecl.isAssignableFrom(klass);
            // TODO: fix this, check if jvm class
            // TODO: use correct type
            JClassObject klassObj;
            klassObj = vm.loadClass(ct);
            return new JVMObject((JVMClassObject) klassObj, o);
        }
    }

}
