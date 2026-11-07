package com.banking.accountservice.dto;

import com.banking.accountservice.entity.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class CreateAccountRequest {
    @NotBlank(message= "Account Holders Name is required")
    private String accountHolderNames;
    @NotBlank(message=" Email is required")
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message=" Phone Number is required")
    private String phone;
    @NotNull(message="Account type is required")
    private AccountType accountType;
    @NotNull(message="Intial deposit is required")
    @Positive(message="Intial Deposit must be Positive ")
    private BigDecimal intialDeposit;

}
