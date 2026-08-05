package com.jummania;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static com.jummania.Utils.annotatedClassNames;

public class MyProcessor extends AbstractProcessor {

    StringBuilder stringBuilder = new StringBuilder();
    private Set<String> types = new HashSet<>();
    SerializerBuilder serializerBuilder = new SerializerBuilder(stringBuilder, types);
    DeSerializer deSerializer = new DeSerializer(stringBuilder, types);

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Serializable.class);
        for (Element element : elements) {
            if (element.getKind() == ElementKind.CLASS) {
                annotatedClassNames.add((TypeElement) element);
            }
        }

        int packSize;

        List<String> imports = new ArrayList<>(3);

        imports.add("com.jummania.writer.Writer");
        imports.add("com.jummania.reader.Reader");
        imports.add("java.io.IOException");

        for (Element element : elements) {

            if (element.getKind() == ElementKind.CLASS) {

                String packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
                types.addAll(imports);
                packSize = stringBuilder.append("package ").append(packageName).append(";\n\n").length();
                String className = element.getSimpleName().toString();
                String targetClassName = className + "_";
                stringBuilder.append("\npublic final class ").append(targetClassName).append(" {\n\n");

                types.add(element.asType().toString());
                String varName = className.substring(0, 1).toLowerCase() + className.substring(1);

                stringBuilder.append("    public static void serialize(").append(className).append(" ").append(varName).append(", Writer writer) throws IOException {\n");

                TypeElement typeElement = (TypeElement) element;
                serializerBuilder.write(processingEnv, typeElement, varName, 1);

                stringBuilder.append("    }\n\n");

                stringBuilder.append("   public static ").append("void").append(" deSerializer(").append("Reader reader) throws IOException {\n");
                deSerializer.write(processingEnv, typeElement, varName, 1);

                stringBuilder.append("    }\n\n");
                stringBuilder.append("}\n");

                try {
                    JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(packageName + "." + targetClassName, element);
                    try (Writer writer = builderFile.openWriter()) {
                        writer.write(toString(packSize));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("com.jummania.Serializable");
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.singleton("org.gradle.annotation.processing.isolating");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_17;
    }


    private String toString(int packSize) {
        StringBuilder importBuilder = new StringBuilder(types.size() * 9);
        types.stream().sorted().forEach(type -> importBuilder.append("import ").append(type).append(";\n"));

        importBuilder.append("\n");
        stringBuilder.insert(packSize, importBuilder);
        String result = stringBuilder.toString();

        stringBuilder.setLength(0);
        types = new HashSet<>(types.size());
        return result;
    }
}
