package cordova.plugin.service;

import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {
    //Keep the post unsuffixed, i'll add the /shake in the url myself
    //change the name to ServiceAPI
    //remove this comment when seen
    @POST("/")
    Call<ResponseBody> postData(@Body HashMap<String, String> data );
}
