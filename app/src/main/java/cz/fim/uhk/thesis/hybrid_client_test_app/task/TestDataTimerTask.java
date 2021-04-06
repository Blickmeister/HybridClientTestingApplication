package cz.fim.uhk.thesis.hybrid_client_test_app.task;

import android.util.Log;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import cz.fim.uhk.thesis.hybrid_client_test_app.api.IsCentralServerApi;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.provider.CurrentTimeProvider;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TestDataTimerTask extends TimerTask {

    // globální a pomocně proměnné
    private IsCentralServerApi isCentralServerApi;
    // kolekce ArrayDeque dle dokumentace rychlejší než LinkedList pro strukturu fronty
    private ArrayDeque<Long> measurements;
    private int stamp;
    private String alertMessage;
    private int responseCode;
    private String testDataFromServer;
    private Timestamp timestampFromClientToSend;
    private Timestamp timestampFromServerAfterReceive;
    private Timestamp timestampFromServerToSend;
    private Timestamp timestampClientAfterReceive;

    // konstanty
    // povolené znaky v náhodně generovaném řetězci
    private static final String ALLOWED_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final String TAG = "MainAct/TestDataTask";
    // TODO zkusit odecist offset nebo neco kdyz je vic serveru tak server vraci jinakci cas nez klient
    //private static final String[] TIME_SERVERS = {"ntp.nic.cz", "tik.cesnet.cz", "lxn.ujf.cas.cz", "ntpm.fit.vutbr.cz"};
    private static final String TIME_SERVER = "ntp.nic.cz";

    public TestDataTimerTask(IsCentralServerApi isCentralServerApi) {
        this.isCentralServerApi = isCentralServerApi;
        measurements = new ArrayDeque<>();
    }

    // metoda pro generování náhodného řetězce dané velikosti
    private String getRandomString(int size) {
        Random random = new Random();
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < size; ++i) {
            builder.append(ALLOWED_CHARS.charAt(random.nextInt(ALLOWED_CHARS.length())));
        }
        return builder.toString();
    }

    // metoda pro získání aktuálního času z NTP serveru (využito Apache Commons Net API)
   /* private Timestamp getCurrentTime() {
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
    }*/

    // metoda pro zaslání a získání dat
    private void sendAndReceiveData() {
        String postedData = getRandomString(10); // vygenerování náhodného řetězce

        // získání aktuálního času z NTP serveru (pro synchronizaci se serverem)
        final CurrentTimeProvider currentTimeProvider = new CurrentTimeProvider();
        currentTimeProvider.execute();
        try {
            timestampFromClientToSend = currentTimeProvider.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        //Log.d(TAG, "TIMESTAMP FROM CLIENT TO SEND: " + timestampFromClientToSend);
        // přidání časového razítka
        postedData += "/" + timestampFromClientToSend.toString();

        // vytvoření objektu requestBody pro odeslání dat na server ve správném formátu
        RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), postedData);
        // request na server
        Call<ResponseBody> call = isCentralServerApi.makeTest(requestBody);

        // zpracování response ze serveru
        call.enqueue(new Callback<ResponseBody>() {
            // pokud dostaneme response (nemusí být úspěšný)
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // kontrola zda response je neúspěšný
                if (!response.isSuccessful()) {
                    // zobrazíme chybový HTTP kód a návrat z metody
                    alertMessage = "HTTP kód: " + response.code();
                    return;
                }
                // uložení response ze serveru
                try {
                    testDataFromServer = response.body().string();
                    // uložení časového razítka ze serveru
                    String[] parts = testDataFromServer.split("/");
                    String stringTimestampAR = parts[2];
                    timestampFromServerAfterReceive = Timestamp.valueOf(stringTimestampAR);
                    //Log.d(TAG, "TIMESTAMP FROM SERVER AFTER RECEIVE: " + timestampFromServer.toString());
                    String stringTimestampTS = parts[3];
                    timestampFromServerToSend = Timestamp.valueOf(stringTimestampTS);
                    // aktuální časové razítko pro výpočet download
                    // je třeba odečíst čas potřebný pro zpracování metody getCurrentTime()
                    // chceme získat skutečný čas při obdržení datagramu ze serveru
                    long startTime = System.currentTimeMillis();
                    // TODO problem getCurrentTime() -> android.os.NetworkOnMainThreadException
                    Timestamp timestampClientAfterReceiveTmp = null;
                    CurrentTimeProvider currentTimeProvider = new CurrentTimeProvider();
                    currentTimeProvider.execute();
                    try {
                        timestampClientAfterReceiveTmp = currentTimeProvider.get();
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    long milis = timestampClientAfterReceiveTmp.getTime();
                    timestampClientAfterReceive = new Timestamp(milis - duration);
                    //Log.d(TAG, "TIMESTAMP CLIENT AFTER RECEIVE: " + timestampClientAfterReceive);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                responseCode = response.code(); // response code pro debugging
            }

            // pokud při spojení či zpracování požadavku došlo k chybě
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                alertMessage = t.getMessage();
            }
        });
    }

    // metoda pro výpočet aktuální hodnoty měření datové propustnosti
    private long computeConnectionValue() {
        // výpočet upload v milisekundách
        long upload = Math.abs(timestampFromClientToSend.getTime() - timestampFromServerAfterReceive.getTime());
        System.out.println("upload: " + upload);
        // výpočet download v milisekundách
        long download = Math.abs((timestampFromServerToSend.getTime() - timestampClientAfterReceive.getTime()));
        System.out.println("download: " + download);
        // součet hodnot pro uložení do fronty
        return upload + download;
    }

    // metoda pro výpočet průměru hodnot měření ve frontě
    private long countMean() {
        long sum = 0;
        ArrayDeque<Long> tmp = new ArrayDeque<Long>(measurements);
        while (!tmp.isEmpty()) {
            sum += tmp.poll();
        }
        return sum / measurements.size();
    }

    // metoda pro provedení měření
    public void dataThroughputMeasurement() {
        long mean;
        // odeslána testovací data s časovým razítkem klienta a získana data ze serveru s časovým
        // razítkem serveru
        sendAndReceiveData();
        // kontrola zda máme jsme získali časové razítko ze serveru
        // jinak nemá cenu provádět měření
        if (timestampFromServerAfterReceive != null && timestampFromServerToSend != null) {
            // výpočet download a upload aktuálního měření a jejich součet
            long actualConnectionValue = computeConnectionValue();
            // kontrola zda je fronta prázdná
            if (measurements.isEmpty()) {
                stamp = 1; // známka nastavena na 1
            } else {
                // alespoň 1 hodnota -> spočte se průměr
                mean = countMean();
                // porovnání průměru a aktuální hodnoty měření
                if (actualConnectionValue > mean) {
                    stamp++; // je-li akt. hodnota větší než průměr -> inkrementace známky
                } else {
                    if (stamp > 1) stamp--; // jinak je-li známka větší než 1 tk dekrementace známky
                }
            }
            // je-li ve frontě více než 5 měření
            if (measurements.size() >= 5) {
                measurements.remove(); // odstraní první hodnotu ve frontě
            }
            // přidání aktuální hodnoty na konec fronty
            measurements.add(actualConnectionValue);
            Log.d(TAG, "časy: " + timestampFromClientToSend + " "
                    + timestampFromServerAfterReceive + " " + timestampFromServerToSend + " "
            + timestampClientAfterReceive);
        }
    }

    public int getStamp() {
        return stamp;
    }

    // TODO pokud zustane jen jako test tak do haje i v mainAct a udelat Log.d tady
    public String getAlertMessage() {
        return alertMessage;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getTestDataFromServer() {
        return testDataFromServer;
    }

    @Override
    public void run() {
        dataThroughputMeasurement();
    }
}
