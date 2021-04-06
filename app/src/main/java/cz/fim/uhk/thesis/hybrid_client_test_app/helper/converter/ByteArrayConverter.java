package cz.fim.uhk.thesis.hybrid_client_test_app.helper.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import cz.fim.uhk.thesis.hybrid_client_test_app.model.P2PClientContext;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;

public class ByteArrayConverter {

    public static List<User> byteArrayToUserList(byte[] array) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(array);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            return (List<User>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] userListToByteArray(List<User> userList) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(userList);
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static User byteArrayToClientContext(byte[] array) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(array);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            return (User) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] clientContextToByteArray(User clientContext) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(clientContext);
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
