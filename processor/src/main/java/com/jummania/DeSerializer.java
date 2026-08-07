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

class DeSerializer {
    private final Set<String> types;
    private final Set<String> names = new HashSet<>();
    private final StringBuilder builder;
    private int i = 0;

    DeSerializer(StringBuilder stringBuilder, Set<String> types) {
        this.builder = stringBuilder;
        this.types = types;
    }

    void read(ProcessingEnvironment processingEnv, TypeElement element, String parentAccessor, int spaceCount) {
        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {

                if (enclosed.getModifiers().contains(Modifier.PRIVATE)) continue;
                VariableElement field = (VariableElement) enclosed;
                String fieldName = field.getSimpleName().toString();
                TypeMirror typeMirror = field.asType();
                String fieldType = typeMirror.toString();

                if (!writeAny(typeMirror, processingEnv, parentAccessor, fieldName, fieldType, spaceCount)) {
                    throw new RuntimeException("Unknown type for reading: " + fieldType);
                }
            }
        }
    }

    String readPrimitive(String fieldType) {
        return switch (fieldType) {
            case "int", "java.lang.Integer" -> "reader.readInt()";
            case "long", "java.lang.Long" -> "reader.readLong()";
            case "short", "java.lang.Short" -> "reader.readShort()";
            case "byte", "java.lang.Byte" -> "reader.readByte()";
            case "char", "java.lang.Character" -> "reader.readChar()";
            case "boolean", "java.lang.Boolean" -> "reader.readBoolean()";
            case "float", "java.lang.Float" -> "reader.readFloat()";
            case "double", "java.lang.Double" -> "reader.readDouble()";
            case "byte[]", "[B" -> "reader.readBytes()";
            case "java.lang.String" -> "reader.readString()";
            default -> null;
        };
    }

    boolean readPrimitive(String fieldName, String fieldType, String space) {
        switch (fieldType) {
            case "int", "java.lang.Integer" -> {
                String read = "reader.readInt();";
                builder.append(space).append("int ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "long", "java.lang.Long" -> {
                String read = "reader.readLong();";
                builder.append(space).append("long ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "short", "java.lang.Short" -> {
                String read = "reader.readShort();";
                builder.append(space).append("short ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "byte", "java.lang.Byte" -> {
                String read = "reader.readByte();";
                builder.append(space).append("byte ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "char", "java.lang.Character" -> {
                String read = "reader.readChar();";
                builder.append(space).append("char ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "boolean", "java.lang.Boolean" -> {
                String read = "reader.readBoolean();";
                builder.append(space).append("boolean ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "float", "java.lang.Float" -> {
                String read = "reader.readFloat();";
                builder.append(space).append("float ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "double", "java.lang.Double" -> {
                String read = "reader.readDouble();";
                builder.append(space).append("double ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "byte[]", "[B" -> {
                String read = "reader.readBytes();";
                builder.append(space).append("byte[] ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "java.lang.String" -> {
                String read = "reader.readString();";
                builder.append(space).append("String ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
        }

        return false;
    }

    void readArray(TypeMirror typeMirror, ProcessingEnvironment processingEnv, String fieldName, String explicitComponentType, int spaceCount) {
        String componentType;
        TypeMirror componentTypeMirror;

        boolean isArray = explicitComponentType != null;
        if (isArray) {
            componentType = explicitComponentType;
            componentTypeMirror = typeMirror;
        } else {
            TypeMirror[] typeArgs = getTypeArguments(typeMirror);
            componentTypeMirror = typeArgs[0];
            componentType = componentTypeMirror != null ? componentTypeMirror.toString() : "java.lang.Object";
        }

        String space = space(spaceCount++);
        String doubleSpace = space(spaceCount);
        types.add("java.util.ArrayList");
        if (isArray) {
            builder.append(componentType).append("[reader.readInt()];\n");
        } else builder.append(" ArrayList(reader.readInt());\n");

        builder.append(doubleSpace).append("for (int i = 0; i < ").append(fieldName).append(isArray ? ".length" : ".size()").append("; i++) {\n");
        String readPrimitive = readPrimitive(componentType);

        if (readPrimitive == null) {
            if (!writeAny(componentTypeMirror, processingEnv, null, fieldName, componentType, spaceCount + 1)) {
                throw new RuntimeException("Unknown component type for reading: " + componentType);
            }
        } else {
            builder.append(fieldName);
            if (isArray) builder.append("[i] = ");
            else builder.append(".add(");
            builder.append(readPrimitive);
        }

        builder.append(isArray ? ";\n" : ");\n");
        //   builder.append()

        // String itemVar = getFieldName(fieldName);
        //    builder.append(space(spaceCount + 1)).append(getNormalizedTypeName(componentTypeMirror)).append(" ").append(itemVar).append(";\n");


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

        writeCheck(fieldName, false, space, doubleSpace);

        types.add("java.util.Map.Entry");
        builder.append(doubleSpace).append("for (Entry<").append(normalKey).append(", ").append(normalVal).append("> ").append(entryVar).append(" : ").append(fieldName).append(".entrySet()) {\n");

        /*
        if (!readPrimitive(false, entryVar + ".getKey()", keyType, tripleSpace)) {
            builder.append(tripleSpace).append(normalKey).append(" ").append(keyVar).append(" = ").append(entryVar).append(".getKey();\n");
            if (!writeAny(keyTypeMirror, processingEnv, null, keyVar, keyType, spaceCount)) {
                throw new RuntimeException("Unknown map key type: " + keyType);
            }
        }

        if (!readPrimitive(false, entryVar + ".getValue()", valType, tripleSpace)) {
            builder.append(tripleSpace).append(normalVal).append(" ").append(valVar).append(" = ").append(entryVar).append(".getValue();\n");
            if (!writeAny(valTypeMirror, processingEnv, null, valVar, valType, spaceCount)) {
                throw new RuntimeException("Unknown map value type: " + valType);
            }
        }

         */

        builder.append(doubleSpace).append("}\n");
        builder.append(space).append("}\n");
    }

    boolean writeAny(TypeMirror typeMirror, ProcessingEnvironment processingEnv, String currentAccessor, String fieldName, String fieldType, int spaceCount) {

        if (currentAccessor == null) currentAccessor = fieldName;
        else currentAccessor = currentAccessor + "." + fieldName;

        String space = space(++spaceCount);

        if (readPrimitive(fieldName, fieldType, space)) return true;

        String className = null;

        if (!names.contains(fieldName)) {
            fieldName = fieldName + i++;
            names.add(fieldName);
            builder.append("\n");
            className = getNormalizedTypeName(typeMirror);
            builder.append(space).append(className).append(" ").append(fieldName).append(" = new ");
        }

        if (hasClass(fieldType)) {
            if (className == null) className = getNormalizedTypeName(typeMirror);
            builder.append(space).append(className).append("_.").append("deSerializer(").append("reader);\n");
            return true;
        }

        //  builder.append(space).append("if (").append(fieldName).append(" == null) ");

        if (isArray(typeMirror, fieldType)) {
            ArrayType arrayType = (ArrayType) typeMirror;
            TypeMirror componentTypeMirror = arrayType.getComponentType();
            String componentType = componentTypeMirror.toString();

            readArray(componentTypeMirror, processingEnv, fieldName, componentType, spaceCount);
            return true;
        }

        if (isMap(processingEnv, typeMirror, fieldType)) {
            writeMap(typeMirror, processingEnv, fieldName, spaceCount);
            return true;
        }

        if (isCollection(processingEnv, typeMirror, fieldType)) {
            readArray(typeMirror, processingEnv, fieldName, null, spaceCount);
            return true;
        }

        TypeElement element = getNestedTypeElement(processingEnv, fieldType);
        if (element != null) {
            writeCheck(null, false, space, space(spaceCount + 1));
            read(processingEnv, element, fieldName, spaceCount);
            builder.append(space).append("}\n");
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

    private void writeCheck(String fieldName, boolean isArray, String singleSpace, String doubleSpace) {
        boolean isObject = fieldName == null;
        String write = isObject ? "Byte((byte) " : "Int(";
        builder.append("writer.write").append(write).append("0);\n");
        builder.append(singleSpace).append("else {\n").append(doubleSpace).append("writer.write").append(write);
        if (!isObject) builder.append(fieldName);
        builder.append(isObject ? "1" : isArray ? ".length" : ".size()").append(");\n");
    }
}
