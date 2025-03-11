package com.bgpay.bgai.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class CustomErrorResponse {
    private HttpStatus status;
    private String errorCode;
    private String message;
}