package com.jummania;

import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static com.jummania.Utils.annotatedClassNames;

@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.ISOLATING)
@SupportedAnnotationTypes("com.jummania.Serializable")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
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
                    JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(packageName + "." + targetClassName);
                    try (Writer writer = builderFile.openWriter()) {
                        writer.write(serializerBuilder.toString());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }


        /*
        if (roundEnv.processingOver() && !isGenerated && !annotatedClassNames.isEmpty()) {

            isGenerated = true; // ফ্ল্যাগ অন করে দিলাম যাতে পরের রাউন্ডে আর না ঢোকে



            for (TypeElement element : annotatedClassNames) {

                String packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();

                String className = element.getSimpleName().toString();

                String generatedClassName = className + "_Serializer";



                SerializerBuilder serializerBuilder = new SerializerBuilder();

                serializerBuilder.append("package ").append(packageName).append(";\n\n");

                serializerBuilder.append("public final class ").append(generatedClassName).append(" {\n");

                String fullClass = element.asType().toString();

                String varName = className.substring(0, 1).toLowerCase() + className.substring(1);

                serializerBuilder.append("    public static void serialize(").append(fullClass).append(" ").append(varName).append(", com.jummania.writer.Writer writer) throws java.io.IOException {\n");



                // ফিল্ডগুলো লুপ করে স্ট্রিং বিল্ড করা

                serializerBuilder.write(element, code, varName);



                code.append("    }\n");

                code.append("}\n");



                // Filer দিয়ে ফিজিক্যাল ফাইল রাইট করা

                try {

                    JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(packageName + "." + generatedClassName);

                    try (Writer writer = builderFile.openWriter()) {

                        writer.write(code.toString());

                    }

                } catch (IOException e) {

                    throw new RuntimeException(e);

                }

            }

        }

         */



        return true;
    }

}
