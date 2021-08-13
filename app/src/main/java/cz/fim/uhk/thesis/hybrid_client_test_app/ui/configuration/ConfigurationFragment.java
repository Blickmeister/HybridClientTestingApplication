package cz.fim.uhk.thesis.hybrid_client_test_app.ui.configuration;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.R;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.database.DatabaseHelper;
import cz.fim.uhk.thesis.hybrid_client_test_app.modularity.LibraryLoaderModule;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Submodul hlavního (řídícího) modulu pro manuální rekonfiguraci klienta
 */
public class ConfigurationFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "MainAct/ConfigFrag";
    // názvy použitých knihoven
    private static final String[] libraryNames = {"LibraryForOfflineMode",
            "LibraryForP2PCommunication"};

    private Button btnGetFatOffline;
    private Button btnGetFatP2pServer;
    private Button btnGetFatP2pClient;
    private Button btnSlim;
    private TextView txtAppState;
    private ProgressBar progressBar;

    private DatabaseHelper myDb;
    private MainActivity mainActivity;
    private Context context;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_conf, container, false);
        btnGetFatOffline = root.findViewById(R.id.btn_getFat_offline);
        btnGetFatP2pServer = root.findViewById(R.id.btn_getFat_p2p_server);
        btnGetFatP2pClient = root.findViewById(R.id.btn_getFat_p2p_client);
        btnSlim = root.findViewById(R.id.btn_slim);
        Button btnShowClients = root.findViewById(R.id.btn_show_clients);
        txtAppState = root.findViewById(R.id.txt_conf_state_to_set);
        progressBar = root.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        // přesměrování na fragment zobrazující seznam klientů ze serveru
        btnShowClients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(Objects.requireNonNull(getActivity()), R.id.nav_host_fragment).navigate(R.id.nav_users);
            }
        });

        btnGetFatOffline.setOnClickListener(this);
        btnGetFatP2pServer.setOnClickListener(this);
        btnGetFatP2pClient.setOnClickListener(this);
        btnSlim.setOnClickListener(this);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity = (MainActivity) getActivity();
        context = getContext();
        myDb = mainActivity.getMyDb();

        // nastavení GUI dle kontextu aplikace
        SharedPreferences sharedPref = mainActivity.getSharedPref();
        int state = sharedPref.getInt(getString(R.string.sh_pref_app_state), 0); // TODO odkomentovat
        //state = 0; // TODO pryc
        if (state == MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE) {
            setGUI(MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE);
        } else if (state == MainActivity.LIBRARY_FOR_ONLINE_MODE_CODE) {
            setGUI(MainActivity.LIBRARY_FOR_ONLINE_MODE_CODE);
        } else if (state == MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE) {
            setGUI(MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE);
        } else if (state == MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE) {
            setGUI(MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE);
        }
    }

    @Override
    @SuppressLint({"StaticFieldLeak", "NonConstantResourceId"})
    // problém AsyncTask -> měl by být implementován jako statická třída
    // to může způsobit komplikace (dala by se využít konstrukce WeakReference)
    // nicméně v mnoha názorech a tutoriálech je toto upozornění ignorováno a nemělo by být nebezpečné
    // NonConstantResourceId -> switch předělat na if-else -> opět není nutné řešit
    public void onClick(View v) {
        // init instance modulu pro dynamické zavedení knihovny
        final LibraryLoaderModule libraryLoaderModule = new LibraryLoaderModule(myDb, getContext(),
                (MainActivity) getActivity());
        switch (v.getId()) {
            case R.id.btn_getFat_offline:
                // aplikace se bude chovat jako tlustý klient s možností fungovat offline
                new AsyncTask<Void, Void, Void>() {
                    int success;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        progressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    protected Void doInBackground(Void... voids) {
                        Log.d(TAG, "start změny kontextu na tlustého klienta s offline režimem ....");
                        // zavedení knihovny pro offline režim pomocí modulu pro dynamické zavedení knihoven
                        success = libraryLoaderModule
                                .loadLibrary(libraryNames[MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION]);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        if (success == 0) {
                            // nastavení GUI
                            setContextByLibrary(MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE, true, mainActivity, context);
                        } else if (success == 1) {
                            setContextByLibrary(MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE, false, mainActivity, context);
                        }
                        if (progressBar.isIndeterminate()) {
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                }.execute();
                break;

            case R.id.btn_getFat_p2p_server:
                // aplikace se bude chovat jako tlustý klient s možností poskytnout službu okolním klientům
                Log.d(TAG, "start změny kontextu na tlustého klienta s p2p režimem v roli server ....");
                progressBar.setVisibility(View.VISIBLE);
                int success;
                // zavedení knihovny pro p2p režim pomocí modulu pro dynamické zavedení knihoven
                // zde není třeba stahovat - každý klient má knihovnu v uložišti
                // cesta ke knihovnám
                String dexPath = mainActivity.getDexPathForLibraries();
                String p2pLibraryName = libraryNames[MainActivity.LIBRARY_FOR_P2P_MODE_POSITION];
                // role v jaké má být knihovna zavedena
                libraryLoaderModule.setP2pLibraryRole(MainActivity.RUN_P2P_LIBRARY_AS_SERVER_VALUE);
                // zavedení pokud je knihovna v uložišti -> měla by být
                if (libraryLoaderModule.isLibraryInDirection(p2pLibraryName)) {
                    success = libraryLoaderModule.loadClass(dexPath, p2pLibraryName);
                } else {
                    Log.e(TAG, "P2P knihovna není v uložišti");
                    success = 1;
                }
                // pokud se úspěšně zavede
                if (success == 0) {
                    // nastavení GUI a kontextu
                    setContextByLibrary(MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE,
                            true, mainActivity, context);
                    // inicializace BroadcastReceiveru a IntentFilteru pro naslouchání zaslání výsledků z p2p knihovny
                    mainActivity.registerP2PBroadcastReceiver();
                } else if (success == 1) {
                    // pokud se vyskytne chyba při zavedení
                    setContextByLibrary(MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE,
                            false, mainActivity, context);
                }
                if (progressBar.isIndeterminate()) {
                    progressBar.setVisibility(View.INVISIBLE);
                }
                break;

            case R.id.btn_getFat_p2p_client:
                // aplikace se bude chovat jako hybridní klient v roli p2p klienta, tedy s možností
                // připojit se k okolnímu zařízení v p2p režimu v roli serveru
                Log.d(TAG, "start změny kontextu na tlustého klienta s p2p režimem v roli klienta ....");
                int getFatP2pClientSuccess;
                String P2P_LIBRARY_DEX_PATH = new ContextWrapper(mainActivity).getFilesDir() +
                        "/libs";
                // role v jaké má být knihovna zavedena
                libraryLoaderModule.setP2pLibraryRole(MainActivity.RUN_P2P_LIBRARY_AS_CLIENT_VALUE);
                // zavedení pokud je knihovna v uložišti -> měla by být
                if (libraryLoaderModule.isLibraryInDirection(libraryNames[MainActivity.LIBRARY_FOR_P2P_MODE_POSITION])) {

                    getFatP2pClientSuccess = libraryLoaderModule.loadClass(P2P_LIBRARY_DEX_PATH,
                            libraryNames[MainActivity.LIBRARY_FOR_P2P_MODE_POSITION]);
                } else {
                    Log.e(TAG, "P2P knihovna není v uložišti");
                    getFatP2pClientSuccess = 1;
                }
                if (getFatP2pClientSuccess == 0) {
                    // nastavení GUI a kontextu
                    setContextByLibrary(MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE,
                            true, mainActivity, context);
                    // inicializace BroadcastReceiveru a IntentFilteru pro naslouchání zaslání výsledků z p2p knihovny
                    mainActivity.registerP2PBroadcastReceiver();
                } else if (getFatP2pClientSuccess == 1) {
                    setContextByLibrary(MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE,
                            false, mainActivity, context);
                }
                break;

            case R.id.btn_slim:
                // aplikace se bude chovat jako tenký klient
                try {
                    SharedPreferences sharedPref = mainActivity.getSharedPref();
                    int state = sharedPref.getInt(getString(R.string.sh_pref_app_state), 0);
                    HashMap<Integer, Object> libraries = mainActivity.getLibraries();
                    // je-li aplikace v roli hybridního klienta s p2p komunikací
                    if (state == MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE ||
                            state == MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE) {
                        // odpojení knihovny
                        Object p2pLibraryInstance = libraries
                                .get(MainActivity.LIBRARY_FOR_P2P_MODE_POSITION);
                        int res;
                        if (p2pLibraryInstance != null) {
                            Method exit = p2pLibraryInstance.getClass().getMethod("exit");
                            res = (int) exit.invoke(p2pLibraryInstance);
                        } else {
                            res = 1;
                        }
                        // pokud se podaří
                        if (res == 0) {
                            // odpojení receiveru aplikace
                            mainActivity.unregisterP2PBroadcastReceiver();
                            // změna kontextu aplikace -> tenký klient
                            setContextByLibrary(MainActivity.LIBRARY_FOR_ONLINE_MODE_CODE,
                                    true, mainActivity, context);
                            // odstranění p2p knihovny ze seznamu knihoven
                            libraries.remove(MainActivity.LIBRARY_FOR_P2P_MODE_POSITION);
                            mainActivity.setLibraries(libraries);
                            // vždy refresh Main Activity
                            mainActivity.recreate();
                        }
                    }
                    // je-li aplikace v roli tlustého klienta s offline režimem nebo p2p serveru
                    if (state == MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE
                            || state == MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE) {
                        // odpojení knihovny
                        Object offlineLibraryInstance = libraries
                                .get(MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION);
                        int res;
                        if (offlineLibraryInstance != null) {
                            Method exit = offlineLibraryInstance.getClass().getMethod("exit");
                            res = (int) exit.invoke(offlineLibraryInstance);
                        } else {
                            res = 1;
                            Log.e(TAG, "offlineLibraryInstance je null");
                        }
                        // pokud se podaří uvolnit offline knihovnu
                        if (res == 0) {
                            // změna kontextu aplikace -> tenký klient
                            setContextByLibrary(MainActivity.LIBRARY_FOR_ONLINE_MODE_CODE,
                                    true, mainActivity, context);
                            // odstranění offline knihovny ze seznamu knihoven
                            libraries.remove(MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION);
                            mainActivity.setLibraries(libraries);
                            // vždy refresh Main Activity
                            mainActivity.recreate();
                        }
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    if (e.getCause() != null) e.getCause().printStackTrace();
                }
                break;
        }
        //editor.apply();
    }

    // nastavení GUI a kontextu aplikace dle typu knihovny
    // libraryType dle řídící proměnné vyjadřující aktuální stav aplikace:
    // 0 -> tenký klient
    // 1 -> hybridní klient s offline režimem
    // 2 -> hybridní klient s p2p komunikací
    public void setContextByLibrary(int libraryType, boolean isLoadSuccessful, MainActivity activity, Context context) {
        // vytažení shared preferences
        SharedPreferences sharedPref = activity.getSharedPref();
        SharedPreferences.Editor editor = sharedPref.edit();
        // pro jistotu v podmínce vše - jinak platí, že bude-li 1 null tak budou i ostatní
        // pro případ, že přistupujeme k metodě z LibraryLoaderModule při stažení knihovny
        if (txtAppState == null || btnGetFatOffline == null || btnGetFatP2pServer == null || btnSlim == null) {
            txtAppState = ((MainActivity) context).findViewById(R.id.txt_conf_state_to_set);
            btnGetFatOffline = ((MainActivity) context).findViewById(R.id.btn_getFat_offline);
            btnGetFatP2pServer = ((MainActivity) context).findViewById(R.id.btn_getFat_p2p_server);
            btnGetFatP2pClient = ((MainActivity) context).findViewById(R.id.btn_getFat_p2p_client);
            btnSlim = ((MainActivity) context).findViewById(R.id.btn_slim);
        }
        switch (libraryType) {
            case MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE:
                if (isLoadSuccessful) {
                    // nastavení GUI
                    setGUI(MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE);
                    // nastavení kontextu -> řídící proměnné stavu aplikace
                    editor.putInt(context.getString(R.string.sh_pref_app_state), libraryType);
                    activity.setApplicationState(libraryType);
                    // ukončení komunikace se serverem
                    activity.getMyTimer().cancel();
                    Log.d(TAG, "Úspěšná změna kontextu na tlustého klienta s offline režimem");
                } else {
                    Log.e(TAG, "Nepodařilo se změnit kontext na tlusetého klienta s offline režimem");
                    Toast.makeText(context, "Nepodařilo se změnit kontext na tlusetého klienta s offline režimem",
                            Toast.LENGTH_LONG).show();
                }
                break;
            case MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE:
                if (isLoadSuccessful) {
                    // nastavení GUI
                    setGUI(MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE);
                    // nastavení kontextu -> řídící proměnné stavu aplikace
                    if (getContext() != null) {
                        editor.putInt(getContext().getString(R.string.sh_pref_app_state), libraryType);
                        activity.setApplicationState(libraryType);
                    } else {
                        Log.e(TAG, "Context je null");
                    }
                    // ukončení komunikace se serverem
                    activity.getMyTimer().cancel();
                    Log.d(TAG, "Úspěšná změna kontextu na tlustého klienta s p2p režimem v roli serveru");
                } else {
                    Log.e(TAG, "Nepodařilo se změnit kontext na tlustého klienta s peer-to-peer" +
                            " režimem v roli serveru");
                    Toast.makeText(getContext(), "Nepodařilo se změnit kontext na tlustého klienta" +
                                    " s peer-to-peer režimem v roli serveru",
                            Toast.LENGTH_LONG).show();
                }
                break;
            case MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE:
                if (isLoadSuccessful) {
                    // nastavení GUI
                    setGUI(libraryType);
                    // nastavení kontextu -> řídící proměnné stavu aplikace
                    if (getContext() != null) {
                        editor.putInt(getContext().getString(R.string.sh_pref_app_state), libraryType);
                        activity.setApplicationState(libraryType);
                    } else {
                        Log.e(TAG, "Context je null");
                    }
                    // ukončení komunikace se serverem
                    activity.getMyTimer().cancel();
                    Log.d(TAG, "Úspěšná změna kontextu na hybridního klienta s p2p režimem v roli klienta");
                } else {
                    Log.e(TAG, "Nepodařilo se změnit kontext na hybridního klienta s peer-to-peer" +
                            " režimem v roli klienta");
                    Toast.makeText(getContext(), "Nepodařilo se změnit kontext na hybridního klienta" +
                                    " s peer-to-peer režimem v roli klienta",
                            Toast.LENGTH_LONG).show();
                }
                break;
            case MainActivity.LIBRARY_FOR_ONLINE_MODE_CODE:
                if (isLoadSuccessful) {
                    // nastavení GUI
                    setGUI(libraryType);
                    // nastavení kontextu -> řídící proměnné stavu aplikace
                    if (getContext() != null) {
                        editor.putInt(getContext().getString(R.string.sh_pref_app_state), libraryType);
                        activity.setApplicationState(libraryType);
                    } else {
                        Log.e(TAG, "Context je null");
                    }
                    Log.d(TAG, "Úspěšná změna kontextu na tenkého klienta");
                } else {
                    Log.e(TAG, "Nepodařilo se změnit kontext na tenkého klienta");
                    Toast.makeText(getContext(), "Nepodařilo se změnit kontext na tenkého klienta",
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
        editor.apply();
    }

    // metoda pro nastavení GUI dle kontextu aplikace
    private void setGUI(int libraryType) {
        switch (libraryType) {
            case MainActivity.LIBRARY_FOR_ONLINE_MODE_CODE:
                txtAppState.setText(R.string.txt_conf_state_to_set);
                btnGetFatOffline.setEnabled(true);
                btnGetFatP2pServer.setEnabled(false);
                btnGetFatP2pClient.setEnabled(true);
                btnSlim.setEnabled(false);
                break;
            case MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE:
                txtAppState.setText(R.string.txt_conf_state_offline_mode);
                btnGetFatOffline.setEnabled(false);
                btnGetFatP2pServer.setEnabled(true);
                btnGetFatP2pClient.setEnabled(false);
                btnSlim.setEnabled(true);
                break;
            case MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE:
                txtAppState.setText(R.string.txt_conf_state_p2p_server);
                btnGetFatOffline.setEnabled(false);
                btnGetFatP2pServer.setEnabled(false);
                btnGetFatP2pClient.setEnabled(false);
                btnSlim.setEnabled(true);
                break;
            case MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE:
                txtAppState.setText(R.string.txt_conf_state_p2p_client);
                btnGetFatOffline.setEnabled(false);
                btnGetFatP2pServer.setEnabled(false);
                btnGetFatP2pClient.setEnabled(false);
                btnSlim.setEnabled(true);
                break;
        }
    }

    public static String[] getLibraryNames() {
        return libraryNames;
    }
}