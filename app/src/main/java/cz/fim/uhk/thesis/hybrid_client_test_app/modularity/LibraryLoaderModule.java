
package cz.fim.uhk.thesis.hybrid_client_test_app.modularity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.R;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.converter.ByteArrayConverter;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.database.DatabaseHelper;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.SensorInformation;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import cz.fim.uhk.thesis.hybrid_client_test_app.ui.configuration.ConfigurationFragment;
import dalvik.system.DexClassLoader;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Modul pro dynamické zavádění externích knihoven
 */
public class LibraryLoaderModule {

    private static final String TAG = "LibraryLoaderModule";
    // localhost alias pro AVD - TODO ODKOMENTOVAT PRO EMULATOR
    private static final String libraryDownloadUrl = "http://10.0.2.2:8080/library/download/";
    //private static final String libraryDownloadUrl = "http://192.168.2.111:8080/library/download/";
    // název třídy reprezentující klienta v offline knihovně
    private final static String OFFLINE_LIBRARY_USER_CLASS_NAME = "cz.fim.uhk.thesis.libraryforofflinemode.model.User";
    // seznamu názvů metod třídy User offline knihovny
    private final static String[] USER_METHOD_NAMES = {"getSsid", "getLatitude", "getLongitude",
            "isOnline", "getActualState", "getFutureState", "getFirstConnectionToServer", "getLastConnectionToServer",
            "getTemperature", "getPressure"};

    private final DatabaseHelper myDb;
    private final Context activityContext;
    private final MainActivity activity;
    private DexClassLoader loader;
    private int p2pLibraryRole;

    private boolean isLibraryDownloadedSuccessfully = false;

    public LibraryLoaderModule(DatabaseHelper db, Context context, MainActivity activity) {
        this.myDb = db;
        this.activityContext = context;
        this.activity = activity;
    }

    // kompletní proces kontroly stavu, stažení, odzipování a dynamického zavedení knihovny
    // návrat 0 -> knihovna zavedena bez stažení bez chyby
    // návrat 1 -> knihovna nezavedena bez stažení -> chyba
    // návrat 2 -> knihovna zavedena se stažením -> jiný postup, mj nutnost čekat na AsyncTask
    public int loadLibrary(String libraryName) {
        int loadState;
        ContextWrapper contextWrapper = new ContextWrapper(activityContext);
        String dexPath = contextWrapper.getFilesDir() + "/" + "libs";
        // kontrola zda je knihovna uz v ulozisti pres zaznam v db
        if (isLibraryInDirection(libraryName)) {
            // pokud ano tak se zavede
            loadState = loadClass(dexPath, libraryName);
        } else {
            // pokud ne tak stažení ze serveru, unzip a uložení do uložiště
            // nelze uplatnit isLoadSuccess proměnnou -> AsyncTask
            downloadLibrary(libraryName, dexPath);
            loadState = 2;
        }
        return loadState;
    }

    // metoda pro kontrolu zda je knihovna už v úložišti preš seznam knihoven v db
    public boolean isLibraryInDirection(String libraryName) {
        // získání dat z databáze
        Cursor result = myDb.getAllData();
        if (result.getCount() == 0) {
            return false;
        }
        // získání názvů knihoven v seznamu
        List<String> libraryNames = new ArrayList<>();
        while (result.moveToNext()) {
            libraryNames.add(result.getString(1));
        }
        // kontrola zda daná knihovna je již v seznamu, tudíž v uložišti
        for (String name : libraryNames) {
            if (name.equals(libraryName)) {
                return true;
            }
        }
        return false;
    }

