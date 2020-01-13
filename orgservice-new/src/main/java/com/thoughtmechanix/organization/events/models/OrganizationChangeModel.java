package com.thoughtmechanix.organization.events.models;

import lombok.Data;

@Data
public class OrganizationChangeModel {
    private String type;
    private String action;
    private String organizationId;
    private String correlationId;


    public OrganizationChangeModel(String type, String action, String organizationId, String correlationId) {
        super();
        this.type   = type;
        this.action = action;
        this.organizationId = organizationId;
        this.correlationId = correlationId;
    }
}