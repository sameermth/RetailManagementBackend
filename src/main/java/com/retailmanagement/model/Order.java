package com.retailmanagement.model;

import com.retailmanagement.model.enums.OrderStatus;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Who created the order

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    private LocalDateTime orderDate;
    private Double totalAmount;
    private Double paidAmount = 0.0;
    private Double pendingAmount;
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
        pendingAmount = totalAmount - paidAmount;
    }
}