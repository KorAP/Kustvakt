package de.ids_mannheim.korap.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * @author hanl
 * @date 28/03/2014
 */

public class KustvaktLogger implements Logger {
    // debugging flag, info, warn, error will always be logged though!
    public static boolean DEBUG = false;

    public static final String ERROR_LOG = "errorLog";
    public static final String SECURITY_LOG = "securityLog";

    //fixme:
    public static final KustvaktLogger ERROR_LOGGER = KustvaktLogger
            .getLogger(ERROR_LOG);
    public static final KustvaktLogger QUERY_LOGGER = KustvaktLogger
            .getLogger("ql");

    @Deprecated
    public static final KustvaktLogger SECURITY_LOGGER = KustvaktLogger
            .getLogger("security");
    private Logger log;

    public static KustvaktLogger getLogger(Class cl) {
        KustvaktLogger l = new KustvaktLogger(LoggerFactory.getLogger(cl));
        return l;
    }

    public static KustvaktLogger getLogger(String name) {
        KustvaktLogger l = new KustvaktLogger(LoggerFactory.getLogger(name));
        return l;
    }

    private KustvaktLogger(Logger log) {
        this.log = log;
    }

    @Override
    public String getName() {
        return log.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        if (DEBUG)
            this.log.trace(s);

    }

    @Override
    public void trace(String s, Object o) {
        if (DEBUG)
            this.log.trace(s, o);
    }

    @Override
    public void trace(String s, Object o, Object o2) {
        if (DEBUG)
            this.log.trace(s, o, o2);
    }

    @Override
    public void trace(String s, Object... objects) {
        if (DEBUG)
            this.log.trace(s, objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        if (DEBUG)
            this.log.trace(s, throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        if (DEBUG)
            return this.log.isTraceEnabled();
        return false;
    }

    @Override
    public void trace(Marker marker, String s) {
        if (DEBUG)
            this.log.trace(marker, s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        if (DEBUG)
            this.log.trace(marker, s, o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o2) {
        if (DEBUG)
            this.log.trace(marker, s, o, o2);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        if (DEBUG)
            this.log.trace(marker, s, objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        if (DEBUG)
            this.log.trace(marker, s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        if (DEBUG)
            return true;
        return false;
    }

    @Override
    public void debug(String s) {
        if (DEBUG)
            this.log.debug(s);
    }

    @Override
    public void debug(String s, Object o) {
        if (DEBUG)
            this.log.debug(s, o);
    }

    @Override
    public void debug(String s, Object o, Object o2) {
        if (DEBUG)
            this.log.debug(s, o, o2);
    }

    @Override
    public void debug(String s, Object... objects) {
        if (DEBUG)
            this.log.debug(s);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        if (DEBUG)
            this.log.debug(s, throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return this.log.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        if (DEBUG)
            this.log.debug(marker, s);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        if (DEBUG)
            this.log.debug(marker, s, o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o2) {
        if (DEBUG)
            this.log.debug(marker, s, o, o2);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        if (DEBUG)
            this.log.debug(marker, s, objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        if (DEBUG)
            this.log.debug(marker, s, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return this.log.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        this.log.info(s);
    }

    @Override
    public void info(String s, Object o) {
        this.log.info(s, o);
    }

    @Override
    public void info(String s, Object o, Object o2) {
        this.log.info(s, o, o2);
    }

    @Override
    public void info(String s, Object... objects) {
        this.log.info(s, objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        this.log.info(s, throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return this.log.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        this.log.info(marker, s);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        this.log.info(marker, s, o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o2) {
        this.log.info(marker, s, o, o2);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        this.log.info(marker, s, objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        this.log.info(marker, s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return this.log.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        this.log.warn(s);
    }

    @Override
    public void warn(String s, Object o) {
        this.log.warn(s, o);
    }

    @Override
    public void warn(String s, Object... objects) {
        this.log.warn(s, objects);
    }

    @Override
    public void warn(String s, Object o, Object o2) {
        this.log.warn(s, o, o2);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        this.log.warn(s, throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return this.log.isTraceEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        this.log.warn(marker, s);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        this.log.warn(marker, s, o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o2) {
        this.log.warn(marker, s, o, o2);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        this.log.warn(marker, s, objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        this.log.warn(marker, s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return this.log.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        this.log.error(s);
    }

    @Override
    public void error(String s, Object o) {
        this.log.error(s, o);
    }

    @Override
    public void error(String s, Object o, Object o2) {
        this.log.error(s, o, o2);
    }

    @Override
    public void error(String s, Object... objects) {
        this.log.error(s, objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        this.log.error(s, throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return this.log.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        this.log.error(marker, s);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        this.log.error(marker, s, o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o2) {
        this.log.error(marker, s, o, o2);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        this.log.error(marker, s, objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        this.log.error(marker, s, throwable);
    }
}
