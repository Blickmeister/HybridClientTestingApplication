package cz.fim.uhk.thesis.hybrid_client_test_app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.R;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.converter.ByteArrayConverter;
import cz.fim.uhk.thesis.hybrid_client_test_app.modularity.LibraryLoaderModule;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import cz.fim.uhk.thesis.hybrid_client_test_app.ui.configuration.ConfigurationFragment;
import dalvik.system.DexClassLoader;

public class P2PBroadcastReceiver extends BroadcastReceiver {
    private MainActivity mainActivity;

    private static final String TAG = "P2PBroadcastReceiver";

    public P2PBroadcastReceiver(MainActivity mainActivity) { this.mainActivity = mainActivity; }

    @Override
    public void onReceive(Context context, Intent intent) {
        // pokud nastala událost obdržení výsledku p2p knihovny v roli klienta
        // tedy seznam klientů z okolního zařízení (peer)
        if (mainActivity.getString(R.string.receive_clients_from_p2plibrary_action_name)
                .equals(intent.getAction())) {

            Log.d(TAG, "obdržení zprávy od p2p serveru");
            // získání výsledků z Extra objektu
            byte[] result = intent.getByteArrayExtra(mainActivity
                    .getString(R.string.receive_clients_from_p2plibrary_extra_name));
            // převod byte[] na list klientů
            List<User> clients = ByteArrayConverter.byteArrayToUserList(result);
            // uložení nového seznamu klientů - nikoliv ze serveru, ale z okolního peer zařízení v roli serveru
            if (clients != null && !clients.isEmpty()) mainActivity.setClients(clients);
            // pro kontrolu vypsaní seznamu klientů získaných z peer zařízení
            for (User us : clients) {
                Log.d(TAG, "Klient ID z p2p serveru: " + us.getSsid());
            }
            Toast.makeText(context, "Data ze p2p serveru úspěšně obdržena", Toast.LENGTH_SHORT)
                    .show();
        } else if (mainActivity.getString(R.string.receive_client_info_from_p2plibrary_action_name)
                .equals(intent.getAction())) {
            Log.d(TAG, "obdržení zprávy od p2p klienta");
            // pokud nastala událost obdržení výsledku p2p knihovny v roli serveru
            // tedy obdržení kontextu klienta - zařízení (peer)
            // získání výsledků z Extra objektu
            byte[] result = intent.getByteArrayExtra(mainActivity
                    .getString(R.string.receive_client_info_from_p2plibrary_extra_name));
            // převod byte[] na kontext klienta
            User clientContext = ByteArrayConverter.byteArrayToClientContext(result);
            // uložení do DB - synchronizace:
            // existuje předpoklad, že p2p hybridní klient v roli serveru má k dispozici offline knihovnu
            // a tedy i DB
            // vytažení instance offline knihovny
            Object offlineLibraryInstance = mainActivity.getLibraries()
                    .get(MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION);
            LibraryLoaderModule libraryLoaderModule = new LibraryLoaderModule(mainActivity.getMyDb(),
                    mainActivity.getApplicationContext(), mainActivity);
            if (offlineLibraryInstance != null && clientContext != null) {
                Log.d(TAG, "ID klienta od p2p klienta: " + clientContext.getSsid());
                // aktualizovaný seznam klientů pro uložení do DB
                List<User> clientsToSave = mainActivity.getClients();
                clientsToSave.add(clientContext);
                mainActivity.setClients(clientsToSave);

                // uložení do DB skrze offline knihovnu
                int res = libraryLoaderModule.loadClass(mainActivity.getDexPathForLibraries(),
                        ConfigurationFragment.getLibraryNames()[MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION]);
                if (res == 0) {
                    Toast.makeText(context, "Data od p2p klienta úspěšně obdržena", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Log.e(TAG, "Nepodařilo se uložení do DB skrze offline knihovnu");
                }
            }
        }
    }
}
