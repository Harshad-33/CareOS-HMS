package com.careos.hms.authentication.mapper;

import com.careos.hms.authentication.dto.AuthUserDto;
import com.careos.hms.authentication.dto.UserPermissionsResponse;
import com.careos.hms.security.CareOsUserPrincipal;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public AuthUserDto toAuthUserDto(CareOsUserPrincipal principal) {
        return new AuthUserDto(
                principal.getPublicId(),
                principal.getUsername(),
                principal.getEmail(),
                principal.getFirstName(),
                principal.getLastName(),
                principal.getFullName(),
                principal.getUserScope(),
                principal.getHospitalId(),
                principal.getHospitalCode(),
                principal.getHospitalName(),
                principal.getRoles(),
                principal.getPermissions(),
                principal.getStatus(),
                principal.isMustChangePassword(),
                principal.getLastLoginAt()
        );
    }

    public UserPermissionsResponse toPermissionsResponse(CareOsUserPrincipal principal) {
        return new UserPermissionsResponse(
                principal.getUserScope(),
                principal.getHospitalId(),
                principal.getRoles(),
                principal.getPermissions()
        );
    }
}
