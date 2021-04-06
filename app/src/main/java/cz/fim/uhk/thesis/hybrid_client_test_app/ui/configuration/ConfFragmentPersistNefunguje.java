package cz.fim.uhk.thesis.hybrid_client_test_app.ui.configuration;

public class ConfFragmentPersistNefunguje {
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
}
