package com.jummania;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.Set;

class Utils {
    static final Set<TypeElement> annotatedClassNames = new HashSet<>();

    public static String getIterableComponentType(TypeMirror typeMirror) {
        if (typeMirror instanceof javax.lang.model.type.DeclaredType declaredType) {
            java.util.List<? extends javax.lang.model.type.TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (!typeArguments.isEmpty()) {
                return typeArguments.get(0).toString();
            }
        }
        return "java.lang.Object"; // ফলব্যাক
    }

    public static boolean isIterable(ProcessingEnvironment processingEnv, TypeMirror typeMirror) {
        javax.lang.model.util.Types typeUtils = processingEnv.getTypeUtils();
        javax.lang.model.util.Elements elementUtils = processingEnv.getElementUtils();

        TypeMirror iterableType = elementUtils.getTypeElement("java.lang.Iterable").asType();

        return typeUtils.isAssignable(typeUtils.erasure(typeMirror), typeUtils.erasure(iterableType));
    }

    public static boolean isMap(ProcessingEnvironment processingEnv, TypeMirror typeMirror) {
        javax.lang.model.util.Types typeUtils = processingEnv.getTypeUtils();
        javax.lang.model.util.Elements elementUtils = processingEnv.getElementUtils();

        TypeMirror mapType = elementUtils.getTypeElement("java.util.Map").asType();

        return typeUtils.isAssignable(typeUtils.erasure(typeMirror), typeUtils.erasure(mapType));
    }

    public static String[] getMapKeyAndValueTypes(TypeMirror typeMirror) {
        if (typeMirror instanceof javax.lang.model.type.DeclaredType declaredType) {
            java.util.List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();

            if (typeArguments.size() >= 2) {
                return new String[]{typeArguments.get(0).toString(), typeArguments.get(1).toString()};
            }
        }
        return new String[]{"java.lang.Object", "java.lang.Object"};
    }

    public static String getArrayComponentType(TypeMirror typeMirror) {
        if (typeMirror instanceof ArrayType arrayType) {
            return arrayType.getComponentType().toString();
        }
        return "java.lang.Object"; // ফলব্যাক টাইপ
    }

    static boolean hasClass(String fieldType) {
        for (TypeElement annotatedElement : annotatedClassNames) {
            if (annotatedElement.getQualifiedName().toString().equals(fieldType)) {
                return true;
            }
        }
        return false;
    }

    static boolean isArray(String fieldType, TypeMirror typeMirror) {
        return fieldType.endsWith("[]") || typeMirror.getKind().name().equals("ARRAY");
    }
}
