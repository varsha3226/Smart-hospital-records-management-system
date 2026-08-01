package com.hospital;

public class TransactionLog {
    private String timestamp;
    private String userRole;
    private String userId;
    private String action;
    private String details;

    public TransactionLog(String timestamp, String userRole, String userId,
                          String action, String details) {
        this.timestamp = timestamp;
        this.userRole = userRole;
        this.userId = userId;
        this.action = action;
        this.details = details;
    }

    public String toData() {
        return "timestamp=" + timestamp +
                ";userRole=" + userRole +
                ";userId=" + userId +
                ";action=" + action.replace(";", ",") +
                ";details=" + details.replace(";", ",");
    }
}
