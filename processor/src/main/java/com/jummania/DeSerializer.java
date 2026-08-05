package com.jummania;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeSerializer {
    private final Set<String> types;
    private final Set<String> names = new HashSet<>();
    private final StringBuilder builder;
    private final int i = 0;

    DeSerializer(StringBuilder stringBuilder, Set<String> types) {
        this.builder = stringBuilder;
        this.types = types;
    }

    void write(ProcessingEnvironment processingEnv, TypeElement element, String parentAccessor, int spaceCount) {

        printConstructorsWithParamNames(element);
        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {

                if (enclosed.getModifiers().contains(Modifier.PRIVATE)) continue;
                VariableElement field = (VariableElement) enclosed;
                String fieldName = field.getSimpleName().toString();
                TypeMirror typeMirror = field.asType();
                String fieldType = typeMirror.toString();

                List<ExecutableElement> constructors = ElementFilter.constructorsIn(element.getEnclosedElements());

                //  System.out.println(constructors);
                /*
                if (!writeAny(typeMirror, processingEnv, parentAccessor, fieldName, fieldType, spaceCount)) {
                    throw new RuntimeException("Unknown type: " + fieldType);
                }

                 */
            }
        }
    }

    public void printConstructorsWithParamNames(TypeElement typeElement) {
        List<ExecutableElement> constructors = ElementFilter.constructorsIn(typeElement.getEnclosedElements());

        for (ExecutableElement constructor : constructors) {
            StringBuilder sb = new StringBuilder();
            sb.append(typeElement.getSimpleName()).append("(");

            List<? extends VariableElement> parameters = constructor.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                VariableElement param = parameters.get(i);
                // param.asType() হলো টাইপ (যেমন: int, String)
                // param.getSimpleName() হলো প্যারামিটারের নাম (যেমন: id, name)
                sb.append(param.asType()).append(" ").append(param.getSimpleName());

                if (i < parameters.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");

            System.out.println(sb);
            // আউটপুট দেখতে পাবেন এমন: Department(int id, java.lang.String name, boolean active)
        }
    }
}
