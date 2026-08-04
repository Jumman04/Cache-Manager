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
import java.util.Collections;
import java.util.Set;

import static com.jummania.Utils.annotatedClassNames;

public class MyProcessor extends AbstractProcessor {

    SerializerBuilder serializerBuilder = new SerializerBuilder();
    private boolean isGenerated = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getElementsAnnotatedWith(Serializable.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                annotatedClassNames.add((TypeElement) element);
            }
        }

        if (roundEnv.processingOver() && !isGenerated && !annotatedClassNames.isEmpty()) {
            isGenerated = true;

            for (TypeElement element : annotatedClassNames) {

                String packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
                serializerBuilder.addImport("com.jummania.writer.Writer");
                serializerBuilder.addImport("java.io.IOException");
                serializerBuilder.packSize = serializerBuilder.append("package ").append(packageName).append(";\n\n").length();
                String className = element.getSimpleName().toString();
                String targetClassName = className + "_";
                serializerBuilder.append("\npublic final class ").append(targetClassName).append(" {\n\n");

                serializerBuilder.addImport(element.asType().toString());
                String varName = className.substring(0, 1).toLowerCase() + className.substring(1);

                serializerBuilder.append("    public static void serialize(").append(className).append(" ").append(varName).append(", Writer writer) throws IOException {\n");

                serializerBuilder.write(processingEnv, element, varName, 1);

                serializerBuilder.append("    }\n\n");

                serializerBuilder.append("}\n");

                try {
                    JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(packageName + "." + targetClassName, element);
                    try (Writer writer = builderFile.openWriter()) {
                        writer.write(serializerBuilder.toString());
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
}
