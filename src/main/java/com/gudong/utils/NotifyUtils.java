package com.gudong.utils;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * description 提示
 *
 * @author maggie
 * @date 2021-07-21 14:15
 */
public class NotifyUtils {
    public static void waring(String msg, Project project) {
        notify(msg, project, NotificationType.WARNING);
    }

    public static void error(String msg, Project project) {
        notify(msg, project, NotificationType.ERROR);
    }

    public static void info(String msg, Project project) {
        notify(msg, project, NotificationType.INFORMATION);
    }

    private static void notify(String msg, Project project, NotificationType type) {
        Notification nodt = new Notification("com.gudong.action.FindResourceAction", "check plugin", msg, type);
        nodt.notify(project);
    }
}
