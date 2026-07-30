package com.jummania;

import javax.annotation.Nonnull;
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

    void write(ProcessingEnvironment processingEnv, TypeElement element, @Nonnull String parentAccessor) {
        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                String fieldName = field.getSimpleName().toString();
                TypeMirror typeMirror = field.asType();
                String fieldType = typeMirror.toString();

                String currentAccessor = parentAccessor + "." + fieldName;


                if (!write(processingEnv, currentAccessor, fieldType)) {
                    if (isArray(fieldType, typeMirror)) {
                        String componentType = getArrayComponentType(typeMirror);
                        writeArray(processingEnv, componentType, currentAccessor, fieldName, ".length");
                    } else if (isIterable(processingEnv, typeMirror, fieldType)) {
                        String componentType = getIterableComponentType(typeMirror);
                        writeArray(processingEnv, componentType, currentAccessor, fieldName, ".size()");
                    } else if (isMap(processingEnv, typeMirror, fieldType)) {
                        writeMap(typeMirror, processingEnv, currentAccessor, fieldName);
                    } else throw new RuntimeException("Unknown type: " + fieldType);
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

    void writeArray(ProcessingEnvironment processingEnv, String componentType, String currentAccessor, String fieldName, String getSize) {

        builder.append("        if (").append(currentAccessor).append(" == null) {\n");
        builder.append("            writer.writeInt(0);\n"); // -1 মানে অ্যারেটি null ছিল
        builder.append("        } else {\n");

        builder.append("            writer.writeInt(").append(currentAccessor).append(getSize).append(");\n");

        builder.append("            for (").append(componentType).append(" ").append(fieldName).append(" : ").append(currentAccessor).append(") {\n");

        //   System.out.println(componentType);
        if (!write(processingEnv, fieldName, componentType)) {
            throw new RuntimeException("Unknown component type: " + componentType);
        }

        builder.append("            }\n");
        builder.append("        }\n");
    }

    void writeMap(TypeMirror typeMirror, ProcessingEnvironment processingEnv, String currentAccessor, String fieldName) {
        String[] kvTypes = getMapKeyAndValueTypes(typeMirror);
        String keyType = kvTypes[0];
        String valType = kvTypes[1];

        String entryVar = "entry_" + fieldName;
        String keyVar = "key";
        String valVar = "value";

        builder.append("        if (").append(currentAccessor).append(" == null) {\n");
        builder.append("            writer.writeInt(0);\n");
        builder.append("        } else {\n");
        builder.append("            writer.writeInt(").append(currentAccessor).append(".size());\n"); // Map-এর সাইজ
        builder.append("            for (java.util.Map.Entry<").append(keyType).append(", ").append(valType).append("> ").append(entryVar).append(" : ").append(currentAccessor).append(".entrySet()) {\n");

        builder.append("                ").append(keyType).append(" ").append(keyVar).append(" = ").append(entryVar).append(".getKey();\n");
        builder.append("                ").append(valType).append(" ").append(valVar).append(" = ").append(entryVar).append(".getValue();\n");

        typeMirror = getInnerTypeMirror(processingEnv, keyType);
        // System.out.println(getInnerTypeMirror(processingEnv, keyType));

        if (!write(processingEnv, keyVar, keyType)) {
            if (isArray(keyType, typeMirror)) {
                String componentType = getArrayComponentType(typeMirror);
                writeArray(processingEnv, componentType, currentAccessor, fieldName, ".length");
            } else if (isIterable(processingEnv, typeMirror, keyType)) {
                String componentType = getIterableComponentType(typeMirror);
                System.out.println(fieldName);
                writeArray(processingEnv, componentType, currentAccessor, fieldName, ".size()");
            } else if (isMap(processingEnv, typeMirror, keyType)) {
                writeMap(typeMirror, processingEnv, currentAccessor, fieldName);
            } else throw new RuntimeException("Unknown type: " + keyType);
        }

        if (!write(processingEnv, valVar, valType)) {
            if (isArray(valType, typeMirror)) {
                String componentType = getArrayComponentType(typeMirror);
                writeArray(processingEnv, componentType, currentAccessor, fieldName, ".length");
            } else if (isIterable(processingEnv, typeMirror, valType)) {
                String componentType = getIterableComponentType(typeMirror);
                writeArray(processingEnv, componentType, currentAccessor, fieldName, ".size()");
            } else if (isMap(processingEnv, typeMirror, valType)) {
                writeMap(typeMirror, processingEnv, currentAccessor, fieldName);
            } else throw new RuntimeException("Unknown type: " + valType);
        }


        builder.append("            }\n");
        builder.append("        }\n");

    }

    boolean write(ProcessingEnvironment processingEnv, String fieldName, String fieldType) {
        if (!writePrimitive(fieldName, fieldType)) {
            TypeElement element = processingEnv.getElementUtils().getTypeElement(fieldType);
            if (element != null) {
                writeIfNull(processingEnv, element, hasClass(fieldType), fieldName, "                ");
            } else return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
