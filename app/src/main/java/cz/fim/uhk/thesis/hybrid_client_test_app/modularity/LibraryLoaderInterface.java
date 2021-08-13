package cz.fim.uhk.thesis.hybrid_client_test_app.modularity;

import android.content.Context;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Společné rozhraní pro řízení chodu externích knihoven - musí být implementováno každou knihovnou
 * Bylo plánováno jeho využití pro řízení chodu knihoven i uvnitř klienta
 * nicméně nepodařilo se persistentní uložení instancí knihoven (hlavních tříd) -> proto zde není použito
 */
public interface LibraryLoaderInterface {

    int start(String path, Context context);

    int stop();

    int resume();

    int exit();

    String getDescription();
}
