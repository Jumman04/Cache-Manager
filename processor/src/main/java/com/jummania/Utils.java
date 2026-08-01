package com.jummania;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.Set;

class Utils {
    static final Set<TypeElement> annotatedClassNames = new HashSet<>();
    static final Set<String> types = new HashSet<>();
    static StringBuilder builder = new StringBuilder();
    static int packSize;

    static boolean vyTypeMirror(ProcessingEnvironment processingEnv, TypeMirror typeMirror, CharSequence name) {
        if (typeMirror == null) return false;
        javax.lang.model.util.Types typeUtils = processingEnv.getTypeUtils();
        javax.lang.model.util.Elements elementUtils = processingEnv.getElementUtils();
        Element element = elementUtils.getTypeElement(name);
        if (element == null) return false;

        String type = element.toString();
        if (types.add(type)) builder.insert(packSize, "import " + type + ";\n");

        return typeUtils.isAssignable(typeUtils.erasure(typeMirror), typeUtils.erasure(element.asType()));
    }

    static boolean isCollection(ProcessingEnvironment processingEnv, TypeMirror typeMirror, String fieldType) {
        if (fieldType != null && (fieldType.startsWith("java.util.List") || fieldType.startsWith("java.util.Set") || fieldType.startsWith("java.util.Collection"))) {
            return true;
        }
        return vyTypeMirror(processingEnv, typeMirror, "java.lang.Collection");
    }

    public static boolean isMap(ProcessingEnvironment processingEnv, TypeMirror typeMirror, String fieldType) {
        if (fieldType.startsWith("java.util.Map")) return true;
        return vyTypeMirror(processingEnv, typeMirror, "java.util.Map");
    }

    static boolean hasClass(String fieldType) {
        for (TypeElement annotatedElement : annotatedClassNames) {
            if (annotatedElement.getQualifiedName().toString().equals(fieldType)) {
                return true;
            }
        }
        return false;
    }

    static boolean isArray(TypeMirror typeMirror, String fieldType) {
        return fieldType.endsWith("[]") || typeMirror.getKind().name().equals("ARRAY");
    }

    public static String getGenericType(String fieldType) {
        int startIndex = fieldType.indexOf('<');
        int endIndex = fieldType.lastIndexOf('>');
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return fieldType.substring(startIndex + 1, endIndex).trim();
        }
        return fieldType;
    }

    static TypeElement getNestedTypeElement(ProcessingEnvironment processingEnv, String fieldType) {
        TypeElement element = processingEnv.getElementUtils().getTypeElement(fieldType);
        if (element != null) {
            return element;
        }

        String modifiedType = fieldType;
        while (modifiedType.contains(".")) {
            int lastDot = modifiedType.lastIndexOf('.');
            if (lastDot == -1) break;

            modifiedType = modifiedType.substring(0, lastDot) + "$" + modifiedType.substring(lastDot + 1);

            element = processingEnv.getElementUtils().getTypeElement(modifiedType);
            if (element != null) {
                return element;
            }
        }

        return null;
    }

    public static TypeMirror[] getTypeArguments(TypeMirror typeMirror) {
        if (typeMirror instanceof javax.lang.model.type.DeclaredType declaredType) {
            java.util.List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (!typeArguments.isEmpty()) {
                return typeArguments.toArray(new TypeMirror[0]);
            }
        }
        return new TypeMirror[0];
    }
}
