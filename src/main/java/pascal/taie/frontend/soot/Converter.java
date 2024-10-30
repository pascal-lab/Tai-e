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
import pascal.taie.language.generics.GSignatures;
import pascal.taie.language.generics.MethodGSignature;
import pascal.taie.language.generics.ReferenceTypeGSignature;
import pascal.taie.language.type.ClassType;
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
import soot.tagkit.SignatureTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.tagkit.VisibilityParameterAnnotationTag;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.BottomType.BOTTOM;
import static pascal.taie.language.type.ByteType.BYTE;
import static pascal.taie.language.type.CharType.CHAR;
import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.FloatType.FLOAT;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;
import static pascal.taie.language.type.ShortType.SHORT;
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
                return BYTE;
            } else if (sootType instanceof ShortType) {
                return SHORT;
            } else if (sootType instanceof IntType) {
                return INT;
            } else if (sootType instanceof LongType) {
                return LONG;
            } else if (sootType instanceof FloatType) {
                return FLOAT;
            } else if (sootType instanceof DoubleType) {
                return DOUBLE;
            } else if (sootType instanceof CharType) {
                return CHAR;
            } else if (sootType instanceof BooleanType) {
                return BOOLEAN;
            }
        } else if (sootType instanceof RefType) {
            return typeSystem.getClassType(loader, sootType.toString());
        } else if (sootType instanceof ArrayType arrayType) {
            return typeSystem.getArrayType(
                    convertType(arrayType.baseType),
                    arrayType.numDimensions);
        } else if (sootType instanceof VoidType) {
            return VOID;
        } else if (sootType instanceof BottomType) {
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
                        convertGSignature(sootField),
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
                    convertGSignature(sootMethod),
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
     * @return the signature attribute for dealing with generics
     *         starting from Java 1.5.
     * @see ReferenceTypeGSignature
     */
    @Nullable
    private static ReferenceTypeGSignature convertGSignature(SootField sootField) {
        Tag tag = sootField.getTag("SignatureTag");
        if (tag instanceof SignatureTag signatureTag) {
            return GSignatures.toTypeSig(signatureTag.getSignature());
        }
        return null;
    }

    /**
     * @return the signature attribute for dealing with generics
     *         starting from Java 1.5.
     * @see MethodGSignature
     */
    @Nullable
    private static MethodGSignature convertGSignature(SootMethod sootMethod) {
        Tag tag = sootMethod.getTag("SignatureTag");
        if (tag instanceof SignatureTag signatureTag) {
            return GSignatures.toMethodSig(signatureTag.getSignature());
        }
        return null;
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
            String className = e.getDesc();
            // Soot's .java front end has different representation from .class
            // front end for AnnotationClassElem, and here we need to remove
            // extra characters generated by .java frontend
            int iBracket = className.indexOf('<');
            if (iBracket != -1) {
                className = className.replace("java/lang/Class<", "")
                        .replace(">", "");
            }
            return new ClassElement(StringReps.toTaieTypeDesc(className));
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
        // in Soot, each ParamNamesTag contains the names of all parameters
        // in the SootMethod, except the case explained below.
        var tag = (ParamNamesTag) sootMethod.getTag(ParamNamesTag.NAME);
        if (tag != null) {
            List<String> names = tag.getNames();
            if (names.size() == sootMethod.getParameterCount()) {
                // when using Soot's source (.java) front end to process
                // non-static inner class, the number of names in ParamNamesTag
                // may be **less than** the number of actually parameters
                // (lack of the implicit "this" variable for the outer instance).
                // For such case, we ignore ParamNamesTag to avoid mismatch
                // between numbers of parameter names and actual parameters.
                return names;
            }
        }
        return null;
    }
}
