package mx.edu.unpa.inventory_backend.mappers;

import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    @Mapping(target = "fullName",       expression = "java(user.getGuardian().getFullName())")
    @Mapping(target = "email",          expression = "java(user.getGuardian().getEmail())")
    @Mapping(target = "employeeNumber", expression = "java(user.getGuardian().getEmployeeNumber())")
    @Mapping(target = "guardian",       source = "guardian")
    UserDetailResponse toDetail(User user);

    @Mapping(target = "fullName",       expression = "java(user.getGuardian().getFullName())")
    @Mapping(target = "email",          expression = "java(user.getGuardian().getEmail())")
    @Mapping(target = "employeeNumber", expression = "java(user.getGuardian().getEmployeeNumber())")
    UserSummaryResponse toSummary(User user);
}
