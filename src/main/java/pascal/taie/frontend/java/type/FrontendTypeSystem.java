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

package pascal.taie.frontend.java.type;

import org.objectweb.asm.tree.LabelNode;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.type.AbstractTypeSystem;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static pascal.taie.frontend.java.type.Top.TOP;
import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.ByteType.BYTE;
import static pascal.taie.language.type.CharType.CHAR;
import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.FloatType.FLOAT;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;
import static pascal.taie.language.type.ShortType.SHORT;

/**
 * This type system provides more functionalities for frontend.
 * It includes type conversion from ASM types, type inference utilities,
 * and subtype checking methods.
 */
public class FrontendTypeSystem extends AbstractTypeSystem {

    private final Map<String, Pair<List<Type>, Type>> methodDescriptorCache = Maps.newConcurrentMap();

    public FrontendTypeSystem(JClassLoader defaultClassLoader) {
        super(defaultClassLoader, Maps.newConcurrentMap(1024), Maps.newConcurrentMap(8));
    }

    @Override
    public ClassType getClassType(JClassLoader loader, String className) {
        return getClassTypeByInternalName(className.replace('.', '/'));
    }

    public ClassType getClassTypeByInternalName(String internalName) {
        return classTypes.computeIfAbsent(internalName,
                name -> new ClassType(defaultClassLoader, name.replace('/', '.')));
    }

    // ==================== ASM Type Conversion Methods ====================

    /**
     * Convert ASM internal name to ReferenceType.
     */
    public ReferenceType fromAsmInternalName(String internalName) {
        if (internalName.charAt(0) != '[') {
            return getClassTypeByInternalName(internalName);
        }
        return (ReferenceType) fromAsmType(
                org.objectweb.asm.Type.getObjectType(internalName));
    }

    /**
     * Convert ASM type descriptor string to Type.
     */
    public Type fromAsmType(String descriptor) {
        return switch (descriptor.charAt(0)) {
            case 'V' -> VoidType.VOID;
            case 'Z' -> BOOLEAN;
            case 'C' -> CHAR;
            case 'B' -> BYTE;
            case 'S' -> SHORT;
            case 'I' -> INT;
            case 'F' -> FLOAT;
            case 'J' -> LONG;
            case 'D' -> DOUBLE;
            case '[' -> fromAsmType(org.objectweb.asm.Type.getType(descriptor));
            case 'L' -> getClassTypeByInternalName(
                    descriptor.substring(1, descriptor.length() - 1));
            default -> throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
        };
    }

    /**
     * Convert ASM Type object to Type.
     */
    public Type fromAsmType(org.objectweb.asm.Type t) {
        int sort = t.getSort();
        if (sort == org.objectweb.asm.Type.VOID) {
            return VoidType.VOID;
        } else if (sort < org.objectweb.asm.Type.ARRAY) {
            return switch (sort) {
                case org.objectweb.asm.Type.BOOLEAN -> BOOLEAN;
                case org.objectweb.asm.Type.BYTE -> BYTE;
                case org.objectweb.asm.Type.CHAR -> CHAR;
                case org.objectweb.asm.Type.SHORT -> SHORT;
                case org.objectweb.asm.Type.INT -> INT;
                case org.objectweb.asm.Type.LONG -> LONG;
                case org.objectweb.asm.Type.FLOAT -> FLOAT;
                case org.objectweb.asm.Type.DOUBLE -> DOUBLE;
                default -> throw new UnsupportedOperationException();
            };
        } else if (sort == org.objectweb.asm.Type.ARRAY) {
            return getArrayType(fromAsmType(t.getElementType()), t.getDimensions());
        } else if (sort == org.objectweb.asm.Type.OBJECT) {
            return getClassType(t.getClassName());
        } else {
            // t maybe a function ? error
            throw new IllegalArgumentException();
        }
    }

    /**
     * Convert ASM method descriptor to parameter types and return type.
     * Results are cached for performance.
     */
    public Pair<List<Type>, Type> fromAsmMethodType(String descriptor) {
        // normally we want to avoid using caching
        // but this method will be called very frequently
        // caching is able to save ~70% of calculation time
        return methodDescriptorCache.computeIfAbsent(descriptor, this::internalFromAsmMethodType);
    }

    private Pair<List<Type>, Type> internalFromAsmMethodType(String descriptor) {
        org.objectweb.asm.Type t = org.objectweb.asm.Type.getType(descriptor);
        return fromAsmMethodType(t);
    }

