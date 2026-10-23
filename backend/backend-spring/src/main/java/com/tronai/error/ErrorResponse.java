package com.tronai.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorResponse {
    private String detail;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String code;

    public ErrorResponse(String detail) {
        this.detail = detail;
    }

    public ErrorResponse(String detail, String code) {
        this.detail = detail;
        this.code = code;
    }
}
