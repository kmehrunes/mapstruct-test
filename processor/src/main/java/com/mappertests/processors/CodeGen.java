package com.mappertests.processors;

import com.squareup.javapoet.*;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.*;

class CodeGen {
    private Messager messager;
    private ProcessingEnvironment processingEnv;

    CodeGen(Messager messager, ProcessingEnvironment processingEnv) {
        this.messager = messager;
        this.processingEnv = processingEnv;
    }

    JavaFile generateTests(final CodeGenData codeGenData) {
        List<MethodSpec> methodSpecs = new ArrayList<>();
        VariableElement mapperInstance = codeGenData.getMapperInstance();

        for (final CodeGenData.MapperMethod method : codeGenData.getMethods()) {
            messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "--- Sub-element: " + method.getMethod().getSimpleName());
            final CodeGenData.MapperMethod inverseMethod = findMapper(codeGenData.getMethods(), method.getToType(),
                    method.getFromType(), processingEnv).orElse(null);

            methodSpecs.add(generateSuccessCase(codeGenData, method, inverseMethod, method.getSpecialMappings()));
            methodSpecs.add(generateNullObjectCase(mapperInstance, method));
            methodSpecs.add(generateNullFieldsCase(mapperInstance, method));
        }

        TypeSpec classSpecs = generateTestClass(methodSpecs, codeGenData.getMapperTestClass());
        JavaFile testFile = generateTestFile(classSpecs, extractPackage(codeGenData.getMapperTestClass()));

        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, testFile.toString());

        codeGenData.getDataGenerators().forEach((key, value) -> messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                "Data generator for " + key + " : " + value.toString()));
        codeGenData.getDataModifiers().forEach((key, value) -> messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                "Data post processor for " + key + " : " + value.toString()));

        return testFile;
    }

    private AnnotationSpec testAnnotation(int invocationCount) {
        return AnnotationSpec.builder(ClassName.get("org.testng.annotations", "Test"))
                .addMember("invocationCount", "$L", invocationCount)
                .build();
    }

    private JavaFile generateTestFile(final TypeSpec testClass, final String testPackage) {
        return JavaFile.builder(testPackage, testClass)
                .build();
    }

    private TypeSpec generateTestClass(final List<MethodSpec> methodSpecs, final TypeElement mapperTest) {
        return TypeSpec.classBuilder("HelloWorld")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethods(methodSpecs)
                .superclass(ClassName.get("", mapperTest.getSimpleName().toString()))
                .build();
    }

    private MethodSpec generateSuccessCase(final CodeGenData codeGenData, final CodeGenData.MapperMethod method,
                                           final CodeGenData.MapperMethod inverseMethod, final SpecialMappings specialMappings) {
        Objects.requireNonNull(inverseMethod);

        String testMethodName = generateTestMethodName(method.getMethod(), "");
        Map<String, String> subs = generateSubstitutions(codeGenData.getMapperInstance(), method, inverseMethod);

        CodeBlock code = CodeBlock.builder()
                .addNamed(generateObjectInstantiation(method.getToType(), codeGenData.getDataGenerators(),
                        codeGenData.getDataModifiers()), subs)
                .addNamed("$targetType:N mapped = $mapper:N.$method:N($mapper:N.$inverseMethod:N(generated));\n", subs)
                .addNamed("org.junit.Assert.assertEquals(generated, mapped)", subs)
                .build();

        return generateTestCase(testMethodName, code, 10);
    }

    private MethodSpec generateNullObjectCase(final VariableElement mapperInstance, final CodeGenData.MapperMethod method) {
        String testMethodName = generateTestMethodName(method.getMethod(), "NullObject");
        Map<String, String> subs = generateSubstitutions(mapperInstance, method);

        CodeBlock code = CodeBlock.builder()
                .addNamed("org.junit.Assert.assertNull($mapper:N.$method:N(null))", subs)
                .build();

        return generateTestCase(testMethodName, code);
    }

    private MethodSpec generateNullFieldsCase(final VariableElement mapperInstance, final CodeGenData.MapperMethod method) {
        String testMethodName = generateTestMethodName(method.getMethod(), "NullFields");
        Map<String, String> subs = generateSubstitutions(mapperInstance, method);

        CodeBlock code = CodeBlock.builder()
                .addNamed("org.junit.Assert.assertNull($mapper:N.$method:N(null))", subs)
                .build();

        return generateTestCase(testMethodName, code);
    }

    private MethodSpec generateTestCase(final String testMethodName, final CodeBlock codeBlock) {
        return generateTestCase(testMethodName, codeBlock, 1);
    }

    private MethodSpec generateTestCase(final String testMethodName, final CodeBlock codeBlock, final int invocations) {
        return MethodSpec.methodBuilder(testMethodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(testAnnotation(invocations))
                .returns(void.class)
                .addStatement(codeBlock)
                .build();
    }

    private String generateObjectInstantiation(final TypeMirror objectType, final Map<String, ExecutableElement> generators,
                                               final Map<String, ExecutableElement> postGenerators) {
        List<TypeMirror> superTypes = getSuperTypes(objectType);
        Optional<ExecutableElement> generator = Optional.ofNullable(generators.get(objectType.toString()));
        Optional<ExecutableElement> postGenerator = findProcessor(objectType, superTypes, postGenerators);

        StringBuilder statement = new StringBuilder("$targetType:N generated = ");
        String objectCreation = generator
                .map(method -> method.getSimpleName().toString() + "()")
                .orElse("$randomizer:N.nextObject($targetType:N.class)");

        if (postGenerator.isPresent()) {
            statement.append(postGenerator.get().getSimpleName())
                    .append("(").append(objectCreation).append(")");
        } else {
            statement.append(objectCreation);
        }

        return statement.append(";\n").toString();
    }

    private List<TypeMirror> getSuperTypes(final TypeMirror typeMirror) {
        List<TypeMirror> superTypes = new ArrayList<>();
        Queue<TypeMirror> typesStack = new LinkedList<>();

        typesStack.add(typeMirror);

        while (!typesStack.isEmpty()) {
            TypeMirror current = typesStack.remove();
            List<? extends TypeMirror> directSupertypes = processingEnv.getTypeUtils().directSupertypes(current);
            superTypes.addAll(directSupertypes);
            typesStack.addAll(directSupertypes);
        }

        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Supertypes of " + typeMirror + ": " + superTypes);

        return superTypes;
    }

    private Optional<ExecutableElement> findProcessor(final TypeMirror objectType, final List<TypeMirror> superTypes,
                                                      final Map<String, ExecutableElement> processors) {
        if (processors.containsKey(objectType.toString())) {
            return Optional.of(processors.get(objectType.toString()));
        } else {
            return superTypes.stream()
                    .map(TypeMirror::toString)
                    .map(processors::get)
                    .filter(Objects::nonNull)
                    .findFirst();
        }
    }

    private String extractPackage(final TypeElement typeElement) {
        List<String> parts = Arrays.asList(typeElement.getQualifiedName().toString().split("\\."));
        return String.join(".", parts.subList(0, parts.size() - 1));
    }

    private String generateTestMethodName(final ExecutableElement method, final String postfix) {
        return method.getSimpleName() + "Test" + postfix;
    }

    private Optional<CodeGenData.MapperMethod> findMapper(final List<CodeGenData.MapperMethod> methods ,
                                                          final TypeMirror fromType, final TypeMirror toType,
                                                          final ProcessingEnvironment env) {
        return methods.stream()
                .filter(method -> env.getTypeUtils().isSameType(method.getFromType(), fromType))
                .filter(method -> env.getTypeUtils().isSameType(method.getToType(), toType))
                .findFirst();
    }

    private Map<String, String> generateSubstitutions(final VariableElement mapperInstance, final CodeGenData.MapperMethod method) {
        Map<String, String> subs = new HashMap<>();

        subs.put("randomizer", "io.github.benas.randombeans.api.EnhancedRandom");
        subs.put("mapper", mapperInstance.getSimpleName().toString());
        subs.put("targetType", method.getToType().toString());
        subs.put("method", method.getMethod().getSimpleName().toString());

        return subs;
    }

    private Map<String, String> generateSubstitutions(final VariableElement mapperInstance,
                                                      final CodeGenData.MapperMethod method,
                                                      final CodeGenData.MapperMethod inverseMethod) {
        Map<String, String> subs = new HashMap<>();

        subs.put("randomizer", "io.github.benas.randombeans.api.EnhancedRandom");
        subs.put("mapper", mapperInstance.getSimpleName().toString());
        subs.put("targetType", method.getToType().toString());
        subs.put("method", method.getMethod().getSimpleName().toString());
        subs.put("inverseMethod", inverseMethod.getMethod().getSimpleName().toString());

        return subs;
    }
}
