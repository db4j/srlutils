// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.srlutils.Simple.Print;


public class Dates {



    /***************Date ADD AND DIFFERENCE FUNCTION******************/
    /**
     * Add number of days or months to the given date
     * @param token should be either "m" for months or "d" for dates
     * @param date1 given date
     * @param tokenVal number of months or days to be added
     */
    public Date dateAdd(String token, Date date1, int tokenVal) {
        Date addedDate = new Date();
        Calendar c1 = Calendar.getInstance();
        c1.setTime( date1 );
        if ( token.equalsIgnoreCase( "d" ) )
            c1.add( Calendar.DATE, tokenVal );
        else if ( token.equalsIgnoreCase( "m" ) )
            c1.add( Calendar.MONTH, tokenVal );
        addedDate = c1.getTime();
        return addedDate;
    }


    /**
     * Gets the difference between two dates
     * @param token should be either "m" for months or "d" for dates
     * @param date1 given Date date1
     * @param date2 given Date date1
     */
    public int dateDiff(String token, Date date1, Date date2) {

        int diff = 0;
        //different date might have different offset
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime( date1 );
        long ldate1 = date1.getTime() + cal1.get( Calendar.ZONE_OFFSET ) + cal1.get( Calendar.DST_OFFSET );

        if ( date2 == null )
            cal2.setTime( new Date() );
        else
            cal2.setTime( date2 );
        long ldate2 = date2.getTime() + cal2.get( Calendar.ZONE_OFFSET ) + cal2.get( Calendar.DST_OFFSET );

        // Use integer calculation, truncate the decimals
        int hr1 = (int) (ldate1 / 3600000); //60*60*1000
        int hr2 = (int) (ldate2 / 3600000);

        int days1 = (int) hr1 / 24;
        int days2 = (int) hr2 / 24;
        int dateDiff = days2 - days1;
        int yearDiff = cal2.get( Calendar.YEAR ) - cal1.get( Calendar.YEAR );
        int monthDiff = yearDiff * 12 + cal2.get( Calendar.MONTH ) - cal1.get( Calendar.MONTH );

        if ( token.equals( "d" ) )
            diff = dateDiff;
        else if ( token.equals( "m" ) )
            diff = monthDiff;

        return diff;
    }


    static long mpd = 24*3600*1000;


    /** return the Date kdays since the epoch */
    public static Date date(int kday) {
        return new Date( kday * mpd );
    }
    /** return the index of date relative to the epoch */
    public static int kday(Date date) {
        long msec = date.getTime();
        return (int) (msec / mpd);
    }

    public static void milli(int dd) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        Calendar cal = new GregorianCalendar( 1970, 0, 1 );
        cal.setTimeZone( tz );
        Date date = null;
        DateFormat fmt = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss Z" );
        fmt.setTimeZone( tz );
        for (int ii = 0; ii < 40; ii++) {
            cal.set( 1970+ii, 0, 1 );
            long milli = cal.getTimeInMillis();
            date = cal.getTime();
            String df = fmt.format( date );
            Print.prf( "%30s -- %20d\n", df, milli );
        }
    }


}

























