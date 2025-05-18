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

package pascal.taie.frontend.newfrontend;

import pascal.taie.frontend.newfrontend.main.DefaultIRBuilder;
import pascal.taie.frontend.newfrontend.hierarchy.DefaultClassLoader;
import pascal.taie.frontend.newfrontend.main.TaiePhase;
import pascal.taie.frontend.newfrontend.report.FrontendStats;
import pascal.taie.frontend.newfrontend.source.AsmSource;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.ByteType.BYTE;
import static pascal.taie.language.type.CharType.CHAR;
import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.FloatType.FLOAT;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;
import static pascal.taie.language.type.ShortType.SHORT;

/**
 * The context for frontend processing. Can be viewed as global state of
 * the new frontend.
 */
public class FrontendContext {

    private DefaultClassLoader defaultClassLoader;

    private TempTypeSystem typeSystem;

    private ClassHierarchy hierarchy;

    private TypeContext typeContext;

    private final boolean useSSA;

    private final DefaultIRBuilder irBuilder;

    private final Map<String, Pair<List<Type>, Type>> methodDescriptorCache = Maps.newConcurrentMap();

    private TaiePhase phase;

    private FrontendStats stats;

    public FrontendContext(boolean useSSA) {
        this.useSSA = useSSA;
        this.irBuilder = new DefaultIRBuilder(this);
    }

    public static FrontendContext get() {
//        assert buildContext != null;
//        return buildContext;
        throw new UnsupportedOperationException();
    }

    public void initClassloaderAndTypeSystem(DefaultClassLoader loader) {
        this.defaultClassLoader = loader;
        this.typeSystem = new TempTypeSystem(this, defaultClassLoader);
        this.typeContext = new TypeContext(typeSystem);
    }

    public void initHierarchy(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    public TypeSystem getTypeSystem() {
        return typeSystem;
    }

    public DefaultIRBuilder getIRBuilder() {
        return irBuilder;
    }

    public TypeContext getTypeContext() {
        return typeContext;
    }

    public JClass getClassByName(String name) {
        JClass klass = defaultClassLoader.loadClass(name);
        assert klass != null;
        return klass;
    }

    public ReferenceType fromAsmInternalName(String internalName) {
        if (internalName.charAt(0) != '[') {
            return typeSystem.getClassTypeByInternalName(internalName);
        }
        return (ReferenceType) fromAsmType(
                org.objectweb.asm.Type.getObjectType(internalName));
    }

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
            case 'L' -> typeSystem.getClassTypeByInternalName(
                    descriptor.substring(1, descriptor.length() - 1));
            default -> throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
        };
    }

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
            return typeSystem.getArrayType(fromAsmType(t.getElementType()), t.getDimensions());
        } else if (sort == org.objectweb.asm.Type.OBJECT) {
            return typeSystem.getClassType(t.getClassName());
        } else {
            // t maybe a function ? error
            throw new IllegalArgumentException();
        }
    }

    public Pair<List<Type>, Type> fromAsmMethodType(String descriptor) {
        // normally we want to avoid using caching
        // but this method will be called very frequently
        // caching is able to save ~70% of calculation time
        return methodDescriptorCache.computeIfAbsent(descriptor, this::internalFromAsmMethodType);
    }

    public Pair<List<Type>, Type> internalFromAsmMethodType(String descriptor) {
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

    public MethodType toMethodType(org.objectweb.asm.Type t) {
        Pair<List<Type>, Type> temp = fromAsmMethodType(t);
        return MethodType.get(temp.first(), temp.second());
    }

    public JClass toJClass(String internalName) {
        if (internalName.charAt(0) == '[') {
            return typeContext.object().getJClass();
        } else {
            return typeSystem.getClassTypeByInternalName(internalName).getJClass();
        }
    }

    public void noticeClassSource(JClass clazz, AsmSource source) {
        irBuilder.putClassSource(clazz, source);
    }

    public ClassHierarchy getClassHierarchy() {
        return hierarchy;
    }

    public FrontendStats getFrontendStats() {
        return stats;
    }

    public void setPhase(TaiePhase phase) {
        this.phase = phase;
    }

    public void setStats(FrontendStats stats) {
        this.stats = stats;
    }

    public TaiePhase getPhase() {
        return phase;
    }

    public boolean isUseSSA() {
        return useSSA;
    }
}
