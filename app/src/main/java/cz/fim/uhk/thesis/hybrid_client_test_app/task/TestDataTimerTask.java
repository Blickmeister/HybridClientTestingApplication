package cz.fim.uhk.thesis.hybrid_client_test_app.task;

import android.content.SharedPreferences;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.R;
import cz.fim.uhk.thesis.hybrid_client_test_app.api.IsCentralServerApi;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.provider.CurrentTimeProvider;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Součást komunikačního modulu
 * Požadavek zaslání testovacích dat na centrální server
 * Zároveň modul vyhodnocení datové propustnosti
 */
public class TestDataTimerTask extends TimerTask {

    // globální a pomocně proměnné
    private final IsCentralServerApi isCentralServerApi;
    // kolekce ArrayDeque dle dokumentace rychlejší než LinkedList pro strukturu fronty
    private final ArrayDeque<Long> measurements;
    private int stamp;
    private String alertMessage;
    private String testDataFromServer;
    private Timestamp timestampFromClientToSend;
    private Timestamp timestampFromServerAfterReceive;
    private Timestamp timestampFromServerToSend;
    private Timestamp timestampClientAfterReceive;
    private final MainActivity mainActivity;

    // konstanty
    // povolené znaky v náhodně generovaném řetězci
    private static final String ALLOWED_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final String TAG = "MainAct/TestDataTask";
    // TODO zkusit odecist offset nebo neco kdyz je vic serveru tak server vraci jinakci cas nez klient
    //private static final String[] TIME_SERVERS = {"ntp.nic.cz", "tik.cesnet.cz", "lxn.ujf.cas.cz", "ntpm.fit.vutbr.cz"};

    public TestDataTimerTask(IsCentralServerApi isCentralServerApi, MainActivity mainActivity) {
        this.isCentralServerApi = isCentralServerApi;
        this.mainActivity = mainActivity;
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
        // přidání časového razítka
        postedData += "/" + timestampFromClientToSend.toString();

        // vytvoření objektu requestBody pro odeslání dat na server ve správném formátu
        RequestBody requestBody = RequestBody.create(postedData, MediaType.parse("text/plain"));
        // request na server
        Call<ResponseBody> call = isCentralServerApi.makeTest(requestBody);

        // zpracování response ze serveru
        call.enqueue(new Callback<ResponseBody>() {
            // pokud dostaneme response (nemusí být úspěšný)
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                // kontrola zda response je neúspěšný
                if (!response.isSuccessful()) {
                    // zobrazíme chybový HTTP kód a návrat z metody
                    alertMessage = "HTTP kód: " + response.code();
                    return;
                }
                // uložení response ze serveru
                try {
                    if (response.body() != null) {
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
                        Timestamp timestampClientAfterReceiveTmp = null;
                        CurrentTimeProvider currentTimeProvider = new CurrentTimeProvider();
                        //currentTimeProvider.execute();
                        long startTime = System.nanoTime();
                        try {
                            timestampClientAfterReceiveTmp = currentTimeProvider.execute().get();
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        long endTime = System.nanoTime();
                        long durationInNano = endTime - startTime;
                        // převod na milisekundy
                        long duration = TimeUnit.NANOSECONDS.toMillis(durationInNano);
                        if (timestampClientAfterReceiveTmp != null) {
                            long milis = timestampClientAfterReceiveTmp.getTime();
                            //timestampClientAfterReceive = new Timestamp(milis - duration);
                            timestampClientAfterReceive = new Timestamp(milis);
                        } else {
                            Log.e(TAG, "Nepodařilo se získat aktuální čas po obdržení datagramu ze serveru");
                        }
                        // fáze vyhodnocení
                        evaluation();
                    } else {
                        Log.e(TAG, "Response body je null");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // pokud při spojení či zpracování požadavku došlo k chybě
            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                alertMessage = t.getMessage();
                Log.e(TAG, "Nepodařilo se připojit k serveru: " + alertMessage);
                if (t instanceof SocketTimeoutException || t instanceof ConnectException) {
                    // pokud se nepodaří připojit k serveru -> zvednutí známky
                    stamp += 1;
                }
            }
        });
    }

    // metoda pro výpočet aktuální hodnoty měření datové propustnosti
    private long computeConnectionValue() {
        // výpočet upload v milisekundách
        long upload = Math.abs(timestampFromClientToSend.getTime() - timestampFromServerAfterReceive.getTime());
        Log.d(TAG, "upload: " + upload + " millis");
        // výpočet download v milisekundách
        long download = Math.abs((timestampFromServerToSend.getTime() - timestampClientAfterReceive.getTime()));
        Log.d(TAG, "download: " + download + " millis");
        // součet hodnot pro uložení do fronty
        return upload + download;
    }

    // metoda pro výpočet průměru hodnot měření ve frontě
    private long countMean() {
        long sum = 0;
        ArrayDeque<Long> tmp = new ArrayDeque<>(measurements);
        while (!tmp.isEmpty()) {
            sum += tmp.poll();
        }
        return sum / measurements.size();
    }

    // metoda pro vyhodnocení datové propustnosti
    private void evaluation() {
        long mean;
        // kontrola zda jsme získali časové razítko ze serveru
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
        Log.d(TAG, "Známka vyhodnocení datové propustnosti: " + stamp);
        // změna kontextu klienta pokud známka vyhodnocení datové propustnosti přesáhne 20
        if (stamp >= 20) {
            // vytažení shared preferences
            SharedPreferences sharedPref = mainActivity.getSharedPref();
            SharedPreferences.Editor editor = sharedPref.edit();
            // podle možného budoucího stavu klienta a stavu aktuálního
            if (mainActivity.getCurrentUser().getFutureState().contains("offline") &&
                    sharedPref.getInt(mainActivity.getString(R.string.sh_pref_app_state), 0)
                            != MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE) {
                // změna na klienta v klienta v roli offline režimu
                editor.putInt(mainActivity.getString(R.string.sh_pref_app_state),
                        MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE);
            } else if (mainActivity.getCurrentUser().getFutureState().contains("p2pClient") &&
                    sharedPref.getInt(mainActivity.getString(R.string.sh_pref_app_state), 0)
                            != MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE) {
                // změna na klienta na klienta v roli P2P klienta
                editor.putInt(mainActivity.getString(R.string.sh_pref_app_state),
                        MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE);
            }
            editor.apply();
            // refresh Main Activity - zavedení nového kontextu a rekonfigurace aplikace (klienta)
            mainActivity.recreate();
        }
    }

    // metoda pro provedení měření
    public void dataThroughputMeasurement() {
        // odeslána testovací data s časovým razítkem klienta a získana data ze serveru s časovým
        // razítkem serveru -> zahájení celého procesu vyhodnovení
        sendAndReceiveData();
    }

    @Override
    public void run() {
        dataThroughputMeasurement();
    }
}
