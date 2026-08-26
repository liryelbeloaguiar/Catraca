package com.queueflow.security;

/** Compile-time constants accepted by @PreAuthorize. */
public final class SecurityExpressions {
    public static final String DEV_ADMIN = "hasRole('DEV_ADMIN')";
    public static final String EMPLOYEE_MANAGER = "hasAuthority('EMPLOYEE_MANAGE')";
    public static final String ADMINISTRATION_MANAGER = "hasAuthority('ADMINISTRATION_MANAGE')";
    public static final String QUEUE_READER_OR_ADMIN = "hasAnyAuthority('QUEUE_READ', 'ADMINISTRATION_MANAGE')";

    private SecurityExpressions() {}
}
