package com.banking.transactionservice.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name="transaction")
public class transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
     private String  id;
    @Column(nullable = false)
    private String receiverAccountNumber;
    @Column(nullable = false,precision = 15,scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;
    private String failureReason;
    private String referenceNumber;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
