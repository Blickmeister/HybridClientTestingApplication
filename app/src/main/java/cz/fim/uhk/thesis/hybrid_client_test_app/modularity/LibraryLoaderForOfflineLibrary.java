package cz.fim.uhk.thesis.hybrid_client_test_app.modularity;

import java.util.List;

import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;

public interface LibraryLoaderForOfflineLibrary extends LibraryLoaderInterface {
    List<User> getUsers();

    void actualizeDatabase(List<?> clients);
}
