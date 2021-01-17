package cz.fim.uhk.thesis.hybrid_client_test_app.ui.configuration;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.R;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.database.DatabaseHelper;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.modularity.LibraryLoaderModule;

public class ConfigurationFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "MainAct/ConfigFrag";
    private static final String[] libraryNames = {"LibraryForOfflineMode", ""};

    private TextView textView;
    private Button btnGetFatOffline;
    private Button btnGetFatP2p;
    private Button btnSlim;
    private Button btnShowClients;
    private RadioGroup radioGroup;

    private DatabaseHelper myDb;
    private MainActivity mainActivity;

    //private boolean isLibraryDownloadedSuccessfully = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_conf, container, false);
        textView = root.findViewById(R.id.text_conf);
        textView.setText("Nastavení konfigurace aplikace");
        btnGetFatOffline = root.findViewById(R.id.btn_getFat_offline);
        btnGetFatP2p = root.findViewById(R.id.btn_getFat_p2p);
        btnSlim = root.findViewById(R.id.btn_slim);
        btnShowClients = root.findViewById(R.id.btn_show_clients);
        radioGroup = root.findViewById(R.id.radioGroup_conf);

        mainActivity = (MainActivity) getActivity();
        myDb = mainActivity.getMyDb();

        // přesměrování na fragment zobrazující seznam klientů ze serveru
        btnShowClients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*FragmentManager fragmentManager = Objects.requireNonNull(getActivity()).getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.content_main, new UsersFragment());
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();*/
                Navigation.findNavController(getActivity(), R.id.nav_host_fragment).navigate(R.id.nav_users);

            }
        });

        btnGetFatOffline.setOnClickListener(this);

        // listener pro radio group ovládající nastavení konfigurace aplikace
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId udržuje aktivní btn

                switch (checkedId) {
                    case R.id.radio_thin:
                        // aplikace se bude chovat jako tenký klient
                        mainActivity.setApplicationState(1);
                        textView.setText("thin");
                        break;
                    case R.id.radio_hybrid_offline:
                        // aplikace se bude chovat jako tenký klient s možností ztloustnout a fungovat offline
                        mainActivity.setApplicationState(2);
                        textView.setText("offline");
                        break;
                    case R.id.radio_hybrid_p2p:
                        // aplikace se bude chovat jako tenký klient s možností ztloustnout a poskytnout službu okolním klientům
                        mainActivity.setApplicationState(3);
                        textView.setText("p2p");
                        break;
                }
            }
        });
        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_getFat_offline:
                // zavedení knihovny pro offline režim pomocí modulu pro dynamické zavedení knihoven
                LibraryLoaderModule libraryLoaderModule = new LibraryLoaderModule(myDb, getContext(), getActivity());
                libraryLoaderModule.loadLibrary(libraryNames[0]);
                break;
            case R.id.btn_getFat_p2p:
                // p2p obsluha
                break;
        }
    }

    // kompletní proces kontroly stavu, stažení, odzipování a dynamického zavedení knihovny
    /*private Object loadLibrary(String dexPath, String libraryName) {
        // kontrola zda je knihovna uz v ulozisti pres zaznam v db
        if (isLibraryInDirection(libraryName)) {
            // pokud ano tak se zavede
            return loadClass(dexPath, libraryName);
        } else {
            // pokud ne tak stažení ze serveru, unzip a uložení do uložiště
            if (downloadLibrary(libraryName, dexPath)) {
                // přečtení infa o knihovně z deskriptoru knihovny a následné uložení do db
                if (saveLibraryInformationToDb(dexPath, libraryName)) {
                    // knihovna se zavede
                    return loadClass(dexPath, libraryName);
                }
            }
        }
        return null; // TODO mozna nechat
    }*/

    /*private boolean saveLibraryInformationToDb(String dexPath, String libraryName) {
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
    }*/

    // metoda pro stažení dané knihovny ze serveru
    /*@SuppressLint("StaticFieldLeak")
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
                        Toast.makeText(getContext(), "Nepodařilo se stáhnout knihovnu ze serveru: "
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
        // request
        /*Call<ResponseBody> call = mainActivity.getIsCentralServerApi().getLibraryByName(libraryName);
        // zpracování response
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    Log.d(TAG, "Nepodařilo se stáhnout knihovnu ze serveru: " + response.message());
                    Toast.makeText(getContext(), "Nepodařilo se stáhnout knihovnu ze serveru: "
                                    + response.getClass(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if(Debug.isDebuggerConnected()) Debug.waitForDebugger();
                    unpackZip(response.body(), dexPath + File.pathSeparator + libraryName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isLibraryDownloadedSuccessfully = true;
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "Nepodařilo se stáhnout knihovnu ze serveru: " + t.getMessage());
            }
        });
        return isLibraryDownloadedSuccessfully;
    }*/

    /*private boolean writeResponseBodyToDisk(ResponseBody body, String path, String libraryName) {
        try {
            String completePath = path + File.pathSeparator + libraryName;
            File downloadedLibrary = new File(completePath);
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                byte[] fileReader = new byte[4096];
                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(downloadedLibrary);

                while (true) {
                    int read = inputStream.read(fileReader);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;
                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }
                outputStream.flush();
                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }*/

    /*private boolean unpackZip(DataInputStream body, String path) throws IOException {
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
                }

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
    }*/

    // metoda pro kontrolu zda je knihovna už v úložišti preš seznam knihoven v db
    /*private boolean isLibraryInDirection(String libraryName) {
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
    }*/

    // metoda pro zavedení knihovny resp. její spouštěcí třídy
    /*private Object loadClass(String dexPath, String libraryName) {
        // cesta ke konkrétní knihovně dle jejího názvu
        String completePath = dexPath + "/" + libraryName + "/" + myDb.getApkName(libraryName);
        // init loaderu pro zavedení knihovny
        DexClassLoader loader = new DexClassLoader
                (completePath, null, null, getActivity().getClassLoader());
        // načtení hlavní třídy knihovny
        Class<?> classToLoad = null;
        try {
            classToLoad = Class.forName("cz.fim.uhk.thesis.libraryforofflinemode." + myDb.getClassName(libraryName), true, loader);
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
    }*/
}