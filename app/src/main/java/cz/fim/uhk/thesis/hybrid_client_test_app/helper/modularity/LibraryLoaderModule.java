package cz.fim.uhk.thesis.hybrid_client_test_app.helper.modularity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
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
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.database.DatabaseHelper;
import dalvik.system.DexClassLoader;

public class LibraryLoaderModule {

    private static final String TAG = "LibraryLoaderModule";
    private static final String libraryDownloadUrl = "http://10.0.2.2:8080/library/download/";

    private DatabaseHelper myDb;
    private Context activityContext;
    private Activity activity;

    private boolean isLibraryDownloadedSuccessfully = false;

    public LibraryLoaderModule(DatabaseHelper db, Context context, Activity activity) {
        this.myDb = db;
        this.activityContext = context;
        this.activity = activity;
    }

    // kompletní proces kontroly stavu, stažení, odzipování a dynamického zavedení knihovny
    public void loadLibrary(String libraryName) {
        ContextWrapper contextWrapper = new ContextWrapper(activityContext);
        String dexPath = contextWrapper.getFilesDir() + "/" + "libs";
        Object libraryInstance = null;
        // kontrola zda je knihovna uz v ulozisti pres zaznam v db
        if (isLibraryInDirection(libraryName)) {
            // pokud ano tak se zavede
            libraryInstance = loadClass(dexPath, libraryName);
        } else {
            // pokud ne tak stažení ze serveru, unzip a uložení do uložiště
            if (downloadLibrary(libraryName, dexPath)) {
                // přečtení infa o knihovně z deskriptoru knihovny a následné uložení do db
                if (saveLibraryInformationToDb(dexPath, libraryName)) {
                    // knihovna se zavede
                    libraryInstance = loadClass(dexPath, libraryName);
                }
            }
        }
        if (libraryInstance != null) {
            // uložení instance knihovny
            MainActivity mainActivity = (MainActivity) activity;
            List<LibraryLoaderInterface> libraries = mainActivity.getLibraries();
            libraries.add((LibraryLoaderInterface) libraryInstance);
            mainActivity.setLibraries(libraries);
            // update informace o knihovně do db
            // TODO
            // zavedení knihovny skrze metodu start()
            try {
                Method m = libraryInstance.getClass().getMethod("start", String.class);
                m.invoke(libraryInstance, dexPath);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(activityContext, "Knihovnu se nepodařilo zavést", Toast.LENGTH_SHORT).show();
        }
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
    @SuppressLint("StaticFieldLeak")
    private boolean downloadLibrary(final String libraryName, final String dexPath) {
        // MainActivity mainActivity = (MainActivity) getActivity();
        // vytvoření nového vlákna pro připojení k serveru pomocí instance AsyncTask
        new AsyncTask<Void,Void,Void>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    // požadavek na server
                    URL url = new URL(libraryDownloadUrl + libraryName);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    // je-li response ze serveru v pořádku
                    if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        // uložení response body a následný unzip a uložení souborů do uložiště
                        DataInputStream stream = new DataInputStream(url.openStream());
                        if(unpackZip(stream, dexPath)) isLibraryDownloadedSuccessfully = true;
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
                    Log.d(TAG,"problém s připojením k " + libraryDownloadUrl
                            + " chybová hláška: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "nepodařilo se získat data z: " + libraryDownloadUrl
                            + " chybová hláška: " + e.getMessage());
                    isLibraryDownloadedSuccessfully = false;
                }
                Log.d(TAG,"Je knihovna stažena a odzipována? " + isLibraryDownloadedSuccessfully);
                return null;
            }
        }.execute();
        return isLibraryDownloadedSuccessfully;
    }

    private boolean unpackZip(DataInputStream body, String path) throws IOException {
        InputStream is = body;
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));;
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
                if(!dir.isDirectory() && !dir.mkdirs()) throw new FileNotFoundException("Failed to ensure directory" + dir.getAbsolutePath());
                if(ze.isDirectory()) continue;
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
        myDb.insertData(libraryName, data.get(1), data.get(3), data.get(4), true);
        return true;
    }

    // metoda pro zavedení knihovny resp. její spouštěcí třídy
    private Object loadClass(String dexPath, String libraryName) {
        // cesta ke konkrétní knihovně dle jejího názvu
        String completePath = dexPath + "/" + libraryName + "/" + myDb.getApkName(libraryName);
        // init loaderu pro zavedení knihovny
        DexClassLoader loader = new DexClassLoader
                (completePath, null, null, activity.getClassLoader());
        // načtení hlavní třídy knihovny
        Class<?> classToLoad = null;
        try {
            classToLoad = Class.forName(myDb.getClassName(libraryName), true, loader);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        // vytvoření instance hlavní třídy knihovny
        Object classInstance = null;
        try {
            if(classToLoad != null) {
                classInstance = classToLoad.newInstance();
            }
        } catch (IllegalAccessException | java.lang.InstantiationException e) {
            e.printStackTrace();
        }
        return classInstance;
    }
}
