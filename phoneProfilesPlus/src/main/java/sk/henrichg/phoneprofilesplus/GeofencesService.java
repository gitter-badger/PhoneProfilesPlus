package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class GeofencesService extends WakefulIntentService {

    public GeofencesService() {
        super("GeofencesService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        if (intent != null) {
            PPApplication.logE("##### GeofencesService.doWakefulWork", "xxx");

            Context appContext = getApplicationContext();

            if (!PPApplication.getApplicationStarted(appContext, true))
                // application is not started
                return;

            if (Event.getGlobalEventsRuning(appContext))
            {
                PPApplication.logE("@@@ GeofencesService.doWakefulWork", "xxx");

                if ((PhoneProfilesService.instance != null) && PhoneProfilesService.isGeofenceScannerStarted())
                    PhoneProfilesService.geofencesScanner.updateGeofencesInDB();

                // start service
                Intent eventsServiceIntent = new Intent(appContext, EventsService.class);
                eventsServiceIntent.putExtra(EventsService.EXTRA_BROADCAST_RECEIVER_TYPE, EventsService.SENSOR_TYPE_GEOFENCES_SCANNER);
                WakefulIntentService.sendWakefulWork(appContext, eventsServiceIntent);
            }
        }
    }

}