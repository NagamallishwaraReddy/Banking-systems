package com.banking.transactionservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionInitiatedEvent {
    private String transaction;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal Amount;
    private String description;

}
