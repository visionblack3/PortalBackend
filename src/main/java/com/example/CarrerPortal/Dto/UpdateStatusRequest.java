package com.example.CarrerPortal.Dto;

public class UpdateStatusRequest {
    private String status;

    // Default constructor (required by Jackson for JSON deserialization)
    public UpdateStatusRequest() {}

    public UpdateStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}