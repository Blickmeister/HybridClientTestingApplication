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
import retrofit2.http.Path;

public interface IsCentralServerApi {

    @GET("users/all")
    Call<List<User>> getUsers();

    @GET("library/download/{name}")
    Call<ResponseBody> getLibraryByName(@Path("name") String libraryName);

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
