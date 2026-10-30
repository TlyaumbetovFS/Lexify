package com.lexify.mapper;

import com.lexify.dto.CollectionResponse;
import com.lexify.model.WordCollection;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CollectionMapper {

    CollectionResponse toResponse(WordCollection collection);
}
