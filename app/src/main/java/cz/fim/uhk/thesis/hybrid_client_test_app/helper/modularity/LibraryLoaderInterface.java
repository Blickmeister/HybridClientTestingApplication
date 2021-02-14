package cz.fim.uhk.thesis.hybrid_client_test_app.helper.modularity;

import android.content.Context;

import java.util.List;

public interface LibraryLoaderInterface {

    int start(String path, Context context);

    int stop();

    int resume(List<?> clients);

    int exit();

    String getDescription();
}
