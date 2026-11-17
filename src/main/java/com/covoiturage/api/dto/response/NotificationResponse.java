package com.covoiturage.api.dto.response;

public class NotificationResponse {
    private int idNotification;
    private String message;
    private String dateEnvoi;
    private String canal;

    public NotificationResponse() {}

    public NotificationResponse(int idNotification, String message, String dateEnvoi, String canal) {
        this.idNotification = idNotification;
        this.message = message;
        this.dateEnvoi = dateEnvoi;
        this.canal = canal;
    }

    public int getIdNotification() { return idNotification; }
    public void setIdNotification(int idNotification) { this.idNotification = idNotification; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(String dateEnvoi) { this.dateEnvoi = dateEnvoi; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
}
