package com.covoiturage.api.controller;

import com.covoiturage.api.dto.response.NotificationResponse;
import com.covoiturage.api.security.RequestAuthHelper;
import com.covoiturage.model.Notification;
import com.covoiturage.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final RequestAuthHelper requestAuthHelper;

    public NotificationController(RequestAuthHelper requestAuthHelper) {
        this.requestAuthHelper = requestAuthHelper;
    }

    @GetMapping
    public List<NotificationResponse> getNotifications(HttpServletRequest request) {
        User connectedUser = requestAuthHelper.requireAuthenticatedUser(request);

        List<NotificationResponse> result = new ArrayList<>();

        for (Notification notification : connectedUser.consulterNotifications()) {
            result.add(new NotificationResponse(
                    notification.getIdNotification(),
                    notification.getMessage(),
                    notification.getDateEnvoi().toString(),
                    notification.getCanal().name()
            ));
        }

        result.sort(Comparator.comparing(NotificationResponse::getDateEnvoi).reversed());
        return result;
    }
}
