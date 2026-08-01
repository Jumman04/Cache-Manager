package com.jummania;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static com.jummania.Utils.annotatedClassNames;
import static com.jummania.Utils.packSize;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.jummania.Serializable")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class MyProcessor extends AbstractProcessor {

    String targetPackage = getClass().getPackageName();
    SerializerBuilder serializerBuilder = new SerializerBuilder();
    private boolean isGenerated = false; // একবার জেনারেট হয়ে গেলে যেন ডাবল কল না হয়

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getElementsAnnotatedWith(Serializable.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                annotatedClassNames.add((TypeElement) element);
            }
        }

        if (roundEnv.processingOver() && !isGenerated && !annotatedClassNames.isEmpty()) {
            isGenerated = true;

            String targetClassName = "GeneratedSerializers";
            packSize = serializerBuilder.append("package ").append(targetPackage).append(";\n\n").append("import com.jummania.writer.Writer;\n").length();
            serializerBuilder.append("\n\npublic final class ").append(targetClassName).append(" {\n\n");

            for (TypeElement element : annotatedClassNames) {
                String fullClass = element.asType().toString();
                String className = element.getSimpleName().toString();
                String varName = className.substring(0, 1).toLowerCase() + className.substring(1);

                serializerBuilder.append("    public static void serialize(").append(fullClass).append(" ").append(varName).append(", Writer writer) throws java.io.IOException {\n");

                serializerBuilder.write(processingEnv, element, varName);

                serializerBuilder.append("    }\n\n");
            }

            serializerBuilder.append("}\n");

            try {
                JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(targetPackage + "." + targetClassName);
                try (Writer writer = builderFile.openWriter()) {
                    writer.write(serializerBuilder.toString());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

}
