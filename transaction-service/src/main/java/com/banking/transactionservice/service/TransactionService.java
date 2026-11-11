package com.banking.transactionservice.service;

import com.banking.transactionservice.client.AccountServiceClient;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.dto.TransferRequest;
import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.entity.TransactionStatus;
import com.banking.transactionservice.entity.TransactionType;
import com.banking.transactionservice.event.TransactionInitiatedEvent;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.Authenticator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate kafkaTemplate;
    private static final String TRANSACTION_INITIATED_TOPIC="transaction.intiated";
    private static final String TRANSACTION_COMPLETED_TOPIC="transaction.completed";
    private static final String TRANSACTION_REFUND_TOPIC="transaction.refunded";
//    SAGA STEP 1:INITIATE TRANSFER
//    DEDUCES FROM THE SENDER VIA FIEGN
//    SAVES TRANSACTION AS PROCESSING
//    PUBLISH EVENT TO KAFKA TO PUBLISH
//    RETURNS
//    PARAM:@request
//     @return
    public TransactionResponse transfer(TransferRequest request){
        log.info("SAGA START - Transfer:{} -> {} amount:{}",
                request.getSenderAccountNumber(),
                request.getReceiverAccountNumber(),
                request.getAmount());
//            SAGA Step 1:Deduct from sender
              accountServiceClient.deductBalance(
                      request.getSenderAccountNumber(),
                      request.getAmount());
        Transaction transaction=new Transaction();
        transaction.setSenderAccountNumber(request.getSenderAccountNumber());
        transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setDescription(request.getDescription());
        transaction.setReferenceNumber(UUID.randomUUID().toString());
        Transaction savedTransaction=transactionRepository.save(transaction);
       //SAGA step:2 Publish for fraud check
        log.info("Transaction saved as Processing: {}",savedTransaction.getId());
      TransactionInitiatedEvent event=new TransactionInitiatedEvent(
              savedTransaction.getId(),
              savedTransaction.getSenderAccountNumber(),
              savedTransaction.getReceiverAccountNumber(),
              savedTransaction.getAmount(),
              savedTransaction.getDescription()
      );
      kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC,savedTransaction.getId(),event);
      log.info("SAGA Step-2 TransactionInitiatedEVent published:{}",savedTransaction.getId());
      return mapToResponse(savedTransaction);
    }
    public TransactionResponse getTransaction(String transactionId){
        return mapToResponse(transactionRepository.findById(transactionId).orElseThrow(()->new RuntimeException("Transaction not found")));
    }
    public List<TransactionResponse> getTransactionHistory(String accountNumber){
        return transactionRepository.findBySenderAccountOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

    }
    private TransactionResponse mapToResponse(Transaction transaction){
        TransactionResponse response=new TransactionResponse();
        response.setId(transaction.getId());
        response.setSenderAccountNumber(transaction.getSenderAccountNumber());
        response.setSenderAccountNumber(transaction.getReceiverAccountNumber());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setFailureReason(transaction.getFailureReason());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCreatedAt(transaction.getCompleted());
        return response;
    }








}
