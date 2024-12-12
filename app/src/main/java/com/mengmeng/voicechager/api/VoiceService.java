package com.mengmeng.voicechager.api;

import com.mengmeng.voicechager.models.VoiceListResponse;
import com.mengmeng.voicechager.models.VoiceResponse;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface VoiceService {
    @Multipart
    @POST("api/v1/audio/clone")
    Call<VoiceResponse> cloneVoice(
        @Header("Authorization") String authorization,
        @Part MultipartBody.Part audio,
        @Part("speaker") String speaker,
        @Part("text") String text,
        @Part("language") String language
    );

    @GET("api/v1/audio/speakers")
    Call<VoiceListResponse> getSpeakerList(
        @Header("Authorization") String authorization
    );
} 