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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.R;
import cz.fim.uhk.thesis.hybrid_client_test_app.helper.database.DatabaseHelper;
import cz.fim.uhk.thesis.hybrid_client_test_app.modularity.LibraryLoaderModule;

public class ConfigurationFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "MainAct/ConfigFrag";
    // názvy použitých knihoven
    private static final String[] libraryNames = {"LibraryForOfflineMode",
            "LibraryForP2PCommunication"};

    private TextView textView;
    private Button btnGetFatOffline;
    private Button btnGetFatP2pServer;
    private Button btnGetFatP2pClient;
    private Button btnSlim;
    private Button btnShowClients;
    private TextView txtAppState;
    private ProgressBar progressBar;
    private RadioGroup radioGroup;

    private DatabaseHelper myDb;
    private MainActivity mainActivity;
    private Context context;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_conf, container, false);
        textView = root.findViewById(R.id.text_conf);
        btnGetFatOffline = root.findViewById(R.id.btn_getFat_offline);
        btnGetFatP2pServer = root.findViewById(R.id.btn_getFat_p2p_server);
        btnGetFatP2pClient = root.findViewById(R.id.btn_getFat_p2p_client);
        btnSlim = root.findViewById(R.id.btn_slim);
        btnShowClients = root.findViewById(R.id.btn_show_clients);
        txtAppState = root.findViewById(R.id.txt_conf_state_to_set);
        progressBar = root.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

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
    @SuppressLint("StaticFieldLeak")
    // problém AsyncTask -> měl by být implementován jako statická třída
    // to může způsobit komplikace (dala by se využít konstrukce WeakReference)
    // nicméně v mnoha názorech a tutoriálech je toto upozornění ignorováno a nemělo by být nebezpečné
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
                        Method exit = p2pLibraryInstance.getClass().getMethod("exit");
                        int res = (int) exit.invoke(p2pLibraryInstance);
                       // int res = 0; // TODo pryc
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
                        }
                    }
                    // je-li aplikace v roli tlustého klienta s offline režimem
                    else if (state == MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE) {
                        // odpojení knihovny
                        Object offlineLibraryInstance = libraries
                                .get(MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION);
                        Method exit = offlineLibraryInstance.getClass().getMethod("exit");
                        int res = (int) exit.invoke(offlineLibraryInstance);

                        // pokud se podaří uvolnit offline knihovnu
                        if (res == 0) {
                            // změna kontextu aplikace -> tenký klient
                            setContextByLibrary(MainActivity.LIBRARY_FOR_ONLINE_MODE_CODE,
                                    true, mainActivity, context);
                            // odstranění offline knihovny ze seznamu knihoven
                            libraries.remove(MainActivity.LIBRARY_FOR_OFFLINE_MODE_POSITION);
                            mainActivity.setLibraries(libraries);
                        }
                    }
                        // refresh main activity
                        /*Intent intent = mainActivity.getIntent();
                        intent.putExtra("")
                        mainActivity.finish();
                        startActivity(intent);*/
                    // vždy refresh Main Activity
                        mainActivity.recreate();
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.getCause().printStackTrace();
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
            txtAppState = (TextView) ((MainActivity) context).findViewById(R.id.txt_conf_state_to_set);
            btnGetFatOffline = (Button) ((MainActivity) context).findViewById(R.id.btn_getFat_offline);
            btnGetFatP2pServer = (Button) ((MainActivity) context).findViewById(R.id.btn_getFat_p2p_server);
            btnGetFatP2pClient = (Button) ((MainActivity) context).findViewById(R.id.btn_getFat_p2p_client);
            btnSlim = (Button) ((MainActivity) context).findViewById(R.id.btn_slim);
        }
        switch (libraryType) {
            case MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE:
                if (isLoadSuccessful) {
                    // nastavení GUI
                    setGUI(MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE);
                    // nastavení kontextu -> řídící proměnné stavu aplikace
                    editor.putInt(context.getString(R.string.sh_pref_app_state), libraryType);
                    activity.setApplicationState(MainActivity.LIBRARY_FOR_OFFLINE_MODE_CODE);
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
                    activity.setApplicationState(MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE);
                    // nastavení kontextu -> řídící proměnné stavu aplikace
                    editor.putInt(getContext().getString(R.string.sh_pref_app_state), libraryType);
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
                    setGUI(MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE);
                    activity.setApplicationState(MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE);
                    // nastavení kontextu -> řídící proměnné stavu aplikace
                    editor.putInt(getContext().getString(R.string.sh_pref_app_state), libraryType);
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
                    setGUI(MainActivity.LIBRARY_FOR_ONLINE_MODE_CODE);
                    // nastavení kontextu -> řídící proměnné stavu aplikace
                    editor.putInt(getContext().getString(R.string.sh_pref_app_state), libraryType);
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
                txtAppState.setText("Tlustý klient s offline režimem");
                btnGetFatOffline.setEnabled(false);
                btnGetFatP2pServer.setEnabled(true);
                btnGetFatP2pClient.setEnabled(false);
                btnSlim.setEnabled(true);
                break;
            case MainActivity.LIBRARY_FOR_P2P_SERVER_MODE_CODE:
                txtAppState.setText("Tlustý klient s peer-to-peer režimem v roli serveru");
                btnGetFatOffline.setEnabled(false);
                btnGetFatP2pServer.setEnabled(false);
                btnGetFatP2pClient.setEnabled(false);
                btnSlim.setEnabled(true);
                break;
            case MainActivity.LIBRARY_FOR_P2P_CLIENT_MODE_CODE:
                txtAppState.setText("Hybridní klient s peer-to-peer režimem v roli klienta");
                btnGetFatOffline.setEnabled(true);
                btnGetFatP2pServer.setEnabled(true);
                btnGetFatP2pClient.setEnabled(false);
                btnSlim.setEnabled(true);
                break;
        }
    }

    public static String[] getLibraryNames() {
        return libraryNames;
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