package cz.fim.uhk.thesis.hybrid_client_test_app;

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
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.database.DatabaseHelper;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.modularity.LibraryLoaderInterface;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.SensorInformation;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.os.Handler;
import android.view.Menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private List<User> clients;
    private List<SensorInformation> sensorInformationList;
    private Timer myTimer;
    private IsCentralServerApi isCentralServerApi;
    // proměnná vyjadřující aktuální stav aplikace:
    // 0 -> tenký klient
    // 1 -> hybridní klient s offline režimem
    // 2 -> hybridní klient s p2p komunikací
    private int applicationState;
    // kolekce instancí knihoven
    private List<LibraryLoaderInterface> libraries = new ArrayList<>();
    private DatabaseHelper myDb;

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
        myTimer = new Timer();
        // opakování volání metody získání seznamu klientů ze serveru každou minutu
        /*final GetClientsTimerTask getClientsTimerTask = new GetClientsTimerTask(isCentralServerApi);
        myTimer.scheduleAtFixedRate(getClientsTimerTask, 0, 60000);
        // TODO získání výsledků
        myTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                clients = getClientsTimerTask.getClientsFromServer();
                String message = getClientsTimerTask.getAlertMessage();
                System.out.println("ALERT MESSAGE: " + message);
            }
        }, 10, 60000);*/

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

    public List<LibraryLoaderInterface> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<LibraryLoaderInterface> libraries) {
        this.libraries = libraries;
    }

    public DatabaseHelper getMyDb() {
        return myDb;
    }
}
