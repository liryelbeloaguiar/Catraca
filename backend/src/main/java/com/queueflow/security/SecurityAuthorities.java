package com.queueflow.security;

/** Authority names used by JWT claims and programmatic authorization checks. */
public final class SecurityAuthorities {
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String ROLE_DEV_ADMIN = ROLE_PREFIX + "DEV_ADMIN";
    public static final String ROLE_ADMIN_USER = ROLE_PREFIX + "ADMIN_USER";
    public static final String ROLE_PATIENT = ROLE_PREFIX + "PATIENT";
    public static final String ADMINISTRATION_MANAGE = "ADMINISTRATION_MANAGE";
    public static final String APPOINTMENT_MANAGE = "APPOINTMENT_MANAGE";
    public static final String APPOINTMENT_READ = "APPOINTMENT_READ";
    public static final String EMPLOYEE_MANAGE = "EMPLOYEE_MANAGE";
    public static final String QUEUE_MANAGE = "QUEUE_MANAGE";
    public static final String QUEUE_READ = "QUEUE_READ";

    private SecurityAuthorities() {}
}
