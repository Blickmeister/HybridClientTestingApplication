package cz.fim.uhk.thesis.hybrid_client_test_app.helper.modularity;

public interface LibraryLoaderInterface {

    int start(String path);

    int stop();

    int resume();

    int exit();

    String getDescription();
}
