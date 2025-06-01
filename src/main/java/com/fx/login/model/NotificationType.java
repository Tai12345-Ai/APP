package com.fx.login.model; // Hoặc package phù hợp hơn

public enum NotificationType {
    GENERAL("Thông báo chung"),
    IMPORTANT("Thông báo quan trọng"),
    FEE_RENT("Thông báo phí thuê"),
    MAINTENANCE("Thông báo bảo trì"),
    EVENT("Sự kiện"),
    NEWS("Tin tức");



    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static NotificationType fromString(String text) {
        for (NotificationType type : NotificationType.values()) {
            if (type.displayName.equalsIgnoreCase(text) || type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        return null;
    }

}