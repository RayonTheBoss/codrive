package com.covoiturage.service;

import com.covoiturage.enums.CanalNotification;
import com.covoiturage.model.Notification;
import com.covoiturage.model.User;
import org.springframework.stereotype.Service;

@Service
public class NotificationService implements INotificationService {
    @Override
    public void notifierSMS(User user, String message) {
        Notification notification = new Notification(message, CanalNotification.SMS);
        user.ajouterNotification(notification);
    }

    @Override
    public void notifierEmail(User user, String message) {
        Notification notification = new Notification(message, CanalNotification.EMAIL);
        user.ajouterNotification(notification);
    }
}
