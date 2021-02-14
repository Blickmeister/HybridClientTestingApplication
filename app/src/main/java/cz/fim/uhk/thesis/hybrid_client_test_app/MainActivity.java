package cz.fim.uhk.thesis.hybrid_client_test_app;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import cz.fim.uhk.thesis.hybrid_client_test_app.api.IsCentralServerApi;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.GetClientsTimerTask;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.database.DatabaseHelper;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.modularity.LibraryLoaderInterface;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.SensorInformation;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import cz.fim.uhk.thesis.hybrid_client_test_app.ui.configuration.ConfigurationFragment;
import dalvik.system.DexClassLoader;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.os.Handler;
import android.util.Log;
import android.view.Menu;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private AppBarConfiguration mAppBarConfiguration;
    private List<User> clients;
    private List<?> usersFromDB;
    private List<SensorInformation> sensorInformationList;
    private Timer myTimer;
    private IsCentralServerApi isCentralServerApi;
    // proměnné vyjadřující aktuální stav aplikace:
    // 0 -> tenký klient
    // 1 -> hybridní klient s offline režimem
    // 2 -> hybridní klient s p2p komunikací
    private int applicationState;
    private SharedPreferences sharedPref;
    // kolekce instancí knihoven
    private List<Object> libraries = new ArrayList<>();
    private DatabaseHelper myDb;
    // název třídy reprezentující klienta v offline knihovně
    private final static String OFFLINE_LIBRARY_USER_CLASS_NAME = "cz.fim.uhk.thesis.libraryforofflinemode.model.User";
    // seznamu názvů metod třídy User offline knihovny
    private final static String[] USER_METHOD_NAMES = {"getSsid", "getLatitude", "getLongitude",
            "isOnline", "getActualState", "getFutureState", "getFirstConnectionToServer", "getLastConnectionToServer",
            "getTemperature", "getPressure"};


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
                .baseUrl("http://10.0.2.2:8080/") // localhost alias pro AVD
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // naplnění těl metod prostřednictvím retrofit objektu
        isCentralServerApi = retrofit.create(IsCentralServerApi.class);

        // inicializace sqlite databáze pro správu knihoven v aplikaci
        myDb = new DatabaseHelper(this);
        clients = new ArrayList<>();

        // init shared preferences pro persistentní uložení stavu aplikace
        sharedPref = this.getSharedPreferences(getString(R.string.sh_pref_file_key), Context.MODE_PRIVATE);

        myTimer = new Timer();

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
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
        // získání stavu aplikace TODO odkomentovat
        applicationState = sharedPref.getInt(getString(R.string.sh_pref_app_state), 0);
        //applicationState = 0; // TODO pryc
        // rozdělení chování aplikace dle jejího aktuálního stavu
        if (applicationState == 0) {
            Log.d(TAG, "Aplikace běží ve stavu tenkého klienta");
            // opakování volání metody získání seznamu klientů ze serveru každou minutu
            final GetClientsTimerTask getClientsTimerTask = new GetClientsTimerTask(isCentralServerApi, this);
            myTimer.scheduleAtFixedRate(getClientsTimerTask, 0, 60000);
            // TODO získání výsledků
            myTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    //clients = getClientsTimerTask.getClientsFromServer();
                    String message = getClientsTimerTask.getAlertMessage();
                    System.out.println("ALERT MESSAGE: " + message);
                }
            }, 10, 600000); // TODO 1 nula pryc -> 60000
            //final TestDataTimerTask testDataTimerTask = new TestDataTimerTask(isCentralServerApi);
            final Handler handler = new Handler();
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
            for(User us : clients) {
                Log.d(TAG, "Uživatel ID: " + us.getSsid());
            }
        } else {
            Log.d(TAG, "Aplikace běží ve stavu tlustého klienta s p2p režimem");
        }
    }

    // metoda pro získání klientů z DB offline knihovny a nastavení seznamu klientů v aplikaci
    private void getClientsFromOfflineLibrary() {
        // získání instance knihovny - persistentní skrz gson nefunguje
        Object offlineLibraryInstance = null;
        if(!libraries.isEmpty()) {
            offlineLibraryInstance = libraries.get(ConfigurationFragment.getLibraryForOfflineModeCode());
        }
        // konstanta pro umístění apk souboru offline knihovny
        String OFFLINE_LIBRARY_DEX_PATH = new ContextWrapper(this).getFilesDir() +
                "/libs/LibraryForOfflineMode/library_for_offline_mode.apk";
        // init class loaderu
        DexClassLoader loader = new DexClassLoader
                (OFFLINE_LIBRARY_DEX_PATH, null, null, getClassLoader());
        // znamená že aplikace byla vypnuta a přitom se nachází ve stavu tlustého klienta
        // je nutné znovu vytáhnout instanci offline knihovny - perzistentní uložení se nepodařilo
        if(offlineLibraryInstance == null) {
            Class<?> mainClass = null;
            try {
                mainClass = Class.forName(myDb.getClassName("LibraryForOfflineMode"),
                        true, loader);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Nepodařilo se nalézt zaváděcí třídu offline knihovny: ");
                e.printStackTrace();
            }
            if(mainClass != null) {
                try {
                    offlineLibraryInstance = mainClass.newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    Log.e(TAG, "Nepodařilo se vytvořit instanci zaváděcí třídy knihovny: ");
                    e.printStackTrace();
                }
            }
        }
        // získání klientů z DB offline knihovny
        Method getUsers = null;
        int res = -1; // zda se povedlo knihovnu zavést skrze start metodu
        List<?> usersFromDB = new ArrayList<>();
        try {
            if(offlineLibraryInstance != null) {
                // nejprve je nutné knihovnu znovu zavést skrze metodu start
                ContextWrapper contextWrapper = new ContextWrapper(this);
                String dexPath = contextWrapper.getFilesDir() + "/" + "libs";
                Method start = offlineLibraryInstance.getClass().getMethod("start",
                        String.class, Context.class);
                res = (int) start.invoke(offlineLibraryInstance, dexPath, this);
                // získání klientů z DB
                getUsers = offlineLibraryInstance.getClass().getMethod("getUsers");
                usersFromDB = (List<?>) getUsers.invoke(offlineLibraryInstance);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Log.e(TAG, "Nepodařilo se získat seznam klientů z DB offline knihovny");
            e.getCause().printStackTrace();
        }
        if(getUsers != null && !usersFromDB.isEmpty() && res == 0) {
            // nastavení seznamu klientů v aplikaci
            setClientsFromUsersInDB(usersFromDB, loader);
                /*for (User user : clients) {
                    System.out.println("Useros id: " + user.getSsid() + "Useros lati: " + user.getLatitude());
                }*/

            /*if (usersInOfflineLibrary != null) {
                    /*List<User> clientsFromServer = activity.getClients();
                    if(clientsFromServer != null) {
                        for(User user : clientsFromServer) {
                            addUser.invoke(libraryInstance, user.getSsid(), user.getLatitude(), user.getLongitude(), user.isOnline(),
                                    user.getActualState(), user.getFutureState(), user.getFirstConnectionToServer(),
                                    user.getLastConnectionToServer(), user.getSensorInformation().getTemperature(),
                                    user.getSensorInformation().getPressure());
                        }
                    }
                    } else {
                    Log.e(TAG, "Atribut kolekce uživatelů z offline knihovny je null");
                }*/
            }
        }

    @Override
    protected void onStop() {
        super.onStop();
        myTimer.cancel();
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

    public List<User> getClients() {
        return clients;
    }

    public void setClients(List<User> clients) {
        this.clients = clients;
    }

    // setter pro převod seznamu klientů z DB offline knihovny na seznam klientů v aplikaci
    public void setClientsFromUsersInDB(List<?> usersFromDB, DexClassLoader loader) {
        List<User> clientsFromOfflineLibrary = new ArrayList<>();
        // vytažení třídy reprezentující klienta v offline knihovně pro následné získání metod
        Class<?> classToLoad = null;
        try {
            classToLoad = Class.forName(OFFLINE_LIBRARY_USER_CLASS_NAME, true, loader);

        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Nepodařilo se nalézt třídu User offline knihovny: ");
            e.printStackTrace();
        }
        // pokud třída User není null - podařilo se ji načíst - jinak nemá cenu pokračovat
        if (classToLoad != null) {
            // vytažení všech potřebných metod pro získání atributů klienta - důvod: nelze použít třídu
            // User v aplikaci -> ačkoliv stejná implementace jiné umístění nelze přetypovat
            try {
                Method getId = classToLoad.getMethod(USER_METHOD_NAMES[0]);
                Method getLatitude = classToLoad.getMethod(USER_METHOD_NAMES[1]);
                Method getLongitude = classToLoad.getMethod(USER_METHOD_NAMES[2]);
                Method isOnline = classToLoad.getMethod(USER_METHOD_NAMES[3]);
                Method getActualState = classToLoad.getMethod(USER_METHOD_NAMES[4]);
                Method getFutureState = classToLoad.getMethod(USER_METHOD_NAMES[5]);
                Method getFirstConnectionToServer = classToLoad.getMethod(USER_METHOD_NAMES[6]);
                Method getLastConnectionToServer = classToLoad.getMethod(USER_METHOD_NAMES[7]);
                Method getTemperature = classToLoad.getMethod(USER_METHOD_NAMES[8]);
                Method getPressure = classToLoad.getMethod(USER_METHOD_NAMES[9]);

                // převod seznamu klientů z DB na seznam klientů aplikace
                for (Object userDB : usersFromDB) {
                    // získání hodnot atributů 1 klienta z DB
                    String id = (String) getId.invoke(userDB);
                    double latitude = (double) getLatitude.invoke(userDB);
                    double longitude = (double) getLongitude.invoke(userDB);
                    boolean online = (boolean) isOnline.invoke(userDB);
                    String actualState = (String) getActualState.invoke(userDB);
                    String futureState = (String) getFutureState.invoke(userDB);
                    Date firstConnectionToServer = (Date) getFirstConnectionToServer.invoke(userDB);
                    Date lastConnectionToServer = (Date) getLastConnectionToServer.invoke(userDB);
                    double temperature = (double) getTemperature.invoke(userDB);
                    double pressure = (double) getPressure.invoke(userDB);
                    // vytvoření User objektu s hodnotami klienta z DB
                    SensorInformation information = new SensorInformation(temperature, pressure);
                    User user = new User(id, latitude, longitude, online, actualState, futureState,
                            firstConnectionToServer, lastConnectionToServer, information);
                    // vložení klienta do listu
                    clientsFromOfflineLibrary.add(user);
                }
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "Nepodařilo se nalézt metody třídy User offline knihovny: ");
                e.printStackTrace();
            } catch (IllegalAccessException | InvocationTargetException e) {
                Log.e(TAG, "Nepodařilo se spustit metody na objektu User offline knihovny: ");
                e.printStackTrace();
            }
        }
        this.clients = clientsFromOfflineLibrary;
    }

    public List<?> getUsersFromDB() {
        return usersFromDB;
    }

    public void setUsersFromDB(List<?> usersFromDB) {
        this.usersFromDB = usersFromDB;
    }

    public List<SensorInformation> getSensorInformationList() {
        return sensorInformationList;
    }

    public IsCentralServerApi getIsCentralServerApi() {
        return isCentralServerApi;
    }

    public int getApplicationState() {
        return applicationState;
    }

    public void setApplicationState(int applicationState) {
        this.applicationState = applicationState;
    }

    public List<Object> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<Object> libraries) {
        this.libraries = libraries;
    }

    public DatabaseHelper getMyDb() {
        return myDb;
    }

    public SharedPreferences getSharedPref() {
        return sharedPref;
    }
}
