package org.odk.collect.android.services;
 
import org.odk.collect.android.R;
import org.odk.collect.android.activities.MainMenuActivity;
import org.odk.collect.android.activities.SplashScreenActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.PreferencesActivity;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
                            
public class AlarmService extends IntentService 
{
      
   private static final int NOTIFICATION_ID = 1;
   private static final String TAG = "AlarmService";
   private NotificationManager notificationManager;
   private PendingIntent pendingIntent;
   
 
   public AlarmService() {
	      super("AlarmService");
	  }
   
   
   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
       return super.onStartCommand(intent,flags,startId);
   }
   
   @Override
   protected void onHandleIntent(Intent intent) {
       Context context = this.getApplicationContext();
       notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent mIntent = new Intent(this, SplashScreenActivity.class);
        
        /*
        Bundle bundle = new Bundle(); 
        bundle.putString("test", "medic");
        mIntent.putExtras(bundle);
        */
        
        /* // Bundle can be obtained in the onCreate or onNewIntent of SplashScreenActivity
         * if (getIntent().getExtras() != null)
         * {
         * 	 Log.i(TAG, "Started with " + getIntent().getExtras().getString("test"));
         * }
        */
        
		pendingIntent = PendingIntent.getActivity(context, 0, mIntent, PendingIntent.FLAG_CANCEL_CURRENT);     
		
		Resources res = this.getResources();
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
    	String title = settings.getString(PreferencesActivity.KEY_NOTIFICATION_TITLE, res.getString(R.string.default_notification_title));
    	String content = settings.getString(PreferencesActivity.KEY_NOTIFICATION_CONTENT, res.getString(R.string.default_notification_content));
    	
		builder.setContentIntent(pendingIntent)
		            .setSmallIcon(R.drawable.ic_notification)
		            .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_notification))
		            .setTicker(title + ": " + content)
		            .setAutoCancel(true)
		            .setContentTitle(title)
		            .setContentText(content);

		notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, builder.build());
		Log.d(TAG,"Created notification: `" + title + ": " + content +"`");
    }
}
