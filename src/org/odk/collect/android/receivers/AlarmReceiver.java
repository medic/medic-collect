package org.odk.collect.android.receivers;

import org.odk.collect.android.services.AlarmService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service1 = new Intent(context, AlarmService.class);
	        context.startService(service1);
	}
}