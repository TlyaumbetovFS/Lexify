package com.lexify.mapper;

import com.lexify.dto.MessageResponse;
import com.lexify.model.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MessageMapper {

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "authorId", source = "author.id")
    MessageResponse toResponse(ChatMessage message);
}
