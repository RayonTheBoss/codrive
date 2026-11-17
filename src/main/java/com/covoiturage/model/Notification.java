package com.covoiturage.model;

import com.covoiturage.enums.CanalNotification;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idNotification;
    private String message;
    private LocalDateTime dateEnvoi;

    @Enumerated(EnumType.STRING)
    private CanalNotification canal;

    public Notification() {}

    public Notification(String message, CanalNotification canal) {
        this.message = message;
        this.canal = canal;
        this.dateEnvoi = LocalDateTime.now();
    }

    public Integer getIdNotification() { return idNotification; }
    public void setIdNotification(Integer idNotification) { this.idNotification = idNotification; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
    public CanalNotification getCanal() { return canal; }
    public void setCanal(CanalNotification canal) { this.canal = canal; }
}