    private Pair<List<Type>, Type> fromAsmMethodType(org.objectweb.asm.Type t) {
        if (t.getSort() == org.objectweb.asm.Type.METHOD) {
            List<Type> paramTypes = new ArrayList<>();
            for (org.objectweb.asm.Type t1 : t.getArgumentTypes()) {
                paramTypes.add(fromAsmType(t1));
            }
            return new Pair<>(paramTypes, fromAsmType(t.getReturnType()));
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Convert ASM Type to MethodType.
     */
    public MethodType toMethodType(org.objectweb.asm.Type t) {
        Pair<List<Type>, Type> temp = fromAsmMethodType(t);
        return MethodType.get(temp.first(), temp.second());
    }

    /**
     * Convert ASM internal name to JClass.
     */
    public JClass toJClass(String internalName) {
        if (internalName.charAt(0) == '[') {
            return objectType().getJClass();
        } else {
            return getClassTypeByInternalName(internalName).getJClass();
        }
    }

    /**
     * Convert ASM frame type to Type.
     */
    public static Type fromAsmFrameType(Object o) {
        if (o instanceof Integer i) {
            return switch (i) {
                case 0 -> TOP; // Opcodes.Top
                case 1 -> INT; // Opcodes.INTEGER
                case 2 -> FLOAT; // Opcodes.FLOAT
                case 3 -> DOUBLE; // Opcodes.DOUBLE
                case 4 -> LONG; // Opcodes.LONG
                case 5 -> NullType.NULL; // Opcodes.NULL
                case 6 -> Uninitialized.UNINITIALIZED; // Opcodes.UNINITIALIZED_THIS
                default -> throw new UnsupportedOperationException();
            };
        } else if (o instanceof LabelNode) {
            return Uninitialized.UNINITIALIZED;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    // ==================== Type Checking Methods ====================

    @Override
    public boolean isSubtype(Type supertype, Type subtype) {
        assert subtype != null;
        if (subtype == supertype) {
            return true;
        } else if (subtype instanceof NullType) {
            return supertype instanceof ReferenceType;
        } else if (subtype instanceof ClassType) {
            if (supertype instanceof ClassType) {
                return isSubclass((ClassType) supertype, (ClassType) subtype);
            }
        } else if (subtype instanceof ArrayType) {
            if (supertype instanceof ClassType) {
                // JLS (11 Ed.), Chapter 10, Arrays
                return supertype == objectType ||
                        supertype == cloneableType ||
                        supertype == serializableType;
            } else if (supertype instanceof ArrayType superArray) {
                ArrayType subArray = (ArrayType) subtype;
                Type superBase = superArray.baseType();
                Type subBase = subArray.baseType();
                if (superArray.dimensions() == subArray.dimensions()) {
                    if (subBase.equals(superBase)) {
                        return true;
                    } else if (superBase instanceof ClassType &&
                            subBase instanceof ClassType) {
                        return isSubclass((ClassType) superBase, (ClassType) subBase);
                    }
                } else if (superArray.dimensions() < subArray.dimensions()) {
                    return superBase == objectType ||
                            superBase == cloneableType ||
                            superBase == serializableType;
                }
            }
        }
        return false;
    }

    private static boolean isSubclass(ClassType supertype, ClassType subtype) {
        JClass subClass = subtype.getJClass();
        assert subClass != null;
        JClass superClass = supertype.getJClass();
        if (subClass.getName().equals(ClassNames.OBJECT)) {
            return subClass == superClass;
        }
        if (!subClass.isInterface() && !superClass.isInterface()) {
            return isSubclassOnly(subClass, superClass);
        } else if (subClass.isInterface() && !superClass.isInterface()) {
            return subClass.getSuperClass() == superClass;
        } else {
            return isSubInterfaces(subClass, superClass);
        }
    }

    private static boolean isSubclassOnly(JClass subClass, JClass superClass) {
        assert !subClass.isInterface() && !superClass.isInterface();
        JClass realSuperClass = subClass.getSuperClass();
        if (realSuperClass == null) {
            return false;
        }
        return realSuperClass == superClass || isSubclassOnly(realSuperClass, superClass);
    }

    private static boolean isSubInterfaces(JClass subClass, JClass superClass) {
        assert superClass.isInterface();
        JClass realSuperClass = subClass.getSuperClass();
        if (realSuperClass == null) {
            return false;
        }
        if (isSubInterfaces(realSuperClass, superClass)) {
            return true;
        }
        for (JClass i : subClass.getInterfaces()) {
            if (i == superClass || isSubInterfaces(i, superClass)) {
                return true;
            }
        }
        return false;
    }

    // ==================== LCA (Least Common Ancestor) Methods ====================

    public Set<ReferenceType> lca(ReferenceType t1, ReferenceType t2) {
        assert !(t1 != t2 && t1.equals(t2));
        if (t1 == t2) {
            return Set.of(t1);
        } else if (t1 instanceof NullType) {
            return Set.of(t2);
        } else if (t2 instanceof NullType) {
            return Set.of(t1);
        } else if (t1 instanceof ClassType ct1 && t2 instanceof ClassType ct2) {
            Set<ClassType> upper1 = upperClosure(ct1);
            Set<ClassType> upper2 = upperClosure(ct2);
            if (upper2.contains(ct1)) {
                return Set.of(ct1);
            } else if (upper1.contains(ct2)) {
                return Set.of(ct2);
            } else {
                intersection(upper1, upper2);
                return minimum(upper1);
            }
        } else if (t1 instanceof ClassType ct1 && t2 instanceof ArrayType) {
            Set<ClassType> upper1 = upperClosure(ct1);
            Set<ClassType> upper2 = getArraySupers();
            intersection(upper1, upper2);
            return minimum(upper1);
        } else if (t1 instanceof ArrayType && t2 instanceof ClassType) {
            return lca(t2, t1);
        } else if (t1 instanceof ArrayType at1 && t2 instanceof ArrayType at2) {
            if (at1.elementType() instanceof PrimitiveType
                    || at2.elementType() instanceof PrimitiveType) {
                return Collections.unmodifiableSet(getArraySupers());
            } else {
                ReferenceType r1 = (ReferenceType) at1.elementType();
                ReferenceType r2 = (ReferenceType) at2.elementType();
                return lca(r1, r2).stream()
                        .map(this::wrap1)
                        .collect(Collectors.toSet());
            }
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Compute LCA for multiple types.
     * @param types null and uninitialized type should be removed
     */
    public Set<ReferenceType> lca(Set<ReferenceType> types) {
        if (types.size() <= 1) {
            return types;
        } else {
            if (allRefArray(types)) {
                return lca(types.stream().map(t -> (ReferenceType) ((ArrayType) t).elementType())
                        .collect(Collectors.toSet()))
                        .stream()
                        .map(this::wrap1)
                        .collect(Collectors.toSet());
            }
            Set<ClassType> res = null;
            for (ReferenceType t : types) {
                Set<ClassType> current;
                if (t instanceof NullType) {
                    continue;
                } else if (t instanceof ArrayType) {
                    current = Sets.newSet(getArraySupers());
                } else {
                    current = upperClosure((ClassType) t);
                }
                if (res == null) {
                    res = current;
                } else {
                    intersection(res, current);
                }
            }
            assert res != null;
            return minimum(res);
        }
    }

    private Set<ReferenceType> minimum(Set<ClassType> in) {
        Set<ClassType> removed = Sets.newHybridSet();
        for (ClassType t1 : in) {
            if (!removed.contains(t1)) {
                Set<ClassType> upper = upperClosure(t1);
                upper.remove(t1);
                removed.addAll(upper);
            }
        }
        in.removeAll(removed);
        return Collections.unmodifiableSet(in);
    }

    private Set<ClassType> getArraySupers() {
        return Set.of(objectType(), cloneableType(), serializableType());
    }

    /**
     * s1 <- s1 ∩ s2
     */
    private <T> void intersection(Set<T> s1, Set<T> s2) {
        s1.removeIf(t -> !s2.contains(t));
    }

    private boolean allRefArray(Set<ReferenceType> types) {
        return types.stream().allMatch(t -> t instanceof ArrayType arrayType
                && arrayType.elementType() instanceof ReferenceType);
    }

    /**
     * Wrap a reference type into an array type with one more dimension.
     */
    public ArrayType wrap1(ReferenceType referenceType) {
        if (referenceType instanceof ArrayType at) {
            return getArrayType(at.baseType(), at.dimensions() + 1);
        } else {
            return getArrayType(referenceType, 1);
        }
    }

    /**
     * Get the upper closure (all supertypes) of a class type.
     */
    private Set<ClassType> upperClosure(ClassType type) {
        Queue<JClass> workList = new LinkedList<>();
        workList.add(type.getJClass());
        Set<ClassType> res = Sets.newHybridSet();
        while (!workList.isEmpty()) {
            JClass now = workList.poll();
            assert now != null;
            if (!res.contains(now.getType())) {
                workList.addAll(getAllDirectSuperType(now));
                res.add(now.getType());
            }
        }
        return res;
    }

    private List<JClass> getAllDirectSuperType(JClass type) {
        List<JClass> res = new ArrayList<>(type.getInterfaces());
        if (type.getSuperClass() != null) {
            res.add(type.getSuperClass());
        }
        return res;
    }
}
