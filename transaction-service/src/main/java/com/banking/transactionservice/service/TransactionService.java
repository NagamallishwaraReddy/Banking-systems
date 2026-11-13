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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.Authenticator;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate kafkaTemplate;
    private final RedisTemplate<String,String> redisTemplate;
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
    public TransactionResponse verifyOtp(String transactionId,String otp){
        log.info("OTP verification for the transaction :{}",transactionId);
        Transaction transaction=transactionRepository.findById(transactionId).orElseThrow(()-> new RuntimeException("Transaction not found"+transactionId));
        String otpkey="verification:otp"+transactionId;
        String storedOtp=redisTemplate.opsForValue().get(otpkey);
        if(storedOtp==null){
            //OTP expired
            log.warn("OTP expired for transaction:{}",transactionId);
            compensateTransaction(transaction,"OTP expired - transaction cancelled and amount refunded");
            return mapToResponse(transaction);
        }
        if(storedOtp.equals(otp)){
//            Block account and refund
            log.warn("Wrong OTP - blocking acount and refunding :{}",transactionId);
            redisTemplate.delete(otpkey);
            blockAccountAndCompensate(transaction, "Wrong OTP entered: transaction cancelled,"+"account blocked for the security");
            return mapToResponse(transaction);
        }
        //OTP COrrect complete transaction
        log.info("OTP verified - completing transaction:{}",transactionId);
        completeTransaction(transaction);
        return mapToResponse(transaction);

    }
    private void compensateTransaction(Transaction transaction,String reason){
        log.warn("SAGA COMPENSATION - refunding :{} amount :{}",
                transaction.getSenderAccountNumber(),
                transaction.getAmount());
           // Credit money back to the sender  synchronously
        accountServiceClient.creditBalance(
                transaction.getSenderAccountNumber(),transaction.getAmount());
         transaction.setStatus(TransactionStatus.FLAGGED);
         transaction.setFailureReason(reason + "SAGA COMPENSATION executed amount refunded at "+ LocalDateTime.now());
         transactionRepository.save(transaction);
         //Public refund event -notification service will alert user
        Map<String,Object> refundEvent=new HashMap<>();
        refundEvent.put("transactionId",transaction.getId());
        refundEvent.put("senderAccountNumber",transaction.getSenderAccountNumber());
        refundEvent.put("amount",transaction.getAmount());
        refundEvent.put("reason",reason);
        kafkaTemplate.send(TRANSACTION_REFUND_TOPIC,transaction.getId(),refundEvent);
        log.info("SAGA COMPENSATION COMPLETE - {} refunded to {} ",transaction.getAmount(),transaction.getSenderAccountNumber());





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
