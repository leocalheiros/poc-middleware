package com.leocalheiros.pocmiddleware.application.dtos.responses;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefaultResponse {
    private long id;
    private String error;
    private List<String> details;
}
