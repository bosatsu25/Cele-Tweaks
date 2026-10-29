package io.github.bosatsu25.celetweaks.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Optional bounds for a numeric field of a feature config section; enforced by the settings screen. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {
    double min();
    double max();
}
