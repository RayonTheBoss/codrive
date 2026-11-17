package com.covoiturage.service;

import com.covoiturage.model.User;

public interface INotificationService {
    void notifierSMS(User user, String message);
    void notifierEmail(User user, String message);
}
