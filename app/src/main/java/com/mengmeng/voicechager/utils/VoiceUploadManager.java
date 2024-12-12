package com.mengmeng.voicechager.utils;

import com.mengmeng.voicechager.api.ApiClient;
import com.mengmeng.voicechager.models.VoiceListResponse;
import com.mengmeng.voicechager.models.VoiceResponse;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import java.io.File;

public class VoiceUploadManager {
    private static final String DEFAULT_SPEAKER = "speaker_1"; // 默认音色
    private static final String DEFAULT_LANGUAGE = "zh"; // 默认语言

    public interface CloneCallback {
        void onSuccess(String audioUrl, String taskId);
        void onFailure(String error);
    }

    public interface SpeakerListCallback {
        void onSuccess(VoiceListResponse.Speaker[] speakers);
        void onFailure(String error);
    }

    public void cloneVoice(File audioFile, String text, CloneCallback callback) {
        try {
            RequestBody audioRequestBody = RequestBody.create(
                MediaType.parse("audio/*"), 
                audioFile
            );
            
            MultipartBody.Part audioPart = MultipartBody.Part.createFormData(
                "audio", 
                audioFile.getName(), 
                audioRequestBody
            );

            ApiClient.getVoiceService().cloneVoice(
                ApiClient.getAuthorizationHeader(),
                audioPart,
                DEFAULT_SPEAKER,
                text,
                DEFAULT_LANGUAGE
            ).enqueue(new Callback<VoiceResponse>() {
                @Override
                public void onResponse(Call<VoiceResponse> call, retrofit2.Response<VoiceResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        VoiceResponse.AudioData data = response.body().getData();
                        if (data != null) {
                            callback.onSuccess(data.getAudioUrl(), data.getTaskId());
                        } else {
                            callback.onFailure("No data in response");
                        }
                    } else {
                        callback.onFailure("Clone failed: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<VoiceResponse> call, Throwable t) {
                    callback.onFailure("Clone error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onFailure("Clone error: " + e.getMessage());
        }
    }

    public void getSpeakerList(SpeakerListCallback callback) {
        ApiClient.getVoiceService().getSpeakerList(ApiClient.getAuthorizationHeader())
            .enqueue(new Callback<VoiceListResponse>() {
                @Override
                public void onResponse(Call<VoiceListResponse> call, retrofit2.Response<VoiceListResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body().getData().toArray(new VoiceListResponse.Speaker[0]));
                    } else {
                        callback.onFailure("Failed to get speaker list: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<VoiceListResponse> call, Throwable t) {
                    callback.onFailure("Error getting speaker list: " + t.getMessage());
                }
            });
    }
}