package com.leocalheiros.pocmiddleware.application.dtos.requests;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductPriceRequest {
    private String sku;
    private String precoDe;
    private String precoPor;
}