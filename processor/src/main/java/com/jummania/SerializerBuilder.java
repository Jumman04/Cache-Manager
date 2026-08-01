package com.jummania;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import static com.jummania.Utils.*;

class SerializerBuilder {
    StringBuilder builder = new StringBuilder();
    String targetPackage;

    SerializerBuilder(String targetPackage) {
        this.targetPackage = targetPackage;
    }

    StringBuilder append(String string) {
        return builder.append(string);
    }

    void write(ProcessingEnvironment processingEnv, TypeElement element, String parentAccessor) {
        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                String fieldName = field.getSimpleName().toString();
                TypeMirror typeMirror = field.asType();
                String fieldType = typeMirror.toString();

                if (!write(typeMirror, processingEnv, parentAccessor, fieldName, fieldType)) {
                    throw new RuntimeException("Unknown type: " + fieldType);
                }
            }
        }
    }

    private void writeIfNull(ProcessingEnvironment processingEnv, TypeElement element, boolean hasClass, String currentAccessor, String space) {
        if (hasClass) {
            append(space).append("if (").append(currentAccessor).append(" == null)").append(" writer.writeInt(0);\n").append(space).append("else ").append("serialize(").append(currentAccessor).append(", writer);\n");
        } else write(processingEnv, element, currentAccessor);
    }

    boolean writePrimitive(String fieldName, String fieldType) {

        switch (fieldType) {
            case "int", "java.lang.Integer" -> {
                builder.append("        writer.writeInt(").append(fieldName);
                if (fieldType.equals("java.lang.Integer")) {
                    builder.append(" == null ? 0 : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "long", "java.lang.Long" -> {
                builder.append("        writer.writeLong(").append(fieldName);
                if (fieldType.equals("java.lang.Long")) {
                    builder.append(" == null ? 0L : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "short", "java.lang.Short" -> {
                builder.append("        writer.writeShort(").append(fieldName);
                if (fieldType.equals("java.lang.Short")) {
                    builder.append(" == null ? (short) 0 : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "byte", "java.lang.Byte" -> {
                builder.append("        writer.writeByte(").append(fieldName);
                if (fieldType.equals("java.lang.Byte")) {
                    builder.append(" == null ? (byte) 0 : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "char", "java.lang.Character" -> {
                builder.append("        writer.writeChar(").append(fieldName);
                if (fieldType.equals("java.lang.Character")) {
                    builder.append(" == null ? '\\0' : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "boolean", "java.lang.Boolean" -> {
                builder.append("        writer.writeBoolean(").append(fieldName);
                if (fieldType.equals("java.lang.Boolean")) {
                    builder.append(" == null ? false : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "float", "java.lang.Float" -> {
                builder.append("        writer.writeFloat(").append(fieldName);
                if (fieldType.equals("java.lang.Float")) {
                    builder.append(" == null ? 0f : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "double", "java.lang.Double" -> {
                builder.append("        writer.writeDouble(").append(fieldName);
                if (fieldType.equals("java.lang.Double")) {
                    builder.append(" == null ? 0.0 : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "java.lang.String" -> {
                builder.append("        writer.writeString(").append(fieldName).append(");\n");
                return true;
            }
        }

        return false;
    }

    void writeArray(TypeMirror typeMirror, ProcessingEnvironment processingEnv, String currentAccessor, String fieldName, String getSize, String explicitComponentType) {
        String componentType;
        TypeMirror componentTypeMirror;

        if (explicitComponentType != null) {
            componentType = explicitComponentType;
            componentTypeMirror = typeMirror;
        } else {
            TypeMirror[] typeArgs = getTypeArguments(typeMirror);
            componentTypeMirror = (typeArgs.length > 0) ? typeArgs[0] : null;
            componentType = componentTypeMirror != null ? componentTypeMirror.toString() : "java.lang.Object";
        }

        builder.append("        if (").append(currentAccessor).append(" == null) {\n");
        builder.append("            writer.writeInt(0);\n");
        builder.append("        } else {\n");
        builder.append("            writer.writeInt(").append(currentAccessor).append(getSize).append(");\n");

        String itemVar = "_" + fieldName;
        builder.append("            for (").append(componentType).append(" ").append(itemVar).append(" : ").append(currentAccessor).append(") {\n");

        if (!write(componentTypeMirror, processingEnv, null, itemVar, componentType)) {
            throw new RuntimeException("Unknown component type: " + componentType);
        }

        builder.append("            }\n");
        builder.append("        }\n");
    }

    void writeMap(TypeMirror typeMirror, ProcessingEnvironment processingEnv, String currentAccessor, String fieldName) {
        TypeMirror[] typeArgs = getTypeArguments(typeMirror);

        TypeMirror keyTypeMirror = (typeArgs.length > 0) ? typeArgs[0] : processingEnv.getElementUtils().getTypeElement("java.lang.Object").asType();
        TypeMirror valTypeMirror = (typeArgs.length > 1) ? typeArgs[1] : processingEnv.getElementUtils().getTypeElement("java.lang.Object").asType();

        String keyType = keyTypeMirror.toString();
        String valType = valTypeMirror.toString();

        String entryVar = "e_" + fieldName;
        String keyVar = "k_" + fieldName;
        String valVar = "v_" + fieldName;

        builder.append("        if (").append(currentAccessor).append(" == null) {\n");
        builder.append("            writer.writeInt(0);\n");
        builder.append("        } else {\n");
        builder.append("            writer.writeInt(").append(currentAccessor).append(".size());\n");
        builder.append("            for (java.util.Map.Entry<").append(keyType).append(", ").append(valType).append("> ").append(entryVar).append(" : ").append(currentAccessor).append(".entrySet()) {\n");

        builder.append("                ").append(keyType).append(" ").append(keyVar).append(" = ").append(entryVar).append(".getKey();\n");
        builder.append("                ").append(valType).append(" ").append(valVar).append(" = ").append(entryVar).append(".getValue();\n");

        if (!write(keyTypeMirror, processingEnv, null, keyVar, keyType)) {
            throw new RuntimeException("Unknown map key type: " + keyType);
        }

        if (!write(valTypeMirror, processingEnv, null, valVar, valType)) {
            throw new RuntimeException("Unknown map value type: " + valType);
        }

        builder.append("            }\n");
        builder.append("        }\n");
    }

    boolean write(TypeMirror typeMirror, ProcessingEnvironment processingEnv, String currentAccessor, String fieldName, String fieldType) {

        if (currentAccessor == null) currentAccessor = fieldName;
        else currentAccessor = currentAccessor + "." + fieldName;

        if (writePrimitive(currentAccessor, fieldType)) {
            return true;
        }

        if (isArray(typeMirror, fieldType)) {
            javax.lang.model.type.ArrayType arrayType = (javax.lang.model.type.ArrayType) typeMirror;
            javax.lang.model.type.TypeMirror componentTypeMirror = arrayType.getComponentType();
            String componentType = componentTypeMirror.toString();

            writeArray(componentTypeMirror, processingEnv, currentAccessor, fieldName, ".length", componentType);
            return true;
        }

        if (isMap(processingEnv, typeMirror, fieldType)) {
            writeMap(typeMirror, processingEnv, currentAccessor, fieldName);
            return true;
        }

        if (isCollection(processingEnv, typeMirror, fieldType)) {
            writeArray(typeMirror, processingEnv, currentAccessor, fieldName, ".size()", null);
            return true;
        }

        TypeElement element = getNestedTypeElement(processingEnv, fieldType);
        if (element != null) {
            writeIfNull(processingEnv, element, hasClass(fieldType), currentAccessor, "                ");
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
