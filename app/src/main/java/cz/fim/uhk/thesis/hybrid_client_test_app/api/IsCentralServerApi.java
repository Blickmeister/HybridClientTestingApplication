package cz.fim.uhk.thesis.hybrid_client_test_app.api;

import java.util.List;

import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Součást komunikačního modulu
 * Definice požadavků na centrální server pro knihovnu Retrofit 2
 */
public interface IsCentralServerApi {

    @GET("users/all")
    Call<List<User>> getUsers();

    @Headers("Content-Type: text/plain")
    @POST("connection/test")
    Call<ResponseBody> makeTest(@Body RequestBody body);

    @Headers("Content-Type: application/json")
    @PUT("users/update")
    Call<ResponseBody> updateUser(@Body User user);

    @Headers("Content-Type: application/json")
    @POST("users/create")
    Call<ResponseBody> createUser(@Body User user);

}
