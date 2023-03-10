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

package pascal.taie.frontend.soot;

import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.AnnotationElement;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.annotation.ArrayElement;
import pascal.taie.language.annotation.BooleanElement;
import pascal.taie.language.annotation.ClassElement;
import pascal.taie.language.annotation.DoubleElement;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.annotation.EnumElement;
import pascal.taie.language.annotation.FloatElement;
import pascal.taie.language.annotation.IntElement;
import pascal.taie.language.annotation.LongElement;
import pascal.taie.language.annotation.StringElement;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Lists;
import pascal.taie.util.collection.Maps;
import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.PrimType;
import soot.RefType;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.VoidType;
import soot.jimple.toolkits.typing.fast.BottomType;
import soot.tagkit.AbstractHost;
import soot.tagkit.AnnotationAnnotationElem;
import soot.tagkit.AnnotationArrayElem;
import soot.tagkit.AnnotationBooleanElem;
import soot.tagkit.AnnotationClassElem;
import soot.tagkit.AnnotationDoubleElem;
import soot.tagkit.AnnotationElem;
import soot.tagkit.AnnotationEnumElem;
import soot.tagkit.AnnotationFloatElem;
import soot.tagkit.AnnotationIntElem;
import soot.tagkit.AnnotationLongElem;
import soot.tagkit.AnnotationStringElem;
import soot.tagkit.AnnotationTag;
import soot.tagkit.ParamNamesTag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.tagkit.VisibilityParameterAnnotationTag;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static pascal.taie.language.type.BottomType.BOTTOM;
import static pascal.taie.language.type.VoidType.VOID;
import static pascal.taie.util.collection.Maps.newConcurrentMap;

/**
 * Converts Soot classes to Tai-e's representation.
 */
class Converter {

    private final JClassLoader loader;

    private final TypeSystem typeSystem;

    // Following four maps may be concurrently written during IR construction,
    // thus we use concurrent map to ensure their thread-safety.
    private final ConcurrentMap<SootField, JField> fieldMap
            = newConcurrentMap(4096);

    private final ConcurrentMap<SootMethod, JMethod> methodMap
            = newConcurrentMap(4096);

    private final ConcurrentMap<SootFieldRef, FieldRef> fieldRefMap
            = newConcurrentMap(4096);

    private final ConcurrentMap<SootMethodRef, MethodRef> methodRefMap
            = newConcurrentMap(4096);

    Converter(JClassLoader loader, TypeSystem typeSystem) {
        this.loader = loader;
        this.typeSystem = typeSystem;
    }

    Type convertType(soot.Type sootType) {
        if (sootType instanceof PrimType) {
            if (sootType instanceof ByteType) {
                return PrimitiveType.BYTE;
            } else if (sootType instanceof ShortType) {
                return PrimitiveType.SHORT;
            } else if (sootType instanceof IntType) {
                return PrimitiveType.INT;
            } else if (sootType instanceof LongType) {
                return PrimitiveType.LONG;
            } else if (sootType instanceof FloatType) {
                return PrimitiveType.FLOAT;
            } else if (sootType instanceof DoubleType) {
                return PrimitiveType.DOUBLE;
            } else if (sootType instanceof CharType) {
                return PrimitiveType.CHAR;
            } else if (sootType instanceof BooleanType) {
                return PrimitiveType.BOOLEAN;
            }
        } else if (sootType instanceof RefType) {
            return typeSystem.getClassType(loader, sootType.toString());
        } else if (sootType instanceof ArrayType arrayType) {
            return typeSystem.getArrayType(
                    convertType(arrayType.baseType),
                    arrayType.numDimensions);
        } else if (sootType instanceof VoidType) {
            return VOID;
        } else if (sootType instanceof BottomType){
            return BOTTOM;
        }
        throw new SootFrontendException("Cannot convert soot Type: " + sootType);
    }

    JClass convertClass(SootClass sootClass) {
        return loader.loadClass(sootClass.getName());
    }

    JField convertField(SootField sootField) {
        return fieldMap.computeIfAbsent(sootField, f ->
                new JField(convertClass(sootField.getDeclaringClass()),
                        sootField.getName(),
                        Modifiers.convert(sootField.getModifiers()),
                        convertType(sootField.getType()),
                        convertAnnotations(sootField)));
    }

    JMethod convertMethod(SootMethod sootMethod) {
        return methodMap.computeIfAbsent(sootMethod, m -> {
            List<Type> paramTypes = Lists.map(
                    m.getParameterTypes(), this::convertType);
            Type returnType = convertType(m.getReturnType());
            List<ClassType> exceptions = Lists.map(
                    m.getExceptions(),
                    sc -> (ClassType) convertType(sc.getType()));
            // TODO: convert attributes
            return new JMethod(convertClass(m.getDeclaringClass()),
                    m.getName(), Modifiers.convert(m.getModifiers()),
                    paramTypes, returnType, exceptions,
                    convertAnnotations(sootMethod),
                    convertParamAnnotations(sootMethod),
                    convertParamNames(sootMethod),
                    sootMethod
            );
        });
    }

