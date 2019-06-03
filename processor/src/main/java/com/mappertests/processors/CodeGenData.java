package com.mappertests.processors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class CodeGenData {
    static class MapperMethod {
        private ExecutableElement method;
        private SpecialMappings specialMappings;
        private TypeMirror fromType;
        private TypeMirror toType;

        ExecutableElement getMethod() {
            return method;
        }

        void setMethod(ExecutableElement method) {
            this.method = method;
            this.toType = method.getReturnType();
            this.fromType = method.getParameters().stream()
                    .findFirst()
                    .map(VariableElement::asType)
//                    .orElse(null);
                    .orElseThrow(() -> new RuntimeException("No arguments!! Not a mapper method " + method.getSimpleName()));
        }

        SpecialMappings getSpecialMappings() {
            return specialMappings;
        }

        void setSpecialMappings(SpecialMappings specialMappings) {
            this.specialMappings = specialMappings;
        }

        public TypeMirror getFromType() {
            return fromType;
        }

        public void setFromType(TypeMirror fromType) {
            this.fromType = fromType;
        }

        public TypeMirror getToType() {
            return toType;
        }

        public void setToType(TypeMirror toType) {
            this.toType = toType;
        }
    }

    private TypeElement mapper;
    private TypeElement mapperTestClass;
    private VariableElement mapperInstance;

    private List<MapperMethod> methods;
    private Map<String, ExecutableElement> dataGenerators;
    private Map<String, ExecutableElement> dataModifiers;

    public TypeElement getMapper() {
        return mapper;
    }

    void setMapper(TypeElement mapper) {
        this.mapper = mapper;
    }

    TypeElement getMapperTestClass() {
        return mapperTestClass;
    }

    void setMapperTestClass(TypeElement mapperTestClass) {
        this.mapperTestClass = mapperTestClass;
    }

    public VariableElement getMapperInstance() {
        return mapperInstance;
    }

    public void setMapperInstance(VariableElement mapperInstance) {
        this.mapperInstance = mapperInstance;
    }

    List<MapperMethod> getMethods() {
        return methods;
    }

    void setMethods(List<MapperMethod> methods) {
        this.methods = methods;
    }

    Map<String, ExecutableElement> getDataGenerators() {
        return dataGenerators;
    }

    void setDataGenerators(Map<String, ExecutableElement> dataGenerators) {
        this.dataGenerators = dataGenerators;
    }

    Map<String, ExecutableElement> getDataModifiers() {
        return dataModifiers;
    }

    void setDataModifiers(Map<String, ExecutableElement> dataModifiers) {
        this.dataModifiers = dataModifiers;
    }
}
