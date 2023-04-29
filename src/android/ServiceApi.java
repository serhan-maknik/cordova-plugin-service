package cordova.plugin.service;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;

import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface ServiceApi {


     @POST
    Call<ResponseBody> postData(@Url String url, @HeaderMap Map<String, String> header, @Body HashMap<String, String> data );
}
