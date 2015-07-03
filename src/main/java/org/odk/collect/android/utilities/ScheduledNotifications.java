package org.odk.collect.android.utilities;

import it.sauronsoftware.cron4j.Predictor;
import it.sauronsoftware.cron4j.SchedulingPattern;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.receivers.AlarmReceiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ScheduledNotifications {

	private AlarmManager mAlarmManager;
	
    public boolean isNotificationToggleOn() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
    	return settings.getBoolean(PreferencesActivity.KEY_NOTIFICATION_TOGGLE, false);
    }

    public boolean isValidSchedule(String pattern) {
    	return SchedulingPattern.validate(pattern);
    }

    /* Will set alarm if the notifications toggle in preferences is set to true.
     * Returns true iff a new Notification is set 	*/
    public boolean initialize() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
    	String pattern = settings.getString(PreferencesActivity.KEY_NOTIFICATION_SCHEDULE, "");

    	if (isNotificationToggleOn()) {
    		return set(pattern);
    	}
    	return false;
    }
            
    public boolean set(String pattern) {
    	try {
	    	Predictor cron = new Predictor(pattern);
	    	Date alarmDate = cron.nextMatchingDate();
	    	set(alarmDate);
	    	return true;
    	}
    	catch (Exception e) {
    		Log.e("Collect", "Failed to set Notification with schedule `"+ pattern + "`", e);
    		return false;
    	}
    }

    public void set(Date alarmDate) {
    	mAlarmManager = (AlarmManager) Collect.getInstance().getSystemService(Context.ALARM_SERVICE);
    	Intent alarmIntent = new Intent(Collect.getInstance(), AlarmReceiver.class);
    	PendingIntent pendingIntent = PendingIntent.getBroadcast( Collect.getInstance().getApplicationContext(), 0, alarmIntent, 0);

		mAlarmManager.set(AlarmManager.RTC, alarmDate.getTime(), pendingIntent);

		SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a");
		Log.i("ScheduledNotification", "Notification set for " + dateFormat.format(alarmDate.getTime()));
    }
    
    public void cancel() {
    	mAlarmManager = (AlarmManager) Collect.getInstance().getSystemService(Context.ALARM_SERVICE);
    	Intent alarmIntent = new Intent(Collect.getInstance(), AlarmReceiver.class);
    	PendingIntent pendingIntent = PendingIntent.getBroadcast( Collect.getInstance().getApplicationContext(), 0, alarmIntent, 0);

		mAlarmManager.cancel(pendingIntent);

		Log.i("ScheduledNotification", "Notification cancelled");
    }
}
