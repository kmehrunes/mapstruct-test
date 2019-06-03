package com.mappers;

import com.mappertests.annotations.AfterDataGeneration;
import com.mappertests.annotations.DataGenerator;
import com.mappertests.annotations.MapperTests;
import com.mappertests.annotations.TargetMapper;

@MapperTests
public class MockClassTest {
    @TargetMapper
    MockClass mapper;

    @DataGenerator
    public MockBO generate() {
        return null;
    }

    @AfterDataGeneration
    public MockBO after(final MockBO generated) {
        return null;
    }

    @AfterDataGeneration
    public MockDO afterDO(final MockDO generated) {
        return null;
    }
}
