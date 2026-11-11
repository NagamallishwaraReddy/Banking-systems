package com.banking.transactionservice.dto;

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
public class TransferRequest {
    @NotBlank(message="Receivers AccountNumber must be required")
    private String receiverAccountNumber;
    @NotBlank(message="Senders AccountNumber must be required")
    private String senderAccountNumber;
    @NotNull(message="Amount must be required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    private String description;


}
