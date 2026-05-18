package io.lvoxx.ssurl.common.util;

import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.Marker;

import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.marker.Markers;

public final class Logs {

    private Logs() {
    }

    public static StructuredArgument kv(String key, Object value) {
        return StructuredArguments.kv(key, value);
    }

    public static StructuredArgument entries(Map<String, ?> map) {
        return StructuredArguments.entries(map);
    }

    public static Marker marker(String key, Object value) {
        return Markers.append(key, value);
    }

    public static Marker markerEntries(Map<String, ?> map) {
        return Markers.appendEntries(map);
    }

    public static Marker event(String eventId, String eventName) {
        return Markers.append(Constants.LOG_MARKER_EVENT_ID, eventId)
                .and(Markers.append(Constants.LOG_MARKER_EVENT, eventName));
    }

    public static void info(Logger log, String format, Object... args) {
        log.info(format, args);
    }

    public static void debug(Logger log, String format, Object... args) {
        log.debug(format, args);
    }

    public static void warn(Logger log, String format, Object... args) {
        log.warn(format, args);
    }

    public static void error(Logger log, String format, Object... args) {
        log.error(format, args);
    }

    public static void error(Logger log, String format, Throwable t, Object... args) {
        log.error(format, t, args);
    }

    @SafeVarargs
    public static void infoLazy(Logger log, String format, Supplier<Object>... suppliers) {
        if (log.isInfoEnabled()) {
            var args = new Object[suppliers.length];
            for (int i = 0; i < suppliers.length; i++) {
                args[i] = suppliers[i].get();
            }
            log.info(format, args);
        }
    }
}