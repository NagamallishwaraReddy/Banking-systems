package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import com.banking.accountservice.dto.CreateAccountRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public  class AccountService {
    public final AccountRepository accountRepository;
    private static SecureRandom secureRandom=new SecureRandom();
    public  AccountResponse createAccount(CreateAccountRequest request){
        log.info("Creating account for {}:",request.getEmail());
        if(accountRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Account Already exists for "+request.getEmail());
        }
        Account account=new Account();
        account.setAccountHolderName(request.getAccountHolderNames());
        account.setGmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getIntialDeposit());
        account.setAccountNumber(generateAccountNumber());
        account.setDailyTransactionLimit(
                request.getAccountType()== AccountType.SAVINGS ? new BigDecimal("100000"):new BigDecimal("500000"));
                Account savedAccount=accountRepository.save(account);
                log.info("Account created :{}",savedAccount.getAccountNumber());
                return mapToResponse(savedAccount);
    }
//    Get Account Details By giving the account Number
    public AccountResponse getAccount(String accountNumber){
        Account account=accountRepository.findByAccountNumber(accountNumber).orElseThrow(()-> new RuntimeException("Account not found"));
        return mapToResponse(account);
    }
//    Get the Balance of the account
    public BigDecimal getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found"));
        return account.getBalance();
    }
    // Block account by Fraud Detection by using Kafka By the param is Account Number
    public void blockAccount(String accountNumber){
        log.info("Blocking Account : {}",accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found"));
       account.setStatus(AccountStatus.BLOCKED);
       accountRepository.save(account);
       log.info("Account Blocked : {}",accountNumber);
    }
//    Deducting amount for the balance of the sender drom the transaction service
    public void deductBalance(String accountNumber,BigDecimal amount){
        log.info("Deduction balance {} from the balance :{}",amount,accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found"));
      if(account.getStatus()!=AccountStatus.ACTIVE){
          throw new RuntimeException("Account is not active "+accountNumber);
      }
      if(account.getBalance().compareTo(amount)<0){
          log.info("Insufficient funds for the account"+accountNumber);
      }
      account.setBalance(account.getBalance().subtract(amount));
      accountRepository.save(account);
      log.info("Balance updated New Balance :{}",account.getBalance());
    }
    //Credit balance called by transaction service by kafka
    public void creditBalance(String accountNumber,BigDecimal amount){
        log.info("Crediting {} to account :{}",amount,accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found"));
       account.setBalance(account.getBalance().add(amount));
       accountRepository.save(account);
       log.info("Balance credited New Balance is :{}",account.getBalance());

    }
//    Generate Unique 12digit number
    private String generateAccountNumber(){
        String accountNumber;
        do {
            long number=secureRandom.nextLong(1_000_000_000_000L);
            accountNumber=String.format("%012d",number);


        }while(accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;

    }
    private AccountResponse mapToResponse( Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setGmail(account.getGmail());
        response.setPhone(account.getPhone());
        response.setAccountType(account.getAccountType());
        response.setStatus(account.getStatus());
        response.setBalance(account.getBalance());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        response.setCreatedAt(account.getCreatedAt());
        return response;
    }
    }