    FieldRef convertFieldRef(SootFieldRef sootFieldRef) {
        return fieldRefMap.computeIfAbsent(sootFieldRef, ref -> {
            JClass cls = convertClass(ref.declaringClass());
            Type type = convertType(ref.type());
            return FieldRef.get(cls, ref.name(), type, ref.isStatic());
        });
    }

    MethodRef convertMethodRef(SootMethodRef sootMethodRef) {
        return methodRefMap.computeIfAbsent(sootMethodRef, ref -> {
            JClass cls = convertClass(ref.getDeclaringClass());
            List<Type> paramTypes = Lists.map(
                    ref.getParameterTypes(), this::convertType);
            Type returnType = convertType(ref.getReturnType());
            return MethodRef.get(cls, ref.getName(), paramTypes, returnType,
                    ref.isStatic());
        });
    }

    /**
     * @return an annotation holder that contains all annotations in {@code host}.
     * @see AbstractHost
     */
    static AnnotationHolder convertAnnotations(AbstractHost host) {
        var tag = (VisibilityAnnotationTag) host.getTag(VisibilityAnnotationTag.NAME);
        return convertAnnotations(tag);
    }

    /**
     * @return an annotation holder that contains all annotations in {@code tag}.
     * @see VisibilityAnnotationTag
     */
    private static AnnotationHolder convertAnnotations(
            @Nullable VisibilityAnnotationTag tag) {
        // in Soot, each VisibilityAnnotationTag may contain multiple annotations
        // (named AnnotationTag, which is a bit confusing).
        return tag == null || tag.getAnnotations() == null ?
                AnnotationHolder.emptyHolder() :
                // converts all annotations in tag
                AnnotationHolder.make(Lists.map(tag.getAnnotations(),
                        Converter::convertAnnotation));
    }

    private static Annotation convertAnnotation(AnnotationTag tag) {
        // AnnotationTag is the class that represent an annotation in Soot
        String annotationType = StringReps.toTaieTypeDesc(tag.getType());
        Map<String, Element> elements = Maps.newHybridMap();
        // converts all elements in tag
        tag.getElems().forEach(e -> {
            String name = e.getName();
            Element elem = convertAnnotationElement(e);
            elements.put(name, elem);
        });
        return new Annotation(annotationType, elements);
    }

    private static Element convertAnnotationElement(AnnotationElem elem) {
        if (elem instanceof AnnotationStringElem e) {
            return new StringElement(e.getValue());
        } else if (elem instanceof AnnotationClassElem e) {
            // FIXME: .java frontend and .class frontend have different
            //  representations for AnnotationClassElem, and current handling
            //  does not treat .java very well.
            return new ClassElement(StringReps.toTaieTypeDesc(e.getDesc()));
        } else if (elem instanceof AnnotationAnnotationElem e) {
            return new AnnotationElement(convertAnnotation(e.getValue()));
        } else if (elem instanceof AnnotationArrayElem e) {
            return new ArrayElement(Lists.map(e.getValues(),
                    Converter::convertAnnotationElement));
        } else if (elem instanceof AnnotationEnumElem e) {
            return new EnumElement(
                    StringReps.toTaieTypeDesc(e.getTypeName()),
                    e.getConstantName());
        } else if (elem instanceof AnnotationIntElem e) {
            return new IntElement(e.getValue());
        } else if (elem instanceof AnnotationBooleanElem e) {
            return new BooleanElement(e.getValue());
        } else if (elem instanceof AnnotationFloatElem e) {
            return new FloatElement(e.getValue());
        } else if (elem instanceof AnnotationDoubleElem e) {
            return new DoubleElement(e.getValue());
        } else if (elem instanceof AnnotationLongElem e) {
            return new LongElement(e.getValue());
        } else {
            throw new SootFrontendException(
                    "Unable to handle AnnotationElem: " + elem);
        }
    }

    /**
     * Converts all annotations of parameters of {@code sootMethod} to a list
     * of {@link AnnotationHolder}, one for annotations of each parameter.
     *
     * @see VisibilityParameterAnnotationTag
     */
    @Nullable
    private static List<AnnotationHolder> convertParamAnnotations(
            SootMethod sootMethod) {
        // in Soot, each VisibilityParameterAnnotationTag contains
        // the annotations for all parameters in the SootMethod
        var tag = (VisibilityParameterAnnotationTag)
                sootMethod.getTag(VisibilityParameterAnnotationTag.NAME);
        return tag == null ? null :
                Lists.map(tag.getVisibilityAnnotations(), Converter::convertAnnotations);
    }

    /**
     * Converts all names of parameters of {@code sootMethod} to a list.
     *
     * @see ParamNamesTag
     */
    @Nullable
    private static List<String> convertParamNames(
            SootMethod sootMethod) {
        // in Soot, each ParamNamesTag contains the names of all parameters in the SootMethod
        var tag = (ParamNamesTag) sootMethod.getTag(ParamNamesTag.NAME);
        return tag == null || tag.getNames().isEmpty() ? null : tag.getNames();
    }
}
