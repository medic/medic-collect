package org.odk.collect.android.services;
 
import org.odk.collect.android.R;
import org.odk.collect.android.activities.MainMenuActivity;
import org.odk.collect.android.activities.SplashScreenActivity;
import org.odk.collect.android.application.Collect;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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
	   Log.i(TAG,"Alarm Service has started.");
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

		builder.setContentIntent(pendingIntent)
		            .setSmallIcon(R.drawable.ic_medic_mobile)
		            .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_medic_mobile))
		            .setTicker(res.getString(R.string.notification_ticker))
		            .setAutoCancel(true)
		            .setContentTitle(res.getString(R.string.notification_title))
		            .setContentText(res.getString(R.string.notification_subject));

		notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, builder.build());
		Log.i(TAG,"Notifications sent.");

    }
 
}