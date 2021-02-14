package cz.fim.uhk.thesis.hybrid_client_test_app.helper;

import java.util.List;
import java.util.TimerTask;

import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.api.IsCentralServerApi;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetClientsTimerTask extends TimerTask {

    private List<User> clientsFromServer;
    private IsCentralServerApi isCentralServerApi;
    private String alertMessage;
    private MainActivity mainActivity;

    public GetClientsTimerTask(IsCentralServerApi isCentralServerApi, MainActivity mainActivity) {
        this.isCentralServerApi = isCentralServerApi;
        this.mainActivity = mainActivity;
    }

    @Override
    public void run() {
        getClientListFromServer();
        printAndroidLable();
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
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                // kontrola zda response je neúspěšný
                if (!response.isSuccessful()) {
                    // zobrazíme chybový HTTP kód a návrat z metody
                    alertMessage = "HTTP kód: " + response.code();
                    return;
                }

                // uložení dat
                clientsFromServer = response.body();
                mainActivity.setClients(clientsFromServer);
            }
            // pokud při spojení či zpracování požadavku došlo k chybě
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
               alertMessage = t.getMessage();
            }
        });
    }

    public List<User> getClientsFromServer() {
        return clientsFromServer;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    private void printAndroidLable() {
        System.out.println("minuta uběhla");
        System.out.println("chyba: " + alertMessage);
        if(clientsFromServer != null) for(User us : clientsFromServer) {
            System.out.println("userID: " + us.getSsid() + " latitude: " + us.getLatitude() + " longitude: " + us.getLongitude()
            + " act state: " + us.getActualState() + " fut state: " + us.getFutureState() +
                    " first conn: " + us.getFirstConnectionToServer() + " last conn: " + us.getLastConnectionToServer()
            + " SI SSID: "  + " teplota: " + us.getSensorInformation().getTemperature()
            + " tlak: " + us.getSensorInformation().getPressure() + " isOnline: " + us.isOnline());
        }
    }


}
