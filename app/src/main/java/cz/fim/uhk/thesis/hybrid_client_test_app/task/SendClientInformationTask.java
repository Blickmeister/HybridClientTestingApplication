package cz.fim.uhk.thesis.hybrid_client_test_app.task;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import cz.fim.uhk.thesis.hybrid_client_test_app.api.IsCentralServerApi;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Součást komunikačního modulu
 * Požadavek zaslání kontextu hybridního klienta na centrální server
 */
public class SendClientInformationTask {
    private final IsCentralServerApi isCentralServerApi;

    private static final String TAG = "SendClientInfoTask";

    public SendClientInformationTask(IsCentralServerApi isCentralServerApi) {
        this.isCentralServerApi = isCentralServerApi;
    }

    public void sendNewClientInformationToServer(User clientToSend) {
        // request na server
        Call<ResponseBody> call = isCentralServerApi.createUser(clientToSend);

        // zpracování response ze serveru
        // metoda enqueue zajistí, aby zpracovaní proběhlo na nově vytvořeném background vlákně
        call.enqueue(new Callback<ResponseBody>() {
            // pokud dostaneme response (nemusí být úspěšný)
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                // kontrola zda response je neúspěšný a návrat
                if (!response.isSuccessful()) {
                    // log chyby
                    Log.e(TAG, "Neúspěšný response, kód: " + response.code()
                            + " zpráva: " + response.message());
                }
            }

            // pokud při spojení či zpracování požadavku došlo k chybě
            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                Log.e(TAG, "Nepodařilo se zaslat informace o klientovi na server: " + t.getMessage());
            }
        });
    }

    public void sendClientInformationToServer(User clientToSend) {
        // request na server
        Call<ResponseBody> call = isCentralServerApi.updateUser(clientToSend);

        // zpracování response ze serveru
        // metoda enqueue zajistí, aby zpracovaní proběhlo na nově vytvořeném background vlákně
        call.enqueue(new Callback<ResponseBody>() {
            // pokud dostaneme response (nemusí být úspěšný)
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                // kontrola zda response je neúspěšný a návrat
                if (!response.isSuccessful()) {
                    // log chyby
                    Log.e(TAG, "Neúspěšný response, kód: " + response.code()
                            + " zpráva: " + response.message());
                }

                // zpracování response - požadavek na ztloustnutí
                // int kód: 0 -> nic 1 -> ztloustnout
                // náznak řešení
                /*try {
                    int res = response.body().bytes()[0];
                    Log.d(TAG, "ZTLOUSTNOUT: " + res);
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }

            // pokud při spojení či zpracování požadavku došlo k chybě
            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                Log.e(TAG, "Nepodařilo se zaslat informace o klientovi na server: " + t.getMessage());
            }
        });
    }

}
