package com.mappers;

import com.mappertests.annotations.AfterDataGeneration;
import com.mappertests.annotations.DataGenerator;
import com.mappertests.annotations.MapperTests;

import com.mappers.MockClass;
import com.mappertests.annotations.TargetMapper;

import static org.junit.jupiter.api.Assertions.*;

@MapperTests
class MockClassTestU {
    @TargetMapper
    MockClass mockClass;

    @DataGenerator
    public MockBO generate() {
        return null;
    }

    @AfterDataGeneration
    public MockBO after(final MockBO generated) {
        return null;
    }
}