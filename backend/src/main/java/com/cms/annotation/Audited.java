package com.cms.annotation;

import com.cms.entity.AuditCategory;
import com.cms.entity.AuditEventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    AuditEventType event();
    AuditCategory category();
    String resourceType() default "";
}
