package cz.fim.uhk.thesis.hybrid_client_test_app.helper.converter;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Pomocná třída pro účely komunikace mezi klientem a knihovnou pro peer-to-peer komunikaci
 * konkrétně pro převod mezi polem bytů (forma zpráv při p2p komunikaci) a seznamem klientů
 */
public class ByteArrayConverter {

    private static final String TAG = "ByteArrayConverter";

    // metoda pro převod pole bytů na seznam klientů
    public static List<User> byteArrayToUserList(byte[] array) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(array);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            return (List<User>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "Chyba při převodu pole bytů na seznam klientů: ");
            e.printStackTrace();
            return null;
        }
    }

    // metoda pro převod seznamu klientů na pole bytů
    public static byte[] userListToByteArray(List<User> userList) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(userList);
            return outputStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Chyba při převodu seznamu klientů na pole bytů: ");
            e.printStackTrace();
            return null;
        }
    }

    // metoda pro převod pole bytů na kontext jednoho klienta
    public static User byteArrayToClientContext(byte[] array) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(array);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            return (User) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "Chyba při převodu pole bytů na kontext jednoho klienta: ");
            e.printStackTrace();
            return null;
        }
    }

    // metoda pro převod kontextu jednoho klienta na pole bytů
    public static byte[] clientContextToByteArray(User clientContext) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(clientContext);
            return outputStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Chyba při převodu kontextu jednoho klienta na pole bytů: ");
            e.printStackTrace();
            return null;
        }
    }
}
