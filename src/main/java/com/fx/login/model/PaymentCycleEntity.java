package com.fx.login.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "payment_cycle")
public class PaymentCycleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fee_id")
    private FeeEntity fee;

    @Column(name = "cycle_type")
    private String cycleType; // "MONTHLY", "QUARTERLY", "YEARLY"

    @Column(name = "last_generated")
    private LocalDate lastGenerated;

    @Column(name = "next_due")
    private LocalDate nextDue;

    @Column(name = "is_active")
    private boolean active;

    // Constructors
    public PaymentCycleEntity() {}

    public PaymentCycleEntity(FeeEntity fee, String cycleType, LocalDate nextDue) {
        this.fee = fee;
        this.cycleType = cycleType;
        this.nextDue = nextDue;
        this.active = true;
        this.lastGenerated = LocalDate.now();
    }
}