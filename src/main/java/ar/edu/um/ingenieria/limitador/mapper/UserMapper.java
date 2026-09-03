package ar.edu.um.ingenieria.limitador.mapper;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.dto.UserDTO;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "firstName", source = "userData.firstName")
    @Mapping(target = "lastName", source = "userData.lastName")
    @Mapping(target = "phoneNumber", source = "userData.phoneNumber")
    @Mapping(target = "address", source = "userData.address")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStrings")
    UserDTO toDto(User user);

    @Mapping(target = "userData.firstName", source = "firstName")
    @Mapping(target = "userData.lastName", source = "lastName")
    @Mapping(target = "userData.phoneNumber", source = "phoneNumber")
    @Mapping(target = "userData.address", source = "address")
    @Mapping(target = "userData.id", ignore = true)
    @Mapping(target = "userData.user", ignore = true)
    @Mapping(target = "roles", source = "roles", qualifiedByName = "stringsToRoles")
    User toEntity(UserDTO dto);

    List<UserDTO> toDtoList(List<User> users);

    List<User> toEntityList(List<UserDTO> dtos);

    @Named("rolesToStrings")
    default Set<String> rolesToStrings(Set<Role> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
            .map(Role::getRoleName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    @Named("stringsToRoles")
    default Set<Role> stringsToRoles(Set<String> roleNames) {
        if (roleNames == null) {
            return null;
        }
        return roleNames.stream()
            .map(name -> {
                Role role = new Role();
                role.setRoleName(name);
                role.setDescription(name != null ? name : "User Role");
                return role;
            })
            .collect(Collectors.toSet());
    }
}
