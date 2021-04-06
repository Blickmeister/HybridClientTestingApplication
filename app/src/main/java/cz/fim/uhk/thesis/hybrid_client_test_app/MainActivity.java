package cz.fim.uhk.thesis.hybrid_client_test_app;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import cz.fim.uhk.thesis.hybrid_client_test_app.api.IsCentralServerApi;
import cz.fim.uhk.thesis.hybrid_client_test_app.task.GetClientsTimerTask;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.database.DatabaseHelper;
import cz.fim.uhk.thesis.hybrid_client_test_app.modularity.LibraryLoaderModule;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import cz.fim.uhk.thesis.hybrid_client_test_app.receiver.P2PBroadcastReceiver;
import cz.fim.uhk.thesis.hybrid_client_test_app.task.SendClientInformationTask;
import cz.fim.uhk.thesis.hybrid_client_test_app.ui.configuration.ConfigurationFragment;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

/**
 *
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Hlavní (řídící) modul
 */
public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity"; // logovací TAG

    private AppBarConfiguration mAppBarConfiguration;
    // seznam klientů v aplikaci
    private List<User> clients;
    // seznam klientů z offline knihovny
    private List<?> usersFromDB;
    // kontext klienta
    private User currentUser;
    // časovač pro pravidelné úlohy aplikace
    private Timer myTimer;
    // pro zasílání na server - komponenty komunikačního modulu
    private IsCentralServerApi isCentralServerApi;
    private SendClientInformationTask sendClientInfoTask;
    // cesta k umístění knihoven pro jejich zavedení
    private String dexPathForLibraries;
    // proměnné vyjadřující aktuální stav aplikace:
    // 0 -> tenký klient
    // 1 -> hybridní klient s offline režimem
    // 2 -> hybridní klient s p2p komunikací
    private int applicationState;
    private SharedPreferences sharedPref;
    // kolekce instancí knihoven
    private HashMap<Integer, Object> libraries = new HashMap<>();
    // pozice knihoven v seznamu knihoven
    public static final int LIBRARY_FOR_OFFLINE_MODE_POSITION = 0;
    public static final int LIBRARY_FOR_P2P_MODE_POSITION = 1;
    // kódy knihoven (pro GUI a kontext aplikace)
    public static final int LIBRARY_FOR_OFFLINE_MODE_CODE = 1;
    public static final int LIBRARY_FOR_P2P_SERVER_MODE_CODE = 2;
    public static final int LIBRARY_FOR_P2P_CLIENT_MODE_CODE = 3;
    public static final int LIBRARY_FOR_ONLINE_MODE_CODE = 0;
    // konstanty reprezentující hodnoty módu spuštění p2p knihovny
    public static final int RUN_P2P_LIBRARY_AS_CLIENT_VALUE = 0;
    public static final int RUN_P2P_LIBRARY_AS_SERVER_VALUE = 1;
    public static final int DOWNLOAD_ONLY_P2P_LIBRARY_VALUE = -1;
    public DatabaseHelper myDb;

    // pro naslouchání výsledků p2p knihovny
    private final P2PBroadcastReceiver pBroadcastReceiver = new P2PBroadcastReceiver(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // nastavení gson converteru (JSON -> Java) pro Date formát
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm")
                .create();

        // client pro HTTP logging
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        // readTimeout(100, TimeUnit.SECONDS).connectTimeout(100,TimeUnit.SECONDS)
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        // init a nastavení retrofit objektu pro připojení k serveru
        Retrofit retrofit = new Retrofit.Builder()
                //.baseUrl("http://10.0.2.2:8080/") // localhost alias pro AVD - TODO ODKOMENTOVAT PRO EMULATOR
                .baseUrl("http://192.168.100.2:8080/") // localhost pro moji síť - fyzické zařízení
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // naplnění těl metod prostřednictvím retrofit objektu
        isCentralServerApi = retrofit.create(IsCentralServerApi.class);
        // init třídy pro zaslání informace o klientovi na server
        sendClientInfoTask = new SendClientInformationTask(isCentralServerApi);

        // init shared preferences pro persistentní uložení stavu aplikace
        sharedPref = this.getSharedPreferences(getString(R.string.sh_pref_file_key), Context.MODE_PRIVATE);

        ContextWrapper contextWrapper = new ContextWrapper(this);
        dexPathForLibraries = contextWrapper.getFilesDir() + "/" + "libs";

        //pBroadcastReceiver = new P2PBroadcastReceiver(this);

        // menu navigace
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_conf, R.id.nav_func, R.id.nav_users, R.id.nav_map)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // inicializace sqlite databáze pro správu knihoven v aplikaci
        myDb = new DatabaseHelper(this);
        clients = new ArrayList<>();

        currentUser = new User(); // objekt reprezentující klienta představujícího tuto aplikaci
        // kontrola, zda zařízení již má SSID (neběží poprvé)
        String ssid = sharedPref.getString(getString(R.string.sh_pref_ssid), null);
        if (ssid == null) {
            // zaregistrování nového zařízení do systému, běží-li aplikace poprvé
            //registerNewDevice(); // TODO okdkomentovat a dodelat (server)
        }

        // získání stavu aplikace
        applicationState = sharedPref.getInt(getString(R.string.sh_pref_app_state), 0); // TODO odkomentovat
        //applicationState = 0; // TODO pryc
        Log.d(TAG, "Aplikace běží ve stavu tenkého klienta");

        // stažení p2p knihovny, pokud již není stažena -> vždy by měla být k dispozici v uložišti pro zavedení
        LibraryLoaderModule libraryLoaderModule = new LibraryLoaderModule(myDb, this, this);
        String p2pLibraryName = ConfigurationFragment.getLibraryNames()
                [MainActivity.LIBRARY_FOR_P2P_MODE_POSITION];
        // není-li knihovna již stažena
        if (!libraryLoaderModule.isLibraryInDirection(p2pLibraryName)) {
            // chceme pouze stáhnout, nikoliv zavést -> set proměnné p2pLibraryRole na -1
            libraryLoaderModule.setP2pLibraryRole(-1);
            // konstanta pro umístění apk souboru p2p knihovny
            String P2P_LIBRARY_DEX_PATH = new ContextWrapper(this).getFilesDir() +
                    "/libs";
            // stažení p2p knihovny
            libraryLoaderModule.downloadLibrary(p2pLibraryName, P2P_LIBRARY_DEX_PATH);
        }

        // rozdělení chování aplikace dle jejího aktuálního stavu
        if (applicationState == 0) {
            /*Log.d(TAG, "Aplikace běží ve stavu tenkého klienta");
            // stažení p2p knihovny, pokud již není stažena -> vždy by měla být k dispozici v uložišti pro zavedení
            LibraryLoaderModule libraryLoaderModule = new LibraryLoaderModule(myDb, this, this);
            String p2pLibraryName = ConfigurationFragment.getLibraryNames()
                    [MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_POSITION];
            // není-li knihovna již stažena
            if (!libraryLoaderModule.isLibraryInDirection(p2pLibraryName)) {
                // chceme pouze stáhnout, nikoliv zavést -> set proměnné p2pLibraryRole na -1
                libraryLoaderModule.setP2pLibraryRole(-1);
                // konstanta pro umístění apk souboru p2p knihovny
                String P2P_LIBRARY_DEX_PATH = new ContextWrapper(this).getFilesDir() +
                        "/libs";
                // stažení p2p knihovny
                libraryLoaderModule.downloadLibrary(p2pLibraryName, P2P_LIBRARY_DEX_PATH);
            }*/
            myTimer = new Timer();
            // opakování volání metody získání seznamu klientů ze serveru každou minutu
            final GetClientsTimerTask getClientsTimerTask = new GetClientsTimerTask(isCentralServerApi, this);
            myTimer.scheduleAtFixedRate(getClientsTimerTask, 0, 60000); // TODO 1 nula pryc -> 60000
            /*
            myTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    //clients = getClientsTimerTask.getClientsFromServer();
                }
            }, 10, 2000000);
            //final TestDataTimerTask testDataTimerTask = new TestDataTimerTask(isCentralServerApi);
            final Handler handler = new Handler();*/
            // TODO asi chyba ve stacku je new TimerTask(atd)
            /*myTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                TestDataTimerTask testDataTimerTask = new TestDataTimerTask(isCentralServerApi);
                                testDataTimerTask.execute();
                                System.out.println("DATA FROM SERVER: " + testDataTimerTask.getTestDataFromServer());
                                System.out.println("STAMP: " + testDataTimerTask.getStamp());
                            } catch (Exception e) {
                                Log.d("MAIN ACTIVITY","Nový vláknos problémos" + e);
                            }
                        }
                    });
                    //System.out.println("ALERT MESSAGE: " + testDataTimerTask.getAlertMessage());
                    //System.out.println("RESPONSE CODE: " + testDataTimerTask.getResponseCode());
                }
            }, 0, 12000);*/
            /*TimerTask doAsynchronousTask = new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            try {
                                TestDataTimerTask testDataTimerTask = new TestDataTimerTask(isCentralServerApi);
                                testDataTimerTask.run();
                            } catch (Exception e) {
                                Log.d("MAIN ACTIVITY","Nový vláknos problémos" + e);
                            }
                        }
                    });
                }
            };
            myTimer.schedule(doAsynchronousTask, 0, 30000); //execute in every 20000 ms*/
            /* final TestDataTimerTask testDataTimerTask = new TestDataTimerTask(isCentralServerApi);
            myTimer.scheduleAtFixedRate(testDataTimerTask, 0, 30000);
             myTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("ZNÁMKA PUNKU: " + testDataTimerTask.getStamp());
                }
            }, 10, 60000);*/
        } else if (applicationState == 1) {
            Log.d(TAG, "Aplikace běží ve stavu tlustého klienta s offline režimem");
            // získání klientů z DB offline knihovny
            getClientsFromOfflineLibrary();
            // otestování přítomnosti klientů v aplikaci
            for (User us : clients) {
                Log.d(TAG, "Uživatel ID: " + us.getSsid());
            }
        } else if (applicationState == 2) {
            Log.d(TAG, "Aplikace běží ve stavu tlustého klienta s p2p režimem v roli server");
            // opět získání klientů z DB offline knihovny
            getClientsFromOfflineLibrary();
            // znovuzavedení p2p knihovny v roli serveru
            startP2PLibrary(RUN_P2P_LIBRARY_AS_SERVER_VALUE);
        } else {
            Log.d(TAG, "Aplikace běží ve stavu hybridního klienta s p2p režimem v roli klienta");
            // znovuzavedení p2p knihovny v roli klienta
            startP2PLibrary(RUN_P2P_LIBRARY_AS_CLIENT_VALUE);

        }
    }

    // metoda pro zaregistrování nového zařízení do systému (architektury)
    private void registerNewDevice() {
            // vygenerování SSID pro identifikaci zařízení - předpona "mk" a IMEI
            String ssid = "mk";
            // získání IMEI zařízení
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String imei = telephonyManager.getDeviceId();
            // pokud zařízení poskytuje IMEI
            if (imei != null || imei.length() != 0) {
                // přidáme IMEI
                ssid += imei;
                // uložení ssid zařízení perzistentně
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.sh_pref_ssid), ssid);
                editor.apply();
            }
            // jinak SSID generuje server -> zašleme jen "mk"
            currentUser.setSsid(ssid);
            // nastavení zbytku inicializačních hodnot
            currentUser.setActualState("tenky");
            // možné budoucí stavy klientů pro automatizované přepínání kontextu
            // stavy oddeleny ; a camel case: stavKlienta1;stavKlienta2
            // dáno pevně -> šel by dodelat treba Radio Button pro prepinani budoucich stavu
            // nicméně přepínat kontext lze i pomocí tlačítek v Configuration fragmentu
            currentUser.setFutureState("offline;p2pServer");
            //currentUser.setFutureState("p2pClient"); // odkomentovat chceme-li v budoucnu p2p klienta
            // zaslání na server
            sendClientInfoTask.sendClientInformationToServer(currentUser);
    }

    // metoda pro získání klientů z DB offline knihovny a nastavení seznamu klientů v aplikaci
    private void getClientsFromOfflineLibrary() {
        // získání instance knihovny - persistentní skrz gson nefunguje
        Object offlineLibraryInstance = null;
        if (!libraries.isEmpty()) {
            offlineLibraryInstance = libraries.get(LIBRARY_FOR_OFFLINE_MODE_POSITION);
        }

        // znamená že aplikace byla vypnuta a přitom se nachází ve stavu tlustého klienta
        // je nutné znovu vytáhnout instanci offline knihovny - perzistentní uložení se nepodařilo
        // znovu zavedení knihovny
        if (offlineLibraryInstance == null) {
            LibraryLoaderModule libraryLoaderModule = new LibraryLoaderModule(myDb, this, this);

            // znovu zavedení - součástí je i získání klientů z DB a uložení do seznamu klientů v aplikaci
            int res = libraryLoaderModule.loadClass(dexPathForLibraries, ConfigurationFragment.getLibraryNames()
                    [LIBRARY_FOR_OFFLINE_MODE_POSITION]);
            // podařilo se zavést
            if (res == 0) {
                Log.d(TAG, "Úspěšné znovuzavedení knihovny pro offline režim");
            } else {
                Log.e(TAG, "Neúspěšné znovuzavedení knihovny pro offline režim");
            }
        }
    }

    // metoda pro znovuzavedení p2p knihovny dle role vykonávání činnosti knihovny
    private void startP2PLibrary(int mode) {
        // získání instance knihovny - persistentní skrz gson nefunguje
        Object p2pLibraryInstance = null;
        if (!libraries.isEmpty()) {
            p2pLibraryInstance = libraries.get(LIBRARY_FOR_P2P_MODE_POSITION);
        }
        // znovu zavedení knihovny - při restartu aplikace
        if (p2pLibraryInstance == null) {
            LibraryLoaderModule libraryLoaderModule = new LibraryLoaderModule(myDb, this, this);

            // nastevení způsobu zavedení knihovny dle role knihovny
            if (mode == RUN_P2P_LIBRARY_AS_SERVER_VALUE)
                libraryLoaderModule.setP2pLibraryRole(RUN_P2P_LIBRARY_AS_SERVER_VALUE);
            else libraryLoaderModule.setP2pLibraryRole(RUN_P2P_LIBRARY_AS_CLIENT_VALUE);
            // znovu zavedení - součástí i zaregistrování receiveru a intentFilteru
            int res = libraryLoaderModule.loadClass(dexPathForLibraries, ConfigurationFragment.getLibraryNames()
                    [LIBRARY_FOR_P2P_MODE_POSITION]);
            // podařilo se zavést
            if (res == 0) {
                // inicializace BroadcastReceiveru a IntentFilteru pro naslouchání zaslání výsledků z p2p knihovny
                registerP2PBroadcastReceiver();
                Log.d(TAG, "Úspěšné znovuzavedení knihovny pro p2p režim");
            } else {
                Log.e(TAG, "Neúspěšné znovuzavedení knihovny pro p2p režim");
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // zrušení Timeru a Receiveru
        myTimer.cancel();
        if (applicationState == 2 || applicationState == 3) {
            // API pro BroadcastReceiver neposkytuje metodu typu isRegistered() -> try catch místo toho
            try {
                unregisterReceiver(pBroadcastReceiver);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Receiver již je odregistrovaný");
            }
            // zastavení chodu p2p knihovny
            Object p2pLibraryInstance = libraries.get(LIBRARY_FOR_P2P_MODE_POSITION);
            if (p2pLibraryInstance != null) {
                int res = 1;
                try {
                    Method stop = p2pLibraryInstance.getClass().getMethod("stop");
                    res = (int) stop.invoke(p2pLibraryInstance);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    Log.e(TAG, "Nepodařilo se zavolat metodu stop() p2p knihovny: ");
                    e.printStackTrace();
                }
                if (res == 0) {
                    Log.d(TAG, "Chod 2p2 knihovny úspěšně pozastaven");
                } else {
                    Log.e(TAG, "Chod 2p2 knihovny se nepodařilo pozastavit");
                }
            }
        } else if (applicationState == 1) {
            // zastavení chodu offline knihovny
            Object offlineLibraryInstance = libraries.get(LIBRARY_FOR_OFFLINE_MODE_POSITION);
            if (offlineLibraryInstance != null) {
                int res = 1;
                try {
                    Method stop = offlineLibraryInstance.getClass().getMethod("stop");
                    res = (int) stop.invoke(offlineLibraryInstance);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    Log.e(TAG, "Nepodařilo se zavolat metodu stop() offline knihovny: ");
                    e.printStackTrace();
                }
                if (res == 0) {
                    Log.d(TAG, "Chod offline knihovny úspěšně pozastaven");
                } else {
                    Log.e(TAG, "Chod offline knihovny se nepodařilo pozastavit");
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (applicationState == 2 || applicationState == 3) {
            // API pro BroadcastReceiver neposkytuje metodu typu isRegistered() -> try catch místo toho
            try {
                registerP2PBroadcastReceiver();
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Receiver již je registrovaný");
            }
            // znovuspuštění chodu p2p knihovny
            Object p2pLibraryInstance = libraries.get(LIBRARY_FOR_P2P_MODE_POSITION);
            if (p2pLibraryInstance != null) {
                int res = 1;
                try {
                    Method start = p2pLibraryInstance.getClass().getMethod("resume");
                    res = (int) start.invoke(p2pLibraryInstance);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    Log.e(TAG, "Nepodařilo se zavolat metodu start() p2p knihovny: ");
                    e.printStackTrace();
                }
                if (res == 0) {
                    Log.d(TAG, "Chod 2p2 knihovny úspěšně znovuspuštěn");
                } else {
                    Log.e(TAG, "Chod 2p2 knihovny se nepodařilo znovuspustit");
                }
            }
        } else if (applicationState == 1) {
            // obnovení chodu offline knihovny
            Object offlineLibraryInstance = libraries.get(LIBRARY_FOR_OFFLINE_MODE_POSITION);
            if (offlineLibraryInstance != null) {
                int res = 1;
                try {
                    Method start = offlineLibraryInstance.getClass().getMethod("resume");
                    res = (int) start.invoke(offlineLibraryInstance);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    Log.e(TAG, "Nepodařilo se zavolat metodu start() offline knihovny: ");
                    e.printStackTrace();
                }
                if (res == 0) {
                    Log.d(TAG, "Chod offline knihovny úspěšně obnoven");
                } else {
                    Log.e(TAG, "Chod offline knihovny se nepodařilo obnovit");
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void registerP2PBroadcastReceiver() {
        if (applicationState == LIBRARY_FOR_P2P_SERVER_MODE_CODE) {
            IntentFilter intentFilter = new IntentFilter(getString(R.string.receive_client_info_from_p2plibrary_action_name));
            registerReceiver(pBroadcastReceiver, intentFilter);
            Log.d(TAG, "Receiver pro naslouchání v roli serveru registrován");
        } else if (applicationState == LIBRARY_FOR_P2P_CLIENT_MODE_CODE) {
            Log.d(TAG, "Receiver pro naslouchání v roli klienta registrován");
            IntentFilter intentFilter = new IntentFilter(getString(R.string.receive_clients_from_p2plibrary_action_name));
            registerReceiver(pBroadcastReceiver, intentFilter);
        }
    }

    public void unregisterP2PBroadcastReceiver() {
        unregisterReceiver(pBroadcastReceiver);
    }

    public List<User> getClients() {
        return clients;
    }

    public void setClients(List<User> clients) {
        this.clients = clients;
    }

    public IsCentralServerApi getIsCentralServerApi() {
        return isCentralServerApi;
    }

    public void setApplicationState(int applicationState) { this.applicationState = applicationState; }

    public HashMap<Integer, Object> getLibraries() {
        return libraries;
    }

    public void setLibraries(HashMap<Integer, Object> libraries) {
        this.libraries = libraries;
    }

    public DatabaseHelper getMyDb() {
        return myDb;
    }

    public SharedPreferences getSharedPref() {
        return sharedPref;
    }

    public String getDexPathForLibraries() {
        return dexPathForLibraries;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}
