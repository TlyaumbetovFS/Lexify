package com.lexify.mapper;

import com.lexify.dto.DictionaryData;
import com.lexify.model.DictionaryEntry;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DictionaryMapper {

    DictionaryEntry toEntry(DictionaryData data, String word, Instant checkedAt);
}
