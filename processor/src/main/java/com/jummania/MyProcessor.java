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

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        // ১. এই রাউন্ডে যে ক্লাসগুলো পাওয়া গেছে, শুধু সেগুলো গ্লোবাল এবং লোকাল সেটে যোগ করুন
        for (Element element : roundEnv.getElementsAnnotatedWith(Serializable.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement typeElement = (TypeElement) element;
                annotatedClassNames.add(typeElement);
            }
        }

        // ২. শুধুমাত্র এই রাউন্ডে নতুন আসা ক্লাসগুলোর ফাইল জেনারেট করুন
        for (Element element : roundEnv.getElementsAnnotatedWith(Serializable.class)) {

            if (element.getKind() == ElementKind.CLASS) {

                // লুপের ভেতরে প্রতিবার নতুন বিল্ডার নিতে হবে যেন আগের কোড মিক্স না হয়
                SerializerBuilder serializerBuilder = new SerializerBuilder();

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

                serializerBuilder.write(processingEnv, (TypeElement) element, varName, 1);

                serializerBuilder.append("    }\n\n");
                serializerBuilder.append("}\n");

                try {
                    // সঠিক অরিজিনেটিং এলিমেন্ট পাস করা হয়েছে
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
