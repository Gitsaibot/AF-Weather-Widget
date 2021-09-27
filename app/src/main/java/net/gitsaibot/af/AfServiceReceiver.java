package net.gitsaibot.af;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AfServiceReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		AfService.enqueueWork(context, intent);
	}

}
