package com.banking.transactionservice.entity;
//PENDING->PROCESSING->COMPLETED(CLEAN TRANSACTION)
//                   ->PENDING VERIFICATION(SUSPICIOUS DETECTED )
//                             ->COMPLETED(VERIFIED)
//                             ->FLAGGED(SAGA REFUND)
//                     ->FAILED
//                     ->FLAGGED
public enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    PROCESSING_VERIFICATION,
    FAILED,
    FLAGGED;


}
