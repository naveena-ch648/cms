package com.cms.dto.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String status;
}