    // metoda pro stažení dané knihovny ze serveru
    // metoda sama nezávisle vyhodnocuje zda se podařilo zavést knihovnu či nikoliv
    // je nezávislá na procesu zavedení v případě, kdy není nutné knihovnu stahovat
    @SuppressLint("StaticFieldLeak")
    public void downloadLibrary(final String libraryName, final String dexPath) {
        // vytvoření nového vlákna pro připojení k serveru pomocí instance AsyncTask
        try {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        // požadavek na server
                        URL url = new URL(libraryDownloadUrl + libraryName);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(10000);
                        connection.connect();
                        // je-li response ze serveru v pořádku
                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            Log.d(TAG, "TUUUUU");
                            // uložení response body a následný unzip a uložení souborů do uložiště
                            DataInputStream stream = new DataInputStream(url.openStream());
                            if (unpackZip(stream, dexPath)) isLibraryDownloadedSuccessfully = true;
                        } else {
                            // pokud response značí chybu
                            isLibraryDownloadedSuccessfully = false;
                            Toast.makeText(activityContext, "Nepodařilo se stáhnout knihovnu ze serveru: "
                                            + " Kód a chyba: " + connection.getResponseCode()
                                            + " " + connection.getResponseMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (MalformedURLException e) {
                        isLibraryDownloadedSuccessfully = false;
                        Log.d(TAG, "problém s připojením k " + libraryDownloadUrl
                                + " chybová hláška: " + e.getMessage());
                    } catch (IOException e) {
                        Log.d(TAG, "nepodařilo se získat data z: " + libraryDownloadUrl
                                + " chybová hláška: " + e.getMessage());
                        isLibraryDownloadedSuccessfully = false;
                    }
                    Log.d(TAG, "Je knihovna stažena a odzipována? " + isLibraryDownloadedSuccessfully);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    // dělení chování dle úspěšnosti získání a zavedení knihovny
                    if (isLibraryDownloadedSuccessfully) {
                        // přečtení infa o knihovně z deskriptoru knihovny a následné uložení do db
                        if (saveLibraryInformationToDb(dexPath, libraryName)) {
                            // pokud chceme pouze stáhnout p2p knihovnu - nikoliv zavést
                            if (libraryName.equals(ConfigurationFragment.getLibraryNames()
                                    [MainActivity.LIBRARY_FOR_P2P_MODE_POSITION]) &&
                                    p2pLibraryRole == MainActivity.DOWNLOAD_ONLY_P2P_LIBRARY_VALUE) {
                                return;
                            }
                            // knihovna se zavede pokud byla úspěšně odzipována
                            // a byly úspěšně uloženy informace o knihovně
                            isLibraryDownloadedSuccessfully = loadClass(dexPath, libraryName) == 0;
                        } else {
                            Log.e(TAG, "Nepodařilo se uložit informace o knihovně");
                        }
                        ConfigurationFragment confFragment = new ConfigurationFragment();
                        // nastavení kontextu a GUI aplikace dle názvu zavedené knihovny
                        if (isLibraryDownloadedSuccessfully) {
                            // pokud se podaří zavést offline knihovnu
                            if (libraryName.equals(ConfigurationFragment.getLibraryNames()
                                    [MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION])) {
                                Log.d(TAG, "knihovna úspěšně stažena");
                                confFragment.setContextByLibrary(MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE,
                                        true, activity, activityContext);

                            } else if (libraryName.equals(ConfigurationFragment.getLibraryNames()
                                    [MainActivity.LIBRARY_FOR_P2P_MODE_POSITION])
                                    && p2pLibraryRole == MainActivity.RUN_P2P_LIBRARY_AS_SERVER_VALUE) {
                                // pokud se podaří zavést p2p knihovnu v roli serveru
                                Log.d(TAG, "knihovna úspěšně stažena");
                                confFragment.setContextByLibrary(MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE,
                                        true, activity, activityContext);

                            } else if (libraryName.equals(ConfigurationFragment.getLibraryNames()
                                    [MainActivity.LIBRARY_FOR_P2P_MODE_POSITION])
                                    && p2pLibraryRole == MainActivity.RUN_P2P_LIBRARY_AS_CLIENT_VALUE) {
                                // pokud se podaří zavést p2p knihovnu v roli klienta
                                Log.d(TAG, "knihovna úspěšně stažena");
                                confFragment.setContextByLibrary(MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE,
                                        true, activity, activityContext);
                            }

                        } else {
                            // pokud se nepodaří zavést offline knihovnu
                            if (libraryName.equals(ConfigurationFragment.getLibraryNames()
                                    [MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION])) {
                                confFragment.setContextByLibrary(MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE,
                                        false, activity, activityContext);

                            } else if (libraryName.equals(ConfigurationFragment.getLibraryNames()
                                    [MainActivity.LIBRARY_FOR_P2P_MODE_POSITION])
                                    && p2pLibraryRole == MainActivity.RUN_P2P_LIBRARY_AS_SERVER_VALUE) {
                                // pokud se nepodaří zavést p2p knihovnu v roli serveru
                                confFragment.setContextByLibrary(MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE,
                                        false, activity, activityContext);

                            } else if (libraryName.equals(ConfigurationFragment.getLibraryNames()
                                    [MainActivity.LIBRARY_FOR_P2P_MODE_POSITION])
                                    && p2pLibraryRole == MainActivity.RUN_P2P_LIBRARY_AS_CLIENT_VALUE) {
                                // pokud se nepodaří zavést p2p knihovnu v roli klienta
                                confFragment.setContextByLibrary(MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE,
                                        false, activity, activityContext);
                            }
                        }
                    } else {
                        Log.e(TAG, "Nepodařilo se odzipovat či stáhnout knihovnu");
                    }
                }
            }.execute();
        } catch (Exception ex) {
            Log.e(TAG, "Problém se získáním knihovny při stažení: " + ex.getMessage());
        }
    }

    private boolean unpackZip(DataInputStream body, String path) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(body));
        boolean mkdirsSuccess = false;
        try {
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(path, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (dir != null && !dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory" + dir.getAbsolutePath());
                if (ze.isDirectory()) continue;
                mkdirsSuccess = true;

                try (FileOutputStream fout = new FileOutputStream(file)) {
                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                }
            }
        } finally {
            zis.close();
        }
        return mkdirsSuccess;
    }

    private boolean saveLibraryInformationToDb(String dexPath, String libraryName) {
        List<String> data = new ArrayList<>();
        // získání dat z txt
        String pathToFile = dexPath + "/" + libraryName + "/" + "descriptor.txt";
        Scanner myReader;
        try {
            myReader = new Scanner(new File(pathToFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        String line;
        while (myReader.hasNextLine()) {
            line = myReader.nextLine();
            String[] split = line.split(":");
            data.add(split[1]);
        }
        // uložení informací do db
        return myDb.insertData(libraryName, data.get(1), data.get(3), data.get(4));
    }

    // metoda pro zavedení knihovny resp. její spouštěcí třídy
    public int loadClass(String dexPath, String libraryName) {
        int returnedState = 1;
        // cesta ke konkrétní knihovně dle jejího názvu
        String completePath = dexPath + "/" + libraryName + "/" + myDb.getApkName(libraryName);
        // init loaderu pro zavedení knihovny
        loader = new DexClassLoader
                (completePath, dexPath, null, activity.getClass().getClassLoader());
        // načtení hlavní třídy knihovny
        Class<?> classToLoad;
        try {
            classToLoad = Class.forName(myDb.getClassName(libraryName), true, loader);

        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Nepodařilo se nalézt zaváděcí třídu knihovny: ");
            e.printStackTrace();
            return returnedState;
        }
        // vytvoření instance hlavní třídy knihovny
        Object libraryInstance = null;
        try {
            if (classToLoad != null) {
                libraryInstance = classToLoad.newInstance();
            }
        } catch (IllegalAccessException | java.lang.InstantiationException e) {
            Log.e(TAG, "Nepodařilo se vytvořit instanci zaváděcí třídy knihovny: ");
            e.printStackTrace();
            return returnedState;
        }
        // získání klientů z DB offline knihovny
        //Method getUsers = null;
        //int res = 1; // zda se povedlo knihovnu zavést skrze start metodu
        //List<?> usersFromDB = new ArrayList<>();
        if (libraryInstance != null) {
            // uložení instance knihovny dle jejího názvu
            HashMap<Integer, Object> libraries = activity.getLibraries();
            if (libraryName.equals(ConfigurationFragment.getLibraryNames()
                    [MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION])) {
                libraries.put(MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION, libraryInstance);
            } else if (libraryName.equals(ConfigurationFragment.getLibraryNames()
                    [MainActivity.LIBRARY_FOR_P2P_MODE_POSITION])) {
                libraries.put(MainActivity.LIBRARY_FOR_P2P_MODE_POSITION, libraryInstance);
            }
            activity.setLibraries(libraries);
            // zavedení knihovny skrze metodu start()
            try {
                // v případě nasazení p2p knihovny potřeba nejprve incializovat stav knihovny
                if (libraryName.equals(ConfigurationFragment.getLibraryNames()
                        [MainActivity.LIBRARY_FOR_P2P_MODE_POSITION])) {
                    initializeStartOfP2PLibrary(p2pLibraryRole, libraryInstance);
                }

                // zavedení knihovny
                Method start = libraryInstance.getClass().getMethod("start", String.class, Context.class);
                int res = (int) start.invoke(libraryInstance, dexPath, activityContext);

                // jedině výstup 0 při startu knihovny znamená úspěšné nastartování
                if (res == 0) {
                    // podařilo se knihovnu úspěšně zavést
                    returnedState = 0;
                    // v případě nasazení offline knihovny
                    if (libraryName.equals(ConfigurationFragment.getLibraryNames()
                            [MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION])) {
                        // po zavedení offline knihovny nutné předat seznam klientů ze serveru do DB
                        List<User> clientsFromServer = activity.getClients();
                        if (clientsFromServer != null) {
                            returnedState = addUsersToDB(libraryInstance, clientsFromServer);
                        }
                        // kontrola zda seznam klientů není null
                        Field usersInOfflineLibrary = null;
                        try {
                            usersInOfflineLibrary = classToLoad.getDeclaredField("users");
                        } catch (NoSuchFieldException e) {
                            Log.e(TAG, "Nepodařilo se získat atribut kolekce uživatelů z offline knihovny: ");
                            e.printStackTrace();
                        }
                        if (usersInOfflineLibrary == null) {
                            Log.e(TAG, "Atribut kolekce uživatelů z offline knihovny je null");
                        }
                        // získání klientů z DB offline knihovny
                        getClientsFromDB(libraryInstance);
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                Log.e(TAG, "Nepodařilo se načíst metody pro zavedení knihovny: " + e.getMessage());
            }
        } else {
            Toast.makeText(activityContext, "Knihovnu se nepodařilo zavést", Toast.LENGTH_SHORT).show();
        }
        return returnedState;
    }

    // metoda pro předání seznamu klientů z prostředí hybridního klienta (z centrálního serveru) do DB v offline knihovně
    public int addUsersToDB(Object libraryInstance, List<User> usersToAdd) {
        int returnedState = 1;
        try {
            // po zavedení knihovny nutné předat seznam klientů ze serveru - metoda addUser(parametry klienta)
            Method addUser = libraryInstance.getClass().getMethod("addUser", String.class, Double.class,
                    Double.class, Boolean.class, String.class, String.class, Date.class, Date.class, Double.class,
                    Double.class);

            if (usersToAdd != null) {
                for (User user : usersToAdd) {
                    if (user.getSensorInformation() != null) {
                        returnedState = (int) addUser.invoke(libraryInstance, user.getSsid(),
                                user.getLatitude(), user.getLongitude(), user.isOnline(),
                                user.getActualState(), user.getFutureState(), user.getFirstConnectionToServer(),
                                user.getLastConnectionToServer(), user.getSensorInformation().getTemperature(),
                                user.getSensorInformation().getPressure());
                    } else {
                        returnedState = (int) addUser.invoke(libraryInstance, user.getSsid(),
                                user.getLatitude(), user.getLongitude(), user.isOnline(),
                                user.getActualState(), user.getFutureState(), user.getFirstConnectionToServer(),
                                user.getLastConnectionToServer(), 0.0, 0.0);
                    }
                    if (returnedState != 0) {
                        Log.e(TAG, "Vyskytla se chyba v předvávání klientů ze serveru do DB offline knihovny");
                        return returnedState;
                    }
                }
            } else {
                Log.d(TAG, "Seznam klientů ze serveru je null");
            }
        } catch (NoSuchMethodException ex) {
            Log.e(TAG, "Nepodařilo se načíst metodu addUser() offline knihovny: ");
            ex.printStackTrace();
            return returnedState;
        } catch (IllegalAccessException | InvocationTargetException ex) {
            Log.e(TAG, "Nepodařilo se předat seznam klientů do offline knihovny: ");
            if (ex.getCause() != null) ex.getCause().printStackTrace();
            return returnedState;
        }
        return returnedState;
    }

    // metoda pro získání klientů z DB offline knihovny a nastavení seznamu klientů v aplikaci
    public void getClientsFromDB(Object libraryInstance) {
        // získání klientů z DB offline knihovny (již nikoliv ze serveru)
        // pomocí metody getUsers()
        Method getUsers = null;
        List<?> usersFromDB = new ArrayList<>();
        try {
            getUsers = libraryInstance.getClass().getMethod("getUsers");
            usersFromDB = (List<?>) getUsers.invoke(libraryInstance);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Log.e(TAG, "Nepodařilo se získat seznam klientů z DB offline knihovny");
            if (e.getCause() != null) e.getCause().printStackTrace();
        }
        if (getUsers != null && usersFromDB != null && !usersFromDB.isEmpty()) {
            setClientsFromUsersInDB(usersFromDB);
        }
    }

    // metoda pro převod seznamu klientů z DB offline knihovny na seznam klientů v aplikaci
    private void setClientsFromUsersInDB(List<?> usersFromDB) {
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
                    // nastavení časů připojení k serveru akutálnímu klientovi
                    SharedPreferences sharedPref = activity.getSharedPref();
                    String currentClientId = sharedPref.getString(activity.getString(R.string.sh_pref_ssid),
                            null);
                    if (id != null && id.equals(currentClientId)) {
                        activity.getCurrentUser().setFirstConnectionToServer(firstConnectionToServer);
                        activity.getCurrentUser().setLastConnectionToServer(lastConnectionToServer);
                    }
                }
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "Nepodařilo se nalézt metody třídy User offline knihovny: ");
                e.printStackTrace();
            } catch (IllegalAccessException | InvocationTargetException e) {
                Log.e(TAG, "Nepodařilo se spustit metody na objektu User offline knihovny: ");
                e.printStackTrace();
            }
        }
        activity.setClients(clientsFromOfflineLibrary);
    }

    // metoda pro nastavení nezbytných parametrů před spouštěním chodu p2p knihovny
    public void initializeStartOfP2PLibrary(int role, Object libraryInstance) {
        // metoda pro init p2p knihovny
        try {
            Method initialize = libraryInstance.getClass().getMethod("initialize",
                    Activity.class, int.class, byte[].class);
            // pokud chceme nasadit knihovnu v režimu serveru
            if (role == MainActivity.RUN_P2P_LIBRARY_AS_SERVER_VALUE) {
                if (activity.getClients() != null && activity.getClients().size() > 0) {
                    // převod seznamu klientů v aplikaci do byte[]
                    byte[] clientsToSend = ByteArrayConverter.userListToByteArray(activity.getClients());
                    // předání dat
                    initialize.invoke(libraryInstance, activity, MainActivity.RUN_P2P_LIBRARY_AS_SERVER_VALUE, clientsToSend);
                } else {
                    Log.d(TAG, "Zavedení p2p knihovny - role klient: Seznam klientů je prázdný");
                }
                // pokud chceme nasadit knihovnu v režimu klienta
            } else {
                // převod dat klienta v aplikaci do byte[]
                if (activity.getCurrentUser() != null) {
                    User currUser = activity.getCurrentUser();
                    SensorInformation currSensorInfo = currUser.getSensorInformation();
                    byte[] clientContextToSend;
                    if (currSensorInfo != null) {
                        clientContextToSend = ByteArrayConverter.clientContextToByteArray(new User(currUser.getSsid(),
                                currUser.getLatitude(), currUser.getLongitude(),
                                false, "p2pClient", currUser.getFutureState(),
                                currUser.getFirstConnectionToServer(), currUser.getLastConnectionToServer(),
                                new SensorInformation(currSensorInfo.getTemperature(), currSensorInfo.getPressure())));
                    } else {
                        clientContextToSend = ByteArrayConverter.clientContextToByteArray(new User(currUser.getSsid(),
                                currUser.getLatitude(), currUser.getLongitude(),
                                false, "p2pClient", currUser.getFutureState(),
                                currUser.getFirstConnectionToServer(), currUser.getLastConnectionToServer(),
                                new SensorInformation(0, 0)));
                    }
                    initialize.invoke(libraryInstance, activity, MainActivity.RUN_P2P_LIBRARY_AS_CLIENT_VALUE, clientContextToSend);
                } else {
                    Log.d(TAG, "Zavedení p2p knihovny - role klient: Žádná data o aktuálním klientovi");
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Log.e(TAG, "Nepodařilo se načíst metodu pro inicializaci chodu p2p knihovny: " + e.getMessage());
        }
    }

    // nastavení role zavedení p2p knihovny
    public void setP2pLibraryRole(int p2pLibraryRole) {
        this.p2pLibraryRole = p2pLibraryRole;
    }
}
