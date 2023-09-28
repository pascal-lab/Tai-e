package pascal.taie.interp;

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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class Utils {

    public static int INT_TRUE = 1;

    public static int INT_FALSE = 0;

    public static String GET_CLASS = "getClass";

    public static List<String> PRIMITIVE_TYPES = List.of(
            ClassNames.BOOLEAN, ClassNames.BYTE, ClassNames.CHARACTER,
            ClassNames.SHORT, ClassNames.INTEGER, ClassNames.FLOAT,
            ClassNames.LONG, ClassNames.DOUBLE);

    public static boolean isGetClass(MethodRef ref) {
        return ref.getName().equals(GET_CLASS) && ref.getParameterTypes().isEmpty();
    }

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
        } else if (t instanceof ClassType classType) {
            try {
                return Class.forName(classType.getName());
            } catch (ClassNotFoundException e) {
                throw new InterpreterException(e);
            }
        } else if (t instanceof ArrayType arrayType) {
            Class<?> res = toJVMType(arrayType.baseType());
            for (int i = 0; i < arrayType.dimensions(); ++i) {
                res = res.arrayType();
            }
            return res;
        } else {
            throw new InterpreterException();
        }
    }

    public static Class<?>[] toJVMTypeList(List<Type> typeList) {
        return typeList.stream().map(Utils::toJVMType)
                .toArray(Class[]::new);
    }

    public static Object[] toJVMObjects(List<JValue> values, List<Type> targetType) {
        Object[] res = new Object[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            res[i] = typedToJVMObj(values.get(i), targetType.get(i));
        }
        return res;
    }

    public static Object typedToJVMObj(JValue value, Type type) {
        if (value == null) {
            return null;
        }
        if (value instanceof JPrimitive primitive) {
            if (primitive.toJVMObj() instanceof Integer i) {
                assert type instanceof PrimitiveType;
                return downCastInt(i, (PrimitiveType) type);
            }
        }
        assert World.get().getTypeSystem().isSubtype(type, value.getType());
        return value.toJVMObj();
    }

    public static Object downCastInt(Integer i, PrimitiveType p) {
        return switch (p) {
            case BOOLEAN -> toBoolean(i.byteValue());
            case BYTE -> i.byteValue();
            case CHAR -> (char) i.intValue();
            case SHORT -> i.shortValue();
            case INT -> i;
            default -> throw new InterpreterException();
        };
    }

    public static int getIntValue(Object o) {
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
            throw new InterpreterException();
        }
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

    public static Field toJVMField(JField field) {
        Class<?> klass = Utils.toJVMType(field.getDeclaringClass().getType());
        try {
            Field f = klass.getDeclaredField(field.getName());
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
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
        assert klass != null;
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

    public static boolean toBoolean(byte b) {
        return (b & INT_TRUE) == INT_TRUE;
    }

    public static int toInt(Boolean b) {
        return b ? INT_TRUE : INT_FALSE;
    }
}
