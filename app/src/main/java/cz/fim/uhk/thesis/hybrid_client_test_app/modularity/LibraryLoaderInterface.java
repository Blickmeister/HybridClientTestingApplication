package cz.fim.uhk.thesis.hybrid_client_test_app.modularity;

import android.content.Context;

public interface LibraryLoaderInterface {

    int start(String path, Context context);

    int stop();

    int resume();

    int exit();

    String getDescription();
}
