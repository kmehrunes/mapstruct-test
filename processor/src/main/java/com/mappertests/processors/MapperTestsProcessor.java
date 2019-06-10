package com.mappertests.processors;

import com.mappertests.annotations.AfterDataGeneration;
import com.mappertests.annotations.DataGenerator;
import com.mappertests.annotations.MapperTests;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mappertests.annotations.TargetMapper;
import com.squareup.javapoet.JavaFile;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;

public class MapperTestsProcessor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;
    private CodeGen codeGen;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        codeGen = new CodeGen(messager, processingEnv);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Arrays.asList(
                MapperTests.class.getCanonicalName(),
                DataGenerator.class.getCanonicalName()
        ));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Map<String, TypeElement> allMappers = roundEnv.getElementsAnnotatedWith(Mapper.class).stream()
                .map(element -> (TypeElement) element)
                .collect(Collectors.toMap(element -> element.getQualifiedName().toString(), Function.identity()));

        allMappers.forEach((key, value) -> {
            messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Mapper " + value.getQualifiedName());
        });

        for (Element element : roundEnv.getElementsAnnotatedWith(MapperTests.class)) {
            final TypeElement mapperTest = (TypeElement) element;

            messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Reading element " + element.getSimpleName());

            final CodeGenData codeGenData = extractCodeGenData(mapperTest, allMappers);
            JavaFile javaFile = codeGen.generateTests(codeGenData);

            try {
                javaFile.writeTo(filer);
            } catch (IOException ex) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write file");
            }
        }

        return true;
    }

    private CodeGenData extractCodeGenData(final TypeElement mapperTest, final Map<String, TypeElement> allMappers) {
        final VariableElement mapperInstance = extractMapperInstance(mapperTest);
        final TypeElement mapper = getMapper(mapperInstance, allMappers);
        final CodeGenData codeGenData = new CodeGenData();

        codeGenData.setMapperTestClass(mapperTest);
        codeGenData.setMapper(mapper);
        codeGenData.setMapperInstance(mapperInstance);
        codeGenData.setMethods(extractMapperMethods(mapper));
        codeGenData.setDataGenerators(extractDataGenerators(mapperTest));
        codeGenData.setDataModifiers(extractAfterDataMappers(mapperTest));

        return codeGenData;
    }

    private VariableElement extractMapperInstance(final TypeElement mapperTest) {
        return mapperTest.getEnclosedElements().stream()
                .filter(element -> element.getAnnotation(TargetMapper.class) != null)
                .findFirst()
                .map(element -> (VariableElement) element)
                .orElseThrow(() -> new RuntimeException("Could not find a mapper, use @TargetMapper"));
    }

    private TypeElement getMapper(final VariableElement mapperInstance, final Map<String, TypeElement> allMappers) {
        final TypeMirror mapperType = mapperInstance.asType();
        final TypeElement mapper = allMappers.get(mapperType.toString());

        if (mapper == null) {
            throw new NoClassDefFoundError("No class found for " + mapperType);
        }

        return mapper;
    }

    private List<CodeGenData.MapperMethod> extractMapperMethods(final TypeElement mapper) {
        final List<CodeGenData.MapperMethod> methods = new ArrayList<>();

        for (Element child : mapper.getEnclosedElements()) {
            messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "--- Sub-element: " + child.getSimpleName());

            if (child.getKind() == ElementKind.METHOD) {
                final CodeGenData.MapperMethod method = new CodeGenData.MapperMethod();
                method.setMethod((ExecutableElement) child);
                method.setSpecialMappings(extractSpecialMappings((ExecutableElement) child));

                methods.add(method);
            }
        }

        return methods;
    }

    private SpecialMappings extractSpecialMappings(final ExecutableElement method) {
        final Optional<Mappings> mappingsAnnotations = Optional.ofNullable(method.getAnnotation(Mappings.class));
        final SpecialMappings specialMappings = new SpecialMappings();

        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "---- processing mappings");

        mappingsAnnotations.ifPresent(mappings -> Stream.of(mappings.value())
                .forEach(mapping -> {
                    if (mapping.ignore()) {
                        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "----- ignoring " + mapping.target());
                        specialMappings.ignoreTargetField(mapping.target());
                    }
                    else if (!mapping.source().isEmpty()) {
                        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "----- mapping " +
                                mapping.source() + " -> " + mapping.target());
                        specialMappings.addFieldMapping(mapping.source(), mapping.target());
                    }
                })
        );

        return specialMappings;
    }

    private Map<String, ExecutableElement> extractDataGenerators(final Set<? extends Element> dataGenerators) {
        return dataGenerators.stream()
                .map(element -> (ExecutableElement) element)
                .collect(Collectors.toMap(element -> element.getReturnType().toString(), Function.identity()));
    }

    private Map<String, ExecutableElement> extractDataGenerators(final TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .filter(method -> method.getAnnotation(DataGenerator.class) != null)
                .map(method -> (ExecutableElement) method)
                .collect(Collectors.toMap(element -> element.getReturnType().toString(), Function.identity()));
    }

    private Map<String, ExecutableElement> extractAfterDataMappers(final TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .filter(method -> method.getAnnotation(AfterDataGeneration.class) != null)
                .map(method -> (ExecutableElement) method)
                .collect(Collectors.toMap(element -> element.getReturnType().toString(), Function.identity()));
    }
}
