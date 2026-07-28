package com.bankagn.bankagn.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpVerifyRequest {
    private String email;
    private String code;
}