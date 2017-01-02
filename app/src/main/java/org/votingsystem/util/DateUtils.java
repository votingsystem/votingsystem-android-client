package org.votingsystem.util;

import android.content.Context;

import org.votingsystem.android.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DateUtils {

    private static final int SECOND = 1000;
    private static final int MINUTE = 60 * SECOND;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;

    //https://developer.android.com/reference/java/text/SimpleDateFormat.html
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ZZZZ");
    private static final DateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZZ");
    private static final DateFormat xmlDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ");

    private static final DateFormat isoDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    static {
        isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String getTimeAgo(long time, Context ctx) {
        // TODO: use DateUtils methods instead
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }
        long now = Calendar.getInstance().getTimeInMillis();
        if (time > now || time <= 0) {
            return null;
        }

        final long diff = now - time;
        if (diff < MINUTE) {
            return "just now";
        } else if (diff < 2 * MINUTE) {
            return "a minute ago";
        } else if (diff < 50 * MINUTE) {
            return diff / MINUTE + " minutes ago";
        } else if (diff < 90 * MINUTE) {
            return "an hour ago";
        } else if (diff < 24 * HOUR) {
            return diff / HOUR + " hours ago";
        } else if (diff < 48 * HOUR) {
            return "yesterday";
        } else {
            return diff / DAY + " days ago";
        }
    }

    private static final SimpleDateFormat[] ACCEPTED_TIMESTAMP_FORMATS = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
            new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z", Locale.US)
    };


    public static String formatShortDate(Context context, Date date) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        return android.text.format.DateUtils.formatDateRange(context, formatter, date.getTime(), date.getTime(),
                android.text.format.DateUtils.FORMAT_ABBREV_ALL | android.text.format.DateUtils.FORMAT_NO_YEAR,
                PrefUtils.getDisplayTimeZone().getID()).toString();
    }

    public static String formatShortTime(Context context, Date time) {
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
        TimeZone tz = PrefUtils.getDisplayTimeZone();
        if (tz != null) {
            format.setTimeZone(tz);
        }
        return format.format(time);
    }

    /**
     * Returns "Today", "Tomorrow", "Yesterday", or a short date format.
     */
    public static String formatHumanFriendlyShortDate(final Context context, long timestamp) {
        long localTimestamp, localTime;
        long now = Calendar.getInstance().getTimeInMillis();

        TimeZone tz = PrefUtils.getDisplayTimeZone();
        localTimestamp = timestamp + tz.getOffset(timestamp);
        localTime = now + tz.getOffset(now);

        long dayOrd = localTimestamp / 86400000L;
        long nowOrd = localTime / 86400000L;

        if (dayOrd == nowOrd) {
            return context.getString(R.string.day_title_today);
        } else if (dayOrd == nowOrd - 1) {
            return context.getString(R.string.day_title_yesterday);
        } else if (dayOrd == nowOrd + 1) {
            return context.getString(R.string.day_title_tomorrow);
        } else {
            return formatShortDate(context, new Date(timestamp));
        }
    }

    public static boolean inRange(Date initDate, Date lapsedDate, long timeRange) {
        return initDate.getTime() - lapsedDate.getTime() < timeRange;
    }

    public static int getDayOfMonthFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int getMonthFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH);
    }

    public static int getYearFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    public static Date getDate(String dateStr) {
        try {
            if (dateStr.endsWith("Z")) return isoDateFormat.parse(dateStr);
            else return dateFormat.parse(dateStr);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Date getXmlDate(String dateStr) {
        try {
            return xmlDateFormat.parse(dateStr);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String getXmlDateStr(Date date) {
        try {
            return xmlDateFormat.format(date).replace("GMT", "");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String getUTCDateStr(Date date) {
        try {
            return shortDateFormat.format(date);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Date getDate(String dateStr, String format) throws ParseException {
        try {
            DateFormat formatter = new SimpleDateFormat(format);
            return formatter.parse(dateStr);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String getISODateStr(Date date) {
        return isoDateFormat.format(date);
    }

    public static String getDateStr(Date date) {
        return dateFormat.format(date);
    }

    public static String getDateStr(Date date, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static String getElapsedTimeHoursMinutesFromMilliseconds(long milliseconds) {
        String format = String.format("%%0%dd", 2);
        long elapsedTime = milliseconds / 1000;
        String minutes = String.format(format, (elapsedTime % 3600) / 60);
        String hours = String.format(format, elapsedTime / 3600);
        String time = hours + ":" + minutes;
        return time;
    }

    public static String getElapsedTimeHoursMinutesSecondsFromMilliseconds(long milliseconds) {
        String format = String.format("%%0%dd", 2);
        long elapsedTime = milliseconds / 1000;
        String seconds = String.format(format, elapsedTime % 60);
        String minutes = String.format(format, (elapsedTime % 3600) / 60);
        String hours = String.format(format, elapsedTime / 3600);
        String time = hours + ":" + minutes + ":" + seconds;
        return time;
    }

    public static Calendar addDays(int numDias) {
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, numDias);
        return today;
    }

    public static Calendar addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal;
    }

    public static String getDayHourElapsedTime(Date date1, Date date2, Context context) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return getDayHourElapsedTime(cal1, cal2, context);
    }

    public static String getDayWeekDateStr(Date date, String hourFormat) {
        if (hourFormat == null) hourFormat = "HH:mm";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if (Calendar.getInstance().get(Calendar.YEAR) != calendar.get(Calendar.YEAR))
            return getDateStr(date, "dd MMM yyyy' '" + hourFormat);
        else return getDateStr(date, "EEE dd MMM' '" + hourFormat);
    }

    public static String getDayWeekDateSimpleStr(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if (Calendar.getInstance().get(Calendar.YEAR) != calendar.get(Calendar.YEAR))
            return getDateStr(date, "dd MMM yyyy");
        else return getDateStr(date, "EEE dd MMM");
    }

    public static Date getDayWeekDate(String dateStr) throws ParseException {
        try {
            return getDate(dateStr, "dd MMM yyyy' 'HH:mm");
        } catch (Exception ex) {
            Calendar resultCalendar = Calendar.getInstance();
            resultCalendar.setTime(getDate(dateStr, "EEE dd MMM' 'HH:mm"));
            resultCalendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
            return resultCalendar.getTime();
        }
    }

    public static String getElapsedTimeStr(Date end) {
        Float hours = (end.getTime() - Calendar.getInstance().getTime().getTime()) / (60 * 60 * 1000F);
        return Integer.valueOf(hours.intValue()).toString();
    }

    public static String getYearDayHourMinuteSecondElapsedTime(Calendar cal1, Calendar cal2,
                                                               Context context) {
        long l1 = cal1.getTimeInMillis();
        long l2 = cal2.getTimeInMillis();
        long diff = l2 - l1;

        long secondInMillis = 1000;
        long minuteInMillis = secondInMillis * 60;
        long hourInMillis = minuteInMillis * 60;
        long dayInMillis = hourInMillis * 24;
        long yearInMillis = dayInMillis * 365;

        long elapsedYears = diff / yearInMillis;
        diff = diff % yearInMillis;
        long elapsedDays = diff / dayInMillis;
        diff = diff % dayInMillis;
        long elapsedHours = diff / hourInMillis;
        diff = diff % hourInMillis;
        long elapsedMinutes = diff / minuteInMillis;
        diff = diff % minuteInMillis;
        long elapsedSeconds = diff / secondInMillis;

        StringBuilder result = new StringBuilder();
        if (elapsedYears > 0) result.append(elapsedYears + ", "
                + context.getString(R.string.years_lbl));
        if (elapsedDays > 0) result.append(elapsedDays + ", "
                + context.getString(R.string.days_lbl));
        if (elapsedHours > 0) result.append(elapsedHours + ", "
                + context.getString(R.string.hours_lbl));
        if (elapsedMinutes > 0) result.append(elapsedMinutes + ", "
                + context.getString(R.string.minutes_lbl));
        if (elapsedSeconds > 0) result.append(elapsedSeconds + ", "
                + context.getString(R.string.seconds_lbl));
        return result.toString();
    }

    public static String getYearDayHourMinuteSecondElapsedTime(Date date1, Date date2,
                                                               Context context) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return getYearDayHourMinuteSecondElapsedTime(cal1, cal2, context);
    }

    public static String getDayHourElapsedTime(Calendar cal1, Calendar cal2, Context context) {
        long l1 = cal1.getTimeInMillis();
        long l2 = cal2.getTimeInMillis();
        long diff = l2 - l1;

        long secondInMillis = 1000;
        long minuteInMillis = secondInMillis * 60;
        long hourInMillis = minuteInMillis * 60;
        long dayInMillis = hourInMillis * 24;
        long yearInMillis = dayInMillis * 365;

        long elapsedDays = diff / dayInMillis;
        diff = diff % dayInMillis;
        long elapsedHours = diff / hourInMillis;
        diff = diff % hourInMillis;

        StringBuilder result = new StringBuilder();
        if (elapsedDays > 0)
            result.append(elapsedDays + " " + context.getString(R.string.days_lbl));
        if (elapsedHours > 0)
            result.append(elapsedHours + ", " + context.getString(R.string.hours_lbl));
        return result.toString();
    }

    public static Calendar getMonday(Calendar calendar) {
        Calendar result = (Calendar) calendar.clone();
        result.add(Calendar.DAY_OF_YEAR, -7);
        result.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        result.set(Calendar.HOUR_OF_DAY, 24);
        result.set(Calendar.MINUTE, 0);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);
        return result;
    }

}