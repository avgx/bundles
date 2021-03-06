package com.neenbedankt.bundles.processor;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import com.neenbedankt.bundles.annotation.Frozen;
import com.squareup.java.JavaWriter;

@SupportedAnnotationTypes("com.neenbedankt.bundles.annotation.Frozen")
public class FrozenFieldsProcessor extends BaseProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Set<? extends Element> elements = env.getElementsAnnotatedWith(Frozen.class);
        Map<TypeElement, Set<AnnotatedField>> fieldsByType = new HashMap<TypeElement, Set<AnnotatedField>>(100);
        for (Element element : elements) {
            if (element.getModifiers().contains(Modifier.FINAL) ||
                element.getModifiers().contains(Modifier.STATIC) ||
                element.getModifiers().contains(Modifier.PROTECTED) ||
                element.getModifiers().contains(Modifier.PRIVATE)) {
                error(element, "Field must not be private, protected, static or final");
                continue;
            }
            Set<AnnotatedField> fields = fieldsByType.get(element.getEnclosingElement());
            if (fields == null) {
                fields = new LinkedHashSet<AnnotatedField>(10);
                fieldsByType.put((TypeElement)element.getEnclosingElement(), fields);
            }
            fields.add(new AnnotatedField(element));
        }
        for (Entry<TypeElement, Set<AnnotatedField>> entry : fieldsByType.entrySet()) {
            JavaFileObject jfo;
            try {
                jfo = processingEnv.getFiler().createSourceFile(entry.getKey().getQualifiedName() + "State", entry.getKey());
                Writer writer = jfo.openWriter();
                JavaWriter jw = new JavaWriter(writer);

                writePackage(jw, entry.getKey());

                jw.beginType(entry.getKey().getQualifiedName() + "State", "class", Modifier.FINAL);
                jw.beginMethod(null, entry.getKey().getSimpleName().toString()+"State", Modifier.PRIVATE);
                jw.endMethod();

                writeOnSaveInstanceState(jw, entry.getKey(), entry.getValue());
                writeOnRestoreInstanceState(jw, entry.getKey(), entry.getValue());
                jw.endType();

                jw.close();
            } catch (IOException e) {
                error(entry.getKey(), "Could not create state support class", e);
            }

        }
        return true;
    }

    private void writeOnRestoreInstanceState(JavaWriter jw, TypeElement key, Set<AnnotatedField> fields) throws IOException {
        jw.beginMethod("void", "restoreInstanceState", Modifier.STATIC, key.getQualifiedName().toString(), "target", "android.os.Bundle", "savedInstanceState");
        jw.beginControlFlow("if (savedInstanceState == null)");
        jw.emitStatement("return");
        jw.endControlFlow();

        for (AnnotatedField field : fields) {
            String op = getOperation(field);
            if (op == null) {
                error(field.getElement(), "Can't write injector, the bundle getter is unknown");
                return;
            }
            String cast = "Serializable".equals(op) ? "("+field.getType()+") " :  "";
            jw.emitStatement("target.%1$s = %4$ssavedInstanceState.get%2$s(\"%3$s\")", field.getName(), op, field.getKey(), cast);
        }
        jw.endMethod();
    }

    private void writeOnSaveInstanceState(JavaWriter jw, TypeElement key, Set<AnnotatedField> fields) throws IOException {
        jw.beginMethod("void", "saveInstanceState", Modifier.STATIC, key.getQualifiedName().toString(), "source", "android.os.Bundle", "outState");
        for (AnnotatedField field : fields) {
            writePutArguments(jw, String.format("source.%s", field.getName()), "outState", field);
        }
        jw.endMethod();
    }

}
