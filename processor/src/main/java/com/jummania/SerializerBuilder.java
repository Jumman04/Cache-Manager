package com.jummania;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jummania.Utils.*;

class SerializerBuilder {
    private final Set<String> types;
    private final Set<String> names = new HashSet<>();
    private final StringBuilder builder;
    private int i = 0;

    SerializerBuilder(StringBuilder stringBuilder, Set<String> types) {
        this.builder = stringBuilder;
        this.types = types;
    }

    void write(ProcessingEnvironment processingEnv, TypeElement element, String parentAccessor, int spaceCount) {
        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {

                if (enclosed.getModifiers().contains(Modifier.PRIVATE)) continue;
                VariableElement field = (VariableElement) enclosed;
                String fieldName = field.getSimpleName().toString();
                TypeMirror typeMirror = field.asType();
                String fieldType = typeMirror.toString();

                if (!writeAny(typeMirror, processingEnv, parentAccessor, fieldName, fieldType, spaceCount)) {
                    throw new RuntimeException("Unknown type: " + fieldType);
                }
            }
        }
    }

    private void writeIfNull(ProcessingEnvironment processingEnv, TypeElement element, boolean hasClass, String className, String fieldName, int spaceCount) {
        if (hasClass) {
            String space = space(spaceCount);
            builder.append(space).append("if (").append(fieldName).append(" == null)").append(" writer.writeInt(0);\n").append(space).append("else ").append(className).append("_.").append("serialize(").append(fieldName).append(", writer);\n");
        } else write(processingEnv, element, fieldName, --spaceCount);
    }

    boolean writePrimitive(String fieldName, String fieldType, String space) {
        switch (fieldType) {
            case "int", "java.lang.Integer" -> {
                builder.append(space).append("writer.writeInt(").append(fieldName);
                if (fieldType.equals("java.lang.Integer")) {
                    builder.append(" == null ? 0 : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "long", "java.lang.Long" -> {
                builder.append(space).append("writer.writeLong(").append(fieldName);
                if (fieldType.equals("java.lang.Long")) {
                    builder.append(" == null ? 0L : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "short", "java.lang.Short" -> {
                builder.append(space).append("writer.writeShort(").append(fieldName);
                if (fieldType.equals("java.lang.Short")) {
                    builder.append(" == null ? (short) 0 : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "byte", "java.lang.Byte" -> {
                builder.append(space).append("writer.writeByte(").append(fieldName);
                if (fieldType.equals("java.lang.Byte")) {
                    builder.append(" == null ? (byte) 0 : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "char", "java.lang.Character" -> {
                builder.append(space).append("writer.writeChar(").append(fieldName);
                if (fieldType.equals("java.lang.Character")) {
                    builder.append(" == null ? '\\0' : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "boolean", "java.lang.Boolean" -> {
                builder.append(space).append("writer.writeBoolean(").append(fieldName);
                if (fieldType.equals("java.lang.Boolean")) {
                    builder.append(" == null ? false : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "float", "java.lang.Float" -> {
                builder.append(space).append("writer.writeFloat(").append(fieldName);
                if (fieldType.equals("java.lang.Float")) {
                    builder.append(" == null ? 0f : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "double", "java.lang.Double" -> {
                builder.append(space).append("writer.writeDouble(").append(fieldName);
                if (fieldType.equals("java.lang.Double")) {
                    builder.append(" == null ? 0.0 : ").append(fieldName);
                }
                builder.append(");\n");
                return true;
            }
            case "java.lang.String" -> {
                builder.append(space).append("writer.writeString(").append(fieldName).append(");\n");
                return true;
            }
        }

        return false;
    }

    void writeArray(TypeMirror typeMirror, ProcessingEnvironment processingEnv, String fieldName, String getSize, String explicitComponentType, int spaceCount) {
        String componentType;
        TypeMirror componentTypeMirror;

        if (explicitComponentType != null) {
            componentType = explicitComponentType;
            componentTypeMirror = typeMirror;
        } else {
            TypeMirror[] typeArgs = getTypeArguments(typeMirror);
            componentTypeMirror = typeArgs[0];
            componentType = componentTypeMirror != null ? componentTypeMirror.toString() : "java.lang.Object";
        }

        String space = space(spaceCount++);
        String doubleSpace = space(spaceCount);
        builder.append(space).append("if (").append(fieldName).append(" == null) {\n");
        builder.append(doubleSpace).append("writer.writeInt(0);\n");
        builder.append(space).append("} else {\n");
        builder.append(doubleSpace).append("writer.writeInt(").append(fieldName).append(getSize).append(");\n");

        String itemVar = getFieldName(fieldName);
        builder.append(doubleSpace).append("for (").append(getNormalizedTypeName(componentTypeMirror)).append(" ").append(itemVar).append(" : ").append(fieldName).append(") {\n");

        if (!writeAny(componentTypeMirror, processingEnv, null, itemVar, componentType, spaceCount)) {
            throw new RuntimeException("Unknown component type: " + componentType);
        }

        builder.append(doubleSpace).append("}\n");
        builder.append(space).append("}\n");
    }

    void writeMap(TypeMirror typeMirror, ProcessingEnvironment processingEnv, String fieldName, int spaceCount) {
        TypeMirror[] typeArgs = getTypeArguments(typeMirror);

        TypeMirror keyTypeMirror = typeArgs[0];
        TypeMirror valTypeMirror = typeArgs[1];

        String keyType = keyTypeMirror.toString();
        String valType = valTypeMirror.toString();

        String normalKey = getNormalizedTypeName(keyTypeMirror);
        String normalVal = getNormalizedTypeName(valTypeMirror);

        String entryVar = "e_" + fieldName;
        String keyVar = "k_" + fieldName;
        String valVar = "v_" + fieldName;

        names.add(valVar);

        String space = space(spaceCount++);
        String doubleSpace = space(spaceCount);
        String tripleSpace = space(spaceCount + 1);

        builder.append(space).append("if (").append(fieldName).append(" == null) {\n");
        builder.append(doubleSpace).append("writer.writeInt(0);\n");
        builder.append(space).append("} else {\n");
        builder.append(doubleSpace).append("writer.writeInt(").append(fieldName).append(".size());\n");
        types.add("java.util.Map.Entry");
        builder.append(doubleSpace).append("for (Entry<").append(normalKey).append(", ").append(normalVal).append("> ").append(entryVar).append(" : ").append(fieldName).append(".entrySet()) {\n");

        if (!writePrimitive(entryVar + ".getKey()", keyType, tripleSpace)) {
            builder.append(tripleSpace).append(normalKey).append(" ").append(keyVar).append(" = ").append(entryVar).append(".getKey();\n");
            if (!writeAny(keyTypeMirror, processingEnv, null, keyVar, keyType, spaceCount)) {
                throw new RuntimeException("Unknown map key type: " + keyType);
            }
        }

        if (!writePrimitive(entryVar + ".getValue()", valType, tripleSpace)) {
            builder.append(tripleSpace).append(normalVal).append(" ").append(valVar).append(" = ").append(entryVar).append(".getValue();\n");
            if (!writeAny(valTypeMirror, processingEnv, null, valVar, valType, spaceCount)) {
                throw new RuntimeException("Unknown map value type: " + valType);
            }
        }

        builder.append(doubleSpace).append("}\n");
        builder.append(space).append("}\n");
    }

    boolean writeAny(TypeMirror typeMirror, ProcessingEnvironment processingEnv, String currentAccessor, String fieldName, String fieldType, int spaceCount) {

        if (currentAccessor == null) currentAccessor = fieldName;
        else currentAccessor = currentAccessor + "." + fieldName;

        String space = space(++spaceCount);

        if (writePrimitive(currentAccessor, fieldType, space)) return true;

        String className = null;

        if (!names.contains(fieldName)) {
            fieldName = fieldName + i++;
            names.add(fieldName);
            if (builder.charAt(builder.length() - 2) != '{') builder.append("\n");
            className = getNormalizedTypeName(typeMirror);
            builder.append(space).append(className).append(" ").append(fieldName).append(" = ").append(currentAccessor).append(";\n");
        }

        if (isArray(typeMirror, fieldType)) {
            ArrayType arrayType = (ArrayType) typeMirror;
            TypeMirror componentTypeMirror = arrayType.getComponentType();
            String componentType = componentTypeMirror.toString();

            writeArray(componentTypeMirror, processingEnv, fieldName, ".length", componentType, spaceCount);
            return true;
        }

        if (isMap(processingEnv, typeMirror, fieldType)) {
            writeMap(typeMirror, processingEnv, fieldName, spaceCount);
            return true;
        }

        if (isCollection(processingEnv, typeMirror, fieldType)) {
            writeArray(typeMirror, processingEnv, fieldName, ".size()", null, spaceCount);
            return true;
        }

        TypeElement element = getNestedTypeElement(processingEnv, fieldType);
        if (element != null) {
            if (className == null) className = getNormalizedTypeName(typeMirror);
            writeIfNull(processingEnv, element, hasClass(fieldType), className, fieldName, spaceCount);
            return true;
        }

        return false;
    }

    private String getFieldName(String fieldName) {
        if (names.contains(fieldName)) {
            fieldName = fieldName + i++;
            names.add(fieldName);
        }

        return fieldName;
    }

    String space(int count) {
        return " ".repeat(count * 4);
    }

    public String getNormalizedTypeName(TypeMirror typeMirror) {
        if (typeMirror == null) {
            return "";
        } else if (typeMirror instanceof DeclaredType declaredType) {
            TypeElement typeElement = (TypeElement) declaredType.asElement();

            String fullQualifiedName = typeElement.getQualifiedName().toString();

            if (!fullQualifiedName.startsWith("java.lang.")) {
                types.add(fullQualifiedName);
            }

            StringBuilder sb = new StringBuilder(typeElement.getSimpleName().toString());

            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (!typeArguments.isEmpty()) {
                sb.append("<");
                for (int i = 0; i < typeArguments.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(getNormalizedTypeName(typeArguments.get(i)));
                }
                sb.append(">");
            }
            return sb.toString();
        } else if (typeMirror instanceof ArrayType arrayType) {
            return getNormalizedTypeName(arrayType.getComponentType()) + "[]";
        } else {
            String typeStr = typeMirror.toString();
            if (typeStr.startsWith("java.lang.")) {
                return typeStr.substring("java.lang.".length());
            }

            return typeStr;
        }
    }
}
