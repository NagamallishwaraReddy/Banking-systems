package com.banking.fraudservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import java.math.BigDecimal;



@FeignClient(name="account-service",url="${account.service.url}")
public interface AccountServiceClient {
    @GetExchange("/api/v1/accounts/{accountNumber}/balance")
     BigDecimal getBalance(
            @PathVariable String accountNumber
    );






}

