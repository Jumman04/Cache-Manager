package com.jummania;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
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

    public static boolean isIterable(ProcessingEnvironment processingEnv, TypeMirror typeMirror, String fieldType) {
        if (fieldType.startsWith("java.util.List") || fieldType.startsWith("java.util.Set") || fieldType.startsWith("java.util.Collection") | fieldType.startsWith("java.lang.Iterable")) {
            return true;
        }
        javax.lang.model.util.Types typeUtils = processingEnv.getTypeUtils();
        javax.lang.model.util.Elements elementUtils = processingEnv.getElementUtils();

        TypeMirror iterableType = elementUtils.getTypeElement("java.lang.Iterable").asType();

        return typeUtils.isAssignable(typeUtils.erasure(typeMirror), typeUtils.erasure(iterableType));
    }

    public static boolean isMap(ProcessingEnvironment processingEnv, TypeMirror typeMirror, String fieldType) {
        if (fieldType.startsWith("java.util.Map")) return true;
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

    // জেনরিক ব্র্যাকেটের ভেতর থেকে টাইপ বের করার মেথড
    public static String getGenericType(String fieldType) {
        int startIndex = fieldType.indexOf('<');
        int endIndex = fieldType.lastIndexOf('>');
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return fieldType.substring(startIndex + 1, endIndex).trim();
        }
        return fieldType; // যদি জেনরিক না হয়, তবে ফিল্ড টাইপটিই রিটার্ন করবে
    }

    public static TypeMirror getInnerTypeMirror(ProcessingEnvironment processingEnv, String genericTypeStr) {
        int startIndex = genericTypeStr.indexOf('<');
        int endIndex = genericTypeStr.lastIndexOf('>');

        String targetType = genericTypeStr;
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            targetType = genericTypeStr.substring(startIndex + 1, endIndex).trim();
        }

        // যদি ভেতরের টাইপটি নিজেই একটি জেনরিক হয় (যেমন List<E>)
        if (targetType.contains("<")) {
            int innerStart = targetType.indexOf('<');
            String rawOuter = targetType.substring(0, innerStart).trim();
            String innerGeneric = targetType.substring(innerStart + 1, targetType.lastIndexOf('>')).trim();

            TypeElement outerElement = processingEnv.getElementUtils().getTypeElement(rawOuter);
            TypeMirror innerMirror = getInnerTypeMirror(processingEnv, targetType); // রিকার্সিভ কল

            if (outerElement != null) {
                javax.lang.model.util.Types typeUtils = processingEnv.getTypeUtils();
                // নেস্টেড জেনরিকের জন্য DeclaredType তৈরি করা
                return typeUtils.getDeclaredType(outerElement, innerMirror);
            }
        }

        return getTypeMirror(processingEnv, targetType);
    }

    public static TypeMirror getTypeMirror(ProcessingEnvironment processingEnv, String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            throw new RuntimeException("TypeName cannot be null or empty");
        }

        // জেনরিক টাইপ ভ্যারিয়েবল চেক (যেমন E, T)
        if (typeName.length() <= 2 && typeName.matches("[A-Z]")) {
            TypeElement objElement = processingEnv.getElementUtils().getTypeElement("java.lang.Object");
            if (objElement != null) {
                return objElement.asType();
            }
        }

        // জেনরিক টাইপ হলে ব্র্যাকেটের আগের অংশ নেওয়া
        int genericIndex = typeName.indexOf('<');
        String rawTypeName = (genericIndex != -1) ? typeName.substring(0, genericIndex).trim() : typeName;

        javax.lang.model.util.Types typeUtils = processingEnv.getTypeUtils();

        // প্রিমি티브 টাইপ চেক
        switch (rawTypeName) {
            case "int" -> {
                return typeUtils.getPrimitiveType(TypeKind.INT);
            }
            case "long" -> {
                return typeUtils.getPrimitiveType(TypeKind.LONG);
            }
            case "short" -> {
                return typeUtils.getPrimitiveType(TypeKind.SHORT);
            }
            case "byte" -> {
                return typeUtils.getPrimitiveType(TypeKind.BYTE);
            }
            case "char" -> {
                return typeUtils.getPrimitiveType(TypeKind.CHAR);
            }
            case "boolean" -> {
                return typeUtils.getPrimitiveType(TypeKind.BOOLEAN);
            }
            case "float" -> {
                return typeUtils.getPrimitiveType(TypeKind.FLOAT);
            }
            case "double" -> {
                return typeUtils.getPrimitiveType(TypeKind.DOUBLE);
            }
        }

        // অবজেক্ট বা ক্লাস টাইপ
        TypeElement element = processingEnv.getElementUtils().getTypeElement(rawTypeName);
        if (element != null) {
            return element.asType();
        }

        throw new RuntimeException("Could not resolve TypeMirror for: " + typeName);
    }
}
