package com.jummania;

import com.google.auto.service.AutoService;

import javax.annotation.Nonnull;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.jummania.MyCustomAnnotation")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class MyProcessor extends AbstractProcessor {

    private final Set<TypeElement> annotatedClassNames = new HashSet<>();
    private boolean isGenerated = false; // একবার জেনারেট হয়ে গেলে যেন ডাবল কল না হয়

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        // ১. প্রতিটি রাউন্ড থেকে অ্যানোটেশনযুক্ত ক্লাসগুলো সেটে জমা করা
        for (Element element : roundEnv.getElementsAnnotatedWith(MyCustomAnnotation.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                annotatedClassNames.add((TypeElement) element);
            }
        }

        // ২. শুধুমাত্র যখন কম্পাইলেশন রাউন্ড শেষ হবে এবং আগে জেনারেট না হয়ে থাকে
        if (roundEnv.processingOver() && !isGenerated && !annotatedClassNames.isEmpty()) {
            isGenerated = true;

            String targetPackage = getClass().getPackageName();
            String targetClassName = "GeneratedSerializers";

            StringBuilder serializerCode = new StringBuilder();
            serializerCode.append("package ").append(targetPackage).append(";\n\n");
            serializerCode.append("public final class ").append(targetClassName).append(" {\n\n");

            for (TypeElement element : annotatedClassNames) {
                String fullClass = element.asType().toString();
                String className = element.getSimpleName().toString();

                String methodName = "serialize";
                String varName = className.substring(0, 1).toLowerCase() + className.substring(1);

                serializerCode.append("    public static void ").append(methodName).append("(").append(fullClass).append(" ").append(varName).append(", com.jummania.writer.Writer writer) throws java.io.IOException {\n");

                write(element, serializerCode, varName);

                serializerCode.append("    }\n\n");
            }

            serializerCode.append("}\n");

            try {
                JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(targetPackage + "." + targetClassName);
                try (Writer writer = builderFile.openWriter()) {
                    writer.write(serializerCode.toString());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    private void write(TypeElement element, StringBuilder code, @Nonnull String parentAccessor) {
        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                String fieldName = field.getSimpleName().toString();
                String fieldType = field.asType().toString();

                String currentAccessor = parentAccessor + "." + fieldName;

                if (writePrimitive(currentAccessor, fieldType, code)) {
                    continue;
                }


                TypeElement subClassElement = processingEnv.getElementUtils().getTypeElement(fieldType);

                if (subClassElement != null) {
                    writeIfNull(code, subClassElement, currentAccessor, fieldType, "        ");
                } else {
                    if (fieldType.endsWith("[]") || field.asType().getKind().name().equals("ARRAY")) {
                        String componentType = getArrayComponentType(field);
                        writeArray(code, componentType, currentAccessor, fieldName, ".length");
                    } else if (isIterable(field)) {
                        String componentType = getIterableComponentType(field);
                        writeArray(code, componentType, currentAccessor, fieldName, ".size()");
                    } else if (isMap(field)) {
                        writeMap(code, field, currentAccessor, fieldName);
                    } else throw new RuntimeException("Unknown type: " + fieldType);
                }
            }
        }
    }

    private void writeIfNull(StringBuilder code, TypeElement element, String currentAccessor, String fieldType, String space) {
        if (hasClass(fieldType)) {
            code.append(space).append("if (").append(currentAccessor).append(" == null)").append(" writer.writeInt(0);\n").append(space).append("else ").append("serialize(").append(currentAccessor).append(", writer);\n");
        } else write(element, code, currentAccessor);
    }

    private boolean writePrimitive(String fieldName, String fieldType, StringBuilder code) {

        switch (fieldType) {
            case "int", "java.lang.Integer" -> {
                code.append("        writer.writeInt(").append(fieldName);
                if (fieldType.equals("java.lang.Integer")) {
                    code.append(" == null ? 0 : ").append(fieldName);
                }
                code.append(");\n");
                return true;
            }
            case "long", "java.lang.Long" -> {
                code.append("        writer.writeLong(").append(fieldName);
                if (fieldType.equals("java.lang.Long")) {
                    code.append(" == null ? 0L : ").append(fieldName);
                }
                code.append(");\n");
                return true;
            }
            case "short", "java.lang.Short" -> {
                code.append("        writer.writeShort(").append(fieldName);
                if (fieldType.equals("java.lang.Short")) {
                    code.append(" == null ? (short) 0 : ").append(fieldName);
                }
                code.append(");\n");
                return true;
            }
            case "byte", "java.lang.Byte" -> {
                code.append("        writer.writeByte(").append(fieldName);
                if (fieldType.equals("java.lang.Byte")) {
                    code.append(" == null ? (byte) 0 : ").append(fieldName);
                }
                code.append(");\n");
                return true;
            }
            case "char", "java.lang.Character" -> {
                code.append("        writer.writeChar(").append(fieldName);
                if (fieldType.equals("java.lang.Character")) {
                    code.append(" == null ? '\\0' : ").append(fieldName);
                }
                code.append(");\n");
                return true;
            }
            case "boolean", "java.lang.Boolean" -> {
                code.append("        writer.writeBoolean(").append(fieldName);
                if (fieldType.equals("java.lang.Boolean")) {
                    code.append(" == null ? false : ").append(fieldName);
                }
                code.append(");\n");
                return true;
            }
            case "float", "java.lang.Float" -> {
                code.append("        writer.writeFloat(").append(fieldName);
                if (fieldType.equals("java.lang.Float")) {
                    code.append(" == null ? 0f : ").append(fieldName);
                }
                code.append(");\n");
                return true;
            }
            case "double", "java.lang.Double" -> {
                code.append("        writer.writeDouble(").append(fieldName);
                if (fieldType.equals("java.lang.Double")) {
                    code.append(" == null ? 0.0 : ").append(fieldName);
                }
                code.append(");\n");
                return true;
            }
            case "java.lang.String" -> {
                code.append("        writer.writeString(").append(fieldName).append(");\n");
                return true;
            }
        }

        return false;
    }

    boolean hasClass(String fieldType) {
        for (TypeElement annotatedElement : annotatedClassNames) {
            if (annotatedElement.getQualifiedName().toString().equals(fieldType)) {
                return true;
            }
        }
        return false;
    }

    private String getArrayComponentType(VariableElement field) {
        TypeMirror typeMirror = field.asType();
        if (typeMirror instanceof ArrayType arrayType) {
            return arrayType.getComponentType().toString();
        }
        return "java.lang.Object"; // ফলব্যাক টাইপ
    }

    private String getIterableComponentType(VariableElement field) {
        javax.lang.model.type.TypeMirror typeMirror = field.asType();

        if (typeMirror instanceof javax.lang.model.type.DeclaredType declaredType) {
            java.util.List<? extends javax.lang.model.type.TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (!typeArguments.isEmpty()) {
                return typeArguments.get(0).toString();
            }
        }
        return "java.lang.Object"; // ফলব্যাক
    }

    private boolean isIterable(VariableElement field) {
        javax.lang.model.type.TypeMirror typeMirror = field.asType();

        javax.lang.model.util.Types typeUtils = processingEnv.getTypeUtils();
        javax.lang.model.util.Elements elementUtils = processingEnv.getElementUtils();

        javax.lang.model.type.TypeMirror iterableType = elementUtils.getTypeElement("java.lang.Iterable").asType();

        return typeUtils.isAssignable(typeUtils.erasure(typeMirror), typeUtils.erasure(iterableType));
    }

    private boolean isMap(VariableElement field) {
        javax.lang.model.type.TypeMirror typeMirror = field.asType();
        javax.lang.model.util.Types typeUtils = processingEnv.getTypeUtils();
        javax.lang.model.util.Elements elementUtils = processingEnv.getElementUtils();

        javax.lang.model.type.TypeMirror mapType = elementUtils.getTypeElement("java.util.Map").asType();

        return typeUtils.isAssignable(typeUtils.erasure(typeMirror), typeUtils.erasure(mapType));
    }

    private String[] getMapKeyAndValueTypes(VariableElement field) {
        javax.lang.model.type.TypeMirror typeMirror = field.asType();
        if (typeMirror instanceof javax.lang.model.type.DeclaredType declaredType) {
            java.util.List<? extends javax.lang.model.type.TypeMirror> typeArguments = declaredType.getTypeArguments();

            if (typeArguments.size() >= 2) {
                return new String[]{typeArguments.get(0).toString(), typeArguments.get(1).toString()};
            }
        }
        return new String[]{"java.lang.Object", "java.lang.Object"};
    }

    private void writeArray(StringBuilder code, String componentType, String currentAccessor, String fieldName, String getSize) {

        code.append("        if (").append(currentAccessor).append(" == null) {\n");
        code.append("            writer.writeInt(0);\n"); // -1 মানে অ্যারেটি null ছিল
        code.append("        } else {\n");

        code.append("            writer.writeInt(").append(currentAccessor).append(getSize).append(");\n");

        code.append("            for (").append(componentType).append(" ").append(fieldName).append(" : ").append(currentAccessor).append(") {\n");

        if (!writePrimitive(fieldName, componentType, code)) {
            TypeElement arrayElement = processingEnv.getElementUtils().getTypeElement(componentType);
            if (arrayElement != null) {
                writeIfNull(code, arrayElement, fieldName, componentType, "                ");
            } else {
                throw new RuntimeException("Unknown array component type: " + componentType);
            }
        }

        code.append("            }\n");
        code.append("        }\n");
    }

    void writeMap(StringBuilder code, VariableElement field, String currentAccessor, String fieldName) {
        String[] kvTypes = getMapKeyAndValueTypes(field);
        String keyType = kvTypes[0];
        String valType = kvTypes[1];

        String entryVar = "entry_" + fieldName;
        String keyVar = "key_" + fieldName;
        String valVar = "val_" + fieldName;

        code.append("        if (").append(currentAccessor).append(" == null) {\n");
        code.append("            writer.writeInt(0);\n");
        code.append("        } else {\n");
        code.append("            writer.writeInt(").append(currentAccessor).append(".size());\n"); // Map-এর সাইজ
        code.append("            for (java.util.Map.Entry<").append(keyType).append(", ").append(valType).append("> ").append(entryVar).append(" : ").append(currentAccessor).append(".entrySet()) {\n");

        code.append("                ").append(keyType).append(" ").append(keyVar).append(" = ").append(entryVar).append(".getKey();\n");
        code.append("                ").append(valType).append(" ").append(valVar).append(" = ").append(entryVar).append(".getValue();\n");

        if (!writePrimitive(keyVar, keyType, code)) {
            throw new RuntimeException("Map Key must be primitive or String: " + keyType);
        }

        if (!writePrimitive(valVar, valType, code)) {
            TypeElement valElement = processingEnv.getElementUtils().getTypeElement(valType);
            if (valElement != null) {
                writeIfNull(code, valElement, valVar, valType, "                ");
            } else {
                throw new RuntimeException("Unknown Map Value type: " + valType);
            }
        }

        code.append("            }\n");
        code.append("        }\n");

    }

}
