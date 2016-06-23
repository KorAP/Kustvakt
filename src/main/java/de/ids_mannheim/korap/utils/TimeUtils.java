package de.ids_mannheim.korap.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author hanl
 *         <p/>
 *         calculates current, expiration and inactive time for
 *         security
 *         purposes.
 * @return
 */
public class TimeUtils {

    private static DecimalFormat df = new DecimalFormat("#.#############");
    private static DateTimeFormatter dtf = DateTimeFormat
            .forPattern("dd/MM/yyyy");
    private static final DateTimeZone dtz = DateTimeZone.forID("Europe/Berlin");
    private static Logger jlog = LoggerFactory.getLogger(TimeUtils.class);


    public static int convertTimeToSeconds (String expirationVal) {
        expirationVal = expirationVal.trim();
        int finIndex = expirationVal.length() - 1;
        char entity = expirationVal.charAt(finIndex);
        int returnSec = Integer.valueOf(expirationVal.substring(0, finIndex));
        jlog.debug("setting time value to {} with time in {}", returnSec,
                entity);
        switch (entity) {
            case 'D':
                return returnSec * 60 * 60 * 24;
            case 'H':
                return returnSec * 60 * 60;
            case 'M':
                return returnSec * 60;
            case 'S':
                return returnSec;
            default:
                jlog.debug("no time unit specified. Trying to read from default (minutes)");
                return Integer.valueOf(expirationVal) * 60;
        }

    }

    public static DateTime getNow () {
        return DateTime.now(dtz);
    }

    public static DateTime getTime(String time) {
        return DateTime.parse(time).withZone(dtz);
    }

    public static DateTime getTime(long time) {
        return new DateTime(time).withZone(dtz);
    }

    //returns difference in milliseconds
    public static long calcDiff (DateTime now, DateTime future) {
        long diff = (future.withZone(dtz).getMillis() - now.withZone(dtz)
                .getMillis());
        return diff;
    }


    public static boolean isExpired(long time) {
        return getNow().isAfter(time);

    }


    // returns difference in seconds in floating number
    public static float floating (DateTime past, DateTime now) {
        long diff = (now.withZone(dtz).getMillis() - past.withZone(dtz)
                .getMillis());
        double fin = diff / 1000.0;
        BigDecimal bd = new BigDecimal(fin).setScale(8, RoundingMode.HALF_EVEN);
        return bd.floatValue();
    }


    public static DateTime fromCosmas (String date) {
        int idx = date.length();
        try {
            Integer sec = Integer.valueOf(date.substring((idx = idx - 2),
                    date.length()).trim());
            Integer min = Integer.valueOf(date.substring((idx = idx - 2),
                    idx + 2).trim());
            Integer hours = Integer.valueOf(date.substring((idx = idx - 2),
                    idx + 2).trim());
            Integer day = Integer.valueOf(date.substring((idx = idx - 2),
                    idx + 2).trim());
            Integer month = Integer.valueOf(date.substring((idx = idx - 2),
                    idx + 2).trim());
            Integer year = Integer.valueOf(date.substring((idx = idx - 4),
                    idx + 4).trim());
            return new DateTime(year, month, day, hours, min, sec);
        }
        catch (NumberFormatException e) {
            return getNow().toDateTime();
        }
    }


    public static String formatDiff (DateTime now, DateTime after) {
        return df.format(calcDiff(now, after));
    }


    /**
     * converts time to the ISO8601 standard.
     * 
     * @param time
     * @return
     */
    public static String format (DateTime time) {
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return fmt.withZone(dtz).print(time);
    }


    public static String format (long time) {
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return fmt.withZone(dtz).print(time);
    }


    /**
     * calculate expiration time
     * 
     * @param creation
     * @param plus
     *            time in seconds
     * @return
     */
    public static DateTime plusSeconds (long creation, int plus) {
        return new DateTime(creation).withZone(dtz).plusSeconds(plus);
    }


    public static DateTime getExpiration (long now, int exp) {
        return new DateTime(now).withZone(dtz).plusSeconds(exp);
    }


    /**
     * @param plus
     * @return
     */
    public static DateTime plusSeconds (int plus) {
        return getNow().withZone(dtz).plusSeconds(plus);
    }


    public static DateTime plusHours (int hours) {
        return getNow().withZone(dtz).plusHours(hours);
    }


    public static DateTime plusMinutes (int minutes) {
        return getNow().withZone(dtz).plusMinutes(minutes);
    }


    /**
     * create time stamp from long value
     * 
     * @param t
     *            time
     * @return Timestamp
     */
    public static LocalDate getTimeStamp (long t) {
        return new DateTime(t).withZone(dtz).toLocalDate();
    }


    public static DateTime getDate (int day, int month, int year) {
        DateTime date = new DateTime().withZone(dtz);
        return date.withDate(year, month, day);
    }


    public static String toString (long val, Locale locale) {
        if (locale == Locale.GERMAN)
            return new DateTime(val).toString("dd. MMMM yyyy, HH:mm",
                    Locale.GERMAN);
        else
            return new DateTime(val).toString("MM-dd-yyyy, hh:mm",
                    Locale.ENGLISH);

    }


    public static String dateToString (long val, int i) {
        switch (i) {
            case 1:
                return new DateTime(val).toString("yyyy-MM");
            case 2:
                return new DateTime(val).toString("yyyy-MM-dd");
            default:
                return new DateTime(val).toString("yyyy");
        }
    }

    private static final List<DateTime> times = new ArrayList<>();

}
