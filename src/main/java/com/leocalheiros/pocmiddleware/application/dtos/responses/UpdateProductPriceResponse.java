package com.leocalheiros.pocmiddleware.application.dtos.responses;

import com.leocalheiros.pocmiddleware.application.dtos.requests.UpdateProductPriceRequest;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductPriceResponse {
    private List<UpdateProductPriceRequest> precos;
}
