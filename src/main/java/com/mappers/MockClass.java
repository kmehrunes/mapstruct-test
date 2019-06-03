package com.mappers;

import com.mappertests.annotations.MapperTests;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MockClass {
    MockClass mapper = Mappers.getMapper(MockClass.class);

    @Mapping(source = "boName", target = "name")
    @Mapping(source = "boAddress", target = "address")
    @Mapping(target = "id", ignore = true)
    MockDO toDO(MockBO mockBO);

    @Mapping(source = "boName", target = "name")
    @Mapping(source = "boAddress", target = "address")
    @Mapping(target = "id", ignore = true)
    MockBO toBO(MockDO mockDO);
}
