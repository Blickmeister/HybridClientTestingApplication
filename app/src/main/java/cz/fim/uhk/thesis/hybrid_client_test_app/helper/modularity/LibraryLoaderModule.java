package cz.fim.uhk.thesis.hybrid_client_test_app.helper.modularity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.reflect.TypeToken;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.database.DatabaseHelper;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import cz.fim.uhk.thesis.hybrid_client_test_app.ui.configuration.ConfigurationFragment;
import dalvik.system.DexClassLoader;

public class LibraryLoaderModule {

    private static final String TAG = "LibraryLoaderModule";
    private static final String libraryDownloadUrl = "http://10.0.2.2:8080/library/download/";

    private DatabaseHelper myDb;
    private Context activityContext;
    private MainActivity activity;

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
    private boolean isLibraryInDirection(String libraryName) {
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
    private void downloadLibrary(final String libraryName, final String dexPath) {
        // MainActivity mainActivity = (MainActivity) getActivity();
        // vytvoření nového vlákna pro připojení k serveru pomocí instance AsyncTask
        /*try {
            activity.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        try {
            new AsyncTask<Void, Void, Void>() {
                @SuppressLint("StaticFieldLeak")
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
                            // knihovna se zavede pokud byla úspěšně odzipována
                            isLibraryDownloadedSuccessfully = loadClass(dexPath, libraryName) == 0;
                        } else {
                            Log.e(TAG, "Nepodařilo se uložit informace o knihovně");
                        }
                        ConfigurationFragment confFragment = new ConfigurationFragment();
                        if (isLibraryDownloadedSuccessfully) {
                            Log.d(TAG, "fakci to!!");
                            confFragment.setContextByLibrary(ConfigurationFragment.getLibraryForOfflineModeCode(),
                                    true, activity, activityContext);
                        } else {
                            confFragment.setContextByLibrary(ConfigurationFragment.getLibraryForOfflineModeCode(),
                                    false, activity, activityContext);
                        }
                        //activity.notify();
                    } else {
                        Log.e(TAG, "Nepodařilo se odzipovat knihovnu");
                    }
                }
            }.execute();
        } catch (Exception ex) {
            Log.e(TAG, "Problém se získáním knihovny při stažení: " + ex.getMessage());
        }
    }

    private boolean unpackZip(DataInputStream body, String path) throws IOException {
        InputStream is = body;
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
        ;
        boolean mkdirsSuccess = false;
        try {
            String filename;
            //is = new FileInputStream(body.byteStream());
            //zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                //filename = ze.getName();
                File file = new File(path, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory" + dir.getAbsolutePath());
                if (ze.isDirectory()) continue;
                mkdirsSuccess = true;
                FileOutputStream fout = new FileOutputStream(file);

                // Need to create directories if not exists, or
                // it will generate an Exception...
                /*if (!ze.isDirectory()) {
                    File fmd = new File(path + filename);
                    mkdirsSuccess = fmd.mkdirs();
                    continue;
                }*/

                //FileOutputStream fout = new FileOutputStream(path + filename);

                try {
                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                } finally {
                    fout.close();
                }

                //fout.close();
                //zis.closeEntry();
            }

            //zis.close();
        } finally {
            zis.close();
        }
        return mkdirsSuccess;
    }

    private boolean saveLibraryInformationToDb(String dexPath, String libraryName) {
        List<String> data = new ArrayList<>();
        // získání dat z txt
        String pathToFile = dexPath + "/" + libraryName + "/" + "descriptor.txt";
        Scanner myReader = null;
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
        myDb.insertData(libraryName, data.get(1), data.get(3), data.get(4));
        return true;
    }

    // metoda pro zavedení knihovny resp. její spouštěcí třídy
    private int loadClass(String dexPath, String libraryName) {
        int returnedState = 1;
        // cesta ke konkrétní knihovně dle jejího názvu
        String completePath = dexPath + "/" + libraryName + "/" + myDb.getApkName(libraryName);
        // init loaderu pro zavedení knihovny
        DexClassLoader loader = new DexClassLoader
                (completePath, null, null, activity.getClassLoader());
        // načtení hlavní třídy knihovny
        Class<?> classToLoad = null;
        Class<?> interToLoad = null;
        try {
            //classToLoad = loader.findClass(myDb.getClassName(libraryName));
            classToLoad = Class.forName(myDb.getClassName(libraryName), true, loader);
            //interToLoad = Class.forName("cz.fim.uhk.thesis.libraryforofflinemode.User", false, loader);

        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Nepodařilo se nalézt zaváděcí třídu knihovny: ");
            e.printStackTrace();
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
        }
        if (libraryInstance != null) {
            /*try {
                Method getUsers = libraryInstance.getClass().getMethod("getClients");
                List<User> users = (List<User>) getUsers.invoke(libraryInstance);
                activity.setClients(users);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }*/


            // uložení instance knihovny TODo mozna zusnate jen persistentni ukladani tohle asi zbytecny - pri zmene na thick clienta se to ztrati stejen tak
            // TODO pri restartu appky
            //MainActivity mainActivity = activity;
            List<Object> libraries = activity.getLibraries();
            libraries.add(libraryInstance);
            activity.setLibraries(libraries);
            // TODO nefuguje asi dat pryc - mam poznamenano
            // TODO max kusit ulozit do souboru kdyz nejde json - vyplati se? nwm
            // nutné ukládat perzistentně (pro případ restartu aplikace)
            // využito SharedPref v kombinaci s Gson knihovnou (výhodou je že knihovna je již k
            // dospozici z důvodu využití u Retrofit knihovny pro komunikaci serverem)
            /*SharedPreferences.Editor editor = activity.getSharedPref().edit();
            Gson gson = new Gson();
            String json = gson.toJson(libraries);
            editor.putString(activityContext.getString(R.string.sh_pref_libraries), json);
            editor.apply();*/
            /*try {
                FileOutputStream fileOutputStream = new FileOutputStream(dexPath + "libraries_instances");
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(libraries);
                objectOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                FileInputStream fileInputStream = new FileInputStream(dexPath + "libraries_instances");
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                libraries = (List<Object>) objectInputStream.readObject();
                objectInputStream.close();
            } catch (IOException | ClassNotFoundException e){
                e.printStackTrace();
            }*/
            // pokus o xml taky nejde
            /*Serializer serializer = new Persister();
            File result = new File(dexPath + "libraries_instances.xml");
            try {
                serializer.write(libraryInstance, result);
            } catch (Exception e) {
                e.printStackTrace();
            }
            File file = new File(dexPath + "libraries_instances.xml");
            try {
                Object lib = serializer.read(Object.class, file);
                Method start = lib.getClass().getMethod("start", String.class, Context.class);
                int res = (int) start.invoke(lib, dexPath, activityContext);
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            // update informace o knihovně do db
            // TODO uz asi nic nebude
            // zavedení knihovny skrze metodu start() a získání klientů z DB offline knihovny (již nikoliv ze serveru)
            // pomocí metody getUsers()
            try {
                // zavedení knihovny
                Method start = libraryInstance.getClass().getMethod("start", String.class, Context.class);
                int res = (int) start.invoke(libraryInstance, dexPath, activityContext);

                // jedině výstup 0 při startu knihovny znamená úspěšné nastartování
                if(res == 0) {
                    // zaslání seznamu klientů ze serveru do DB offline knihovny
                    if (libraryName.equals(ConfigurationFragment.getLibraryNames()[0])) {
                        try {
                            // po zavedení knihovny nutné předat seznam klientů ze serveru - metoda addUser(parametry klienta)
                            Method addUser = libraryInstance.getClass().getMethod("addUser", String.class, Double.class,
                                    Double.class, Boolean.class, String.class, String.class, Date.class, Date.class, Double.class,
                                    Double.class);

                            List<User> clientsFromServer = activity.getClients();
                            if (clientsFromServer != null) {
                                for (User user : clientsFromServer) {
                                    int result = (int) addUser.invoke(libraryInstance, user.getSsid(),
                                            user.getLatitude(), user.getLongitude(), user.isOnline(),
                                            user.getActualState(), user.getFutureState(), user.getFirstConnectionToServer(),
                                            user.getLastConnectionToServer(), user.getSensorInformation().getTemperature(),
                                            user.getSensorInformation().getPressure());
                                    if(result != 0) {
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
                        } catch (IllegalAccessException | InvocationTargetException ex) {
                            Log.e(TAG, "Nepodařilo se předat seznam klientů do offline knihovny: ");
                            ex.printStackTrace();
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
                    }

                    // TODO získání klientů z DB knihovny a premistit na mista kde je treba - mainActivita v create pri appstate = 1 a 2
                    // TODO vlastni metoda pro to v mainAct - pouzit k tomu libraryInstance ulozenou v seznamu libraries v mainAct
                    // TODO nwm nekdy de nekdy ne - taky problem v tom jak se nemaze DB ze se vklada 2x stejny user se stejnym ID - poruseni
                    if (libraryName.equals(ConfigurationFragment.getLibraryNames()[0])) {
                        // získání klientů z DB offline knihovny
                        Method getUsers = libraryInstance.getClass().getMethod("getUsers");
                        List<?> usersFromDB = (List<?>) getUsers.invoke(libraryInstance);
                        //activity.setUsersFromDB(usersFromDB);
                        activity.setClientsFromUsersInDB(usersFromDB, loader);
                        // otestování přítomnosti klientů v aplikaci
                        for(User us : activity.getClients()) {
                            Log.d(TAG, "Uživatel ID: " + us.getSsid());
                        }
                    }
                    // podařilo se knihovnu úspěšně zavést
                    returnedState = 0;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                Log.e(TAG, "Nepodařilo se načíst metody pro zavedení knihovny: " + e.getMessage());
            }
        } else {
            Toast.makeText(activityContext, "Knihovnu se nepodařilo zavést", Toast.LENGTH_SHORT).show();
        }
        return returnedState;
    }
}
