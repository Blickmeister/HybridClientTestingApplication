package cz.fim.uhk.thesis.hybrid_client_test_app.helper.provider;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Timestamp;

public class CurrentTimeProvider extends AsyncTask<Void, Void, Timestamp> {

    private static final String TIME_SERVER = "ntp.nic.cz";
    private static final String TAG = "CurrentTypeProvider";

    public CurrentTimeProvider() {}

    @Override
    protected Timestamp doInBackground(Void... voids) {
        return getCurrentTime();
    }

    // metoda pro získání aktuálního času z NTP serveru (využito Apache Commons Net API)
    private Timestamp getCurrentTime() {
        NTPUDPClient timeClient = new NTPUDPClient();
        timeClient.setDefaultTimeout(4000); // pokud se nepodaří připojit po limitu se přestane snažit
        InetAddress inetAddress;
        TimeInfo timeInfo = null;
        try {
            timeClient.open(); // otevřít socket pro komunikaci
            // pokus o připojení na server (k dispozici více serverů v případě selhání)
            //for (String timeServer : TIME_SERVERS) {
            for (int i = 0; i < 4; i++) {
                try {
                    //inetAddress = InetAddress.getByName(timeServer); // získání NTP serveru
                    inetAddress = InetAddress.getByName(TIME_SERVER); // získání NTP serveru
                    timeInfo = timeClient.getTime(inetAddress); // získání času ze serveru
                } catch (UnknownHostException uhe) {
                    Log.d(TAG, "Adresa NTP serveru je neznámá: " + uhe);
                } catch (IOException ioe) {
                    Log.d(TAG, "Nepodařilo se získat čas ze serveru: " + ioe);
                }
                if (timeInfo != null) {
                    // získáný čas ze serveru
                    return new Timestamp(timeInfo.getMessage().getTransmitTimeStamp().getTime());
                }
            }
        } catch (SocketException se) {
            Log.d(TAG,"Nepodařilo se socket pro komunikaci s NTP serverem: " + se);
        }

        timeClient.close(); // zavřít socket a ukončit komunikaci
        // pokud se nepodaří získat čas ze serveru je použit systémový čas zařízení
        return new Timestamp(System.currentTimeMillis());
    }
}
