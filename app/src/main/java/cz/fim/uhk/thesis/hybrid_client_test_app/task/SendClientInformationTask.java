package cz.fim.uhk.thesis.hybrid_client_test_app.task;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import cz.fim.uhk.thesis.hybrid_client_test_app.api.IsCentralServerApi;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SendClientInformationTask {
    private IsCentralServerApi isCentralServerApi;

    private static final String TAG = "SendClientInfoTask";

    public SendClientInformationTask(IsCentralServerApi isCentralServerApi) {
        this.isCentralServerApi = isCentralServerApi;
    }

    public void sendClientInformationToServer(User clientToSend) {
        // request na server
        Call<ResponseBody> call = isCentralServerApi.createUser(clientToSend);

        // zpracování response ze serveru
        // metoda enqueue zajistí, aby zpracovaní proběhlo na nově vytvořeném background vlákně
        call.enqueue(new Callback<ResponseBody>() {
            // pokud dostaneme response (nemusí být úspěšný)
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // kontrola zda response je neúspěšný a návrat
                if (!response.isSuccessful()) {
                    // log chyby
                    Log.e(TAG, "Neúspěšný response, kód: " + response.code()
                            + " zpráva: " + response.message());
                    return;
                }

                // zpracování response - požadavek na ztloustnutí
                // int kód: 0 -> nic 1 -> ztloustnout
                try {
                    int res = response.body().bytes()[0];
                    Log.d(TAG, "ZTLOUSTNOUT: " + res);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // pokud při spojení či zpracování požadavku došlo k chybě
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Nepodařilo se zaslat informace o klientovi na server: " + t.getMessage());
            }
        });


    }

}
