package com.lexify.mapper;

import com.lexify.dto.WordResponse;
import com.lexify.model.Word;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WordMapper {

    WordResponse toResponse(Word word);
}
