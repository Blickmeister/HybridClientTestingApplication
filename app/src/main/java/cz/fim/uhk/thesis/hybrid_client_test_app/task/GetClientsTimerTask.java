package cz.fim.uhk.thesis.hybrid_client_test_app.task;

import android.content.SharedPreferences;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.TimerTask;

import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.R;
import cz.fim.uhk.thesis.hybrid_client_test_app.api.IsCentralServerApi;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Součást komunikačního modulu
 * Požadavek získání aktuálního seznamu klientů z centrálního serveru
 */
public class GetClientsTimerTask extends TimerTask {

    private List<User> clientsFromServer;
    private final IsCentralServerApi isCentralServerApi;
    private String alertMessage;
    private final MainActivity mainActivity;

    private static final String TAG = "GetClientsTimerTask";

    public GetClientsTimerTask(IsCentralServerApi isCentralServerApi, MainActivity mainActivity) {
        this.isCentralServerApi = isCentralServerApi;
        this.mainActivity = mainActivity;
    }

    @Override
    public void run() {
        getClientListFromServer();
        printAndroidLabel();
    }

    // metoda pro získání seznamu klientů ze serveru
    private void getClientListFromServer() {
        // request na server
        Call<List<User>> call = isCentralServerApi.getUsers();

        // zpracování response ze serveru
        // metoda enqueue zajistí, aby zpracovaní proběhlo na nově vytvořeném background vlákně
        call.enqueue(new Callback<List<User>>() {
            // pokud dostaneme response (nemusí být úspěšný)
            @Override
            public void onResponse(@NotNull Call<List<User>> call, @NotNull Response<List<User>> response) {
                // kontrola zda response je neúspěšný
                if (!response.isSuccessful()) {
                    // zobrazíme chybový HTTP kód a návrat z metody
                    alertMessage = "HTTP kód: " + response.code();
                    return;
                }

                // uložení dat
                clientsFromServer = response.body();
                mainActivity.setClients(clientsFromServer); // seznam klientů

                // nastavení časů připojení k serveru akutálnímu klientovi
                for (User client : mainActivity.getClients()) {
                    SharedPreferences sharedPref = mainActivity.getSharedPref();
                    String currentClientId = sharedPref.getString(mainActivity.getString(R.string.sh_pref_ssid),
                            null);
                    if (client.getSsid().equals(currentClientId)) {
                        mainActivity.getCurrentUser().setFirstConnectionToServer(client.getFirstConnectionToServer());
                        mainActivity.getCurrentUser().setLastConnectionToServer(client.getLastConnectionToServer());
                        // délka ID v případě nezískání IMEI u klienta
                        int LENGTH_OF_EMPTY_ID = 2;
                        // nastavení ID ze serveru
                        if (mainActivity.getCurrentUser().getSsid().length() <= LENGTH_OF_EMPTY_ID) {
                            mainActivity.getCurrentUser().setSsid(client.getSsid());
                        }
                    }
                }
            }

            // pokud při spojení či zpracování požadavku došlo k chybě
            @Override
            public void onFailure(@NotNull Call<List<User>> call, @NotNull Throwable t) {
                alertMessage = t.getMessage();
            }
        });
    }

    private void printAndroidLabel() {
        Log.d(TAG, "minuta uběhla");
        Log.d(TAG, "chyba: " + alertMessage);
        if (clientsFromServer != null) for (User us : clientsFromServer) {
            Log.d(TAG, "userID: " + us.getSsid() + " latitude: " + us.getLatitude() + " longitude: " + us.getLongitude()
                    + " act state: " + us.getActualState() + " fut state: " + us.getFutureState() +
                    " first conn: " + us.getFirstConnectionToServer() + " last conn: " + us.getLastConnectionToServer()
                    + " isOnline: " + us.isOnline());
            if (us.getSensorInformation() != null) {
                Log.d(TAG, " teplota: " + us.getSensorInformation().getTemperature()
                        + " tlak: " + us.getSensorInformation().getPressure() + " isOnline: " + us.isOnline());
            }
        }
    }


}
