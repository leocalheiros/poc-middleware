package com.leocalheiros.pocmiddleware.domain.models;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AuthorizationToken {
    private String apiKey;
    private String secretKey;
}
