package com.jummania;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jummania.Utils.*;

class SerializerBuilder {
    StringBuilder builder = new StringBuilder();
    int i = 0;
    Set<String> names = new HashSet<>();
    int packSize;

    StringBuilder append(String string) {
        return builder.append(string);
    }

    void write(ProcessingEnvironment processingEnv, TypeElement element, String parentAccessor, int spaceCount) {
        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                String fieldName = field.getSimpleName().toString();
                TypeMirror typeMirror = field.asType();
                String fieldType = typeMirror.toString();

                if (!write(typeMirror, processingEnv, parentAccessor, fieldName, fieldType, spaceCount)) {
                    throw new RuntimeException("Unknown type: " + fieldType);
                }
            }
        }
    }

    private void writeIfNull(ProcessingEnvironment processingEnv, TypeElement element, boolean hasClass, String currentAccessor, int spaceCount) {
        if (hasClass) {
            String space = space(spaceCount);
            append(space).append("if (").append(currentAccessor).append(" == null)").append(" writer.writeInt(0);\n").append(space).append("else ").append("serialize(").append(currentAccessor).append(", writer);\n");
        } else write(processingEnv, element, currentAccessor, --spaceCount);
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
            componentTypeMirror = (typeArgs.length > 0) ? typeArgs[0] : null;
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

        if (!write(componentTypeMirror, processingEnv, null, itemVar, componentType, spaceCount)) {
            throw new RuntimeException("Unknown component type: " + componentType);
        }

        builder.append(doubleSpace).append("}\n");
        builder.append(space).append("}\n");
    }

    void writeMap(TypeMirror typeMirror, ProcessingEnvironment processingEnv, String fieldName, int spaceCount) {
        TypeMirror[] typeArgs = getTypeArguments(typeMirror);

        TypeMirror keyTypeMirror = (typeArgs.length > 0) ? typeArgs[0] : processingEnv.getElementUtils().getTypeElement("java.lang.Object").asType();
        TypeMirror valTypeMirror = (typeArgs.length > 1) ? typeArgs[1] : processingEnv.getElementUtils().getTypeElement("java.lang.Object").asType();

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

        builder.append(tripleSpace).append(normalKey).append(" ").append(keyVar).append(" = ").append(entryVar).append(".getKey();\n");
        builder.append(tripleSpace).append(normalVal).append(" ").append(valVar).append(" = ").append(entryVar).append(".getValue();\n");

        if (!write(keyTypeMirror, processingEnv, null, keyVar, keyType, spaceCount)) {
            throw new RuntimeException("Unknown map key type: " + keyType);
        }

        if (!write(valTypeMirror, processingEnv, null, valVar, valType, spaceCount)) {
            throw new RuntimeException("Unknown map value type: " + valType);
        }

        builder.append(doubleSpace).append("}\n");
        builder.append(space).append("}\n");
    }

    boolean write(TypeMirror typeMirror, ProcessingEnvironment processingEnv, String currentAccessor, String fieldName, String fieldType, int spaceCount) {

        if (currentAccessor == null) currentAccessor = fieldName;
        else {
            currentAccessor = currentAccessor + "." + fieldName;
        }

        String space = space(++spaceCount);

        if (writePrimitive(currentAccessor, fieldType, space)) {
            return true;
        }

        if (!names.contains(fieldName)) {
            fieldName = fieldName + i++;
            names.add(fieldName);
            if (builder.charAt(builder.length() - 2) != '{') append("\n");
            append(space).append(getNormalizedTypeName(typeMirror)).append(" ").append(fieldName).append(" = ").append(currentAccessor).append(";\n");
        }

        if (isArray(typeMirror, fieldType)) {
            javax.lang.model.type.ArrayType arrayType = (javax.lang.model.type.ArrayType) typeMirror;
            javax.lang.model.type.TypeMirror componentTypeMirror = arrayType.getComponentType();
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
            writeIfNull(processingEnv, element, hasClass(fieldType), fieldName, spaceCount);
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder importBuilder = new StringBuilder(types.size() * 9);
        types.stream().sorted().forEach(type -> importBuilder.append("import ").append(type).append(";\n"));

        importBuilder.append("\n");
        builder.insert(packSize, importBuilder);
        return builder.toString();
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
        return switch (typeMirror) {
            case null -> "";
            case DeclaredType declaredType -> {
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
                yield sb.toString();
            }
            case ArrayType arrayType -> getNormalizedTypeName(arrayType.getComponentType()) + "[]";
            default -> {
                String typeStr = typeMirror.toString();
                if (typeStr.startsWith("java.lang.")) {
                    yield typeStr.substring("java.lang.".length());
                }

                yield typeStr;
            }
        };
    }
}
