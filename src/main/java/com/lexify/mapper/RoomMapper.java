package com.lexify.mapper;

import com.lexify.dto.RoomResponse;
import com.lexify.model.ChatRoom;
import com.lexify.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoomMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "moderatorIds", source = "moderators")
    RoomResponse toResponse(ChatRoom room);

    default Long toId(User user) {
        return user.getId();
    }
}
