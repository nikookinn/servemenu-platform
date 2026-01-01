package com.servemenu.businessservice.common.constant;

public final class KafkaTopics {
    // Consumer topics (from other services)
    public static final String USER_EVENTS = "user.events";
    
    // Producer topics (published by business-service)
    public static final String BUSINESS_CREATED_EVENTS = "business.business-created";
    public static final String BUSINESS_DETAILS_COMPLETED_EVENTS = "business.business-details-completed";
    public static final String BUSINESS_EVENTS = "business.business-events";
    public static final String STORE_EVENTS = "business.store-events";
    public static final String TABLE_EVENTS = "business.table-events";

    private KafkaTopics() {}
}
