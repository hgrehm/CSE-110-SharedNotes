package edu.ucsd.cse110.sharednotes.model;

import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class NoteAPI {
    private volatile static NoteAPI instance = null;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     */
    public void echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        msg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + msg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Note getNote(String title) {
        Request request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                .method("GET", null)
                .build();
        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Gson gson = new Gson();
            JsonParser parser = new JsonParser();
            JsonObject object = (JsonObject) parser.parse(body);// response will be the json String
            Note emp = gson.fromJson(object, Note.class);
            Log.i("GET NOTE", body);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Note("title", "content");
    }

    public void putNote(Note note) {
        var requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("content", note.content)
                .addFormDataPart("updated_at", String.valueOf(note.updatedAt))
                .build();

        Request request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + note.title)
                .method("PUT", requestBody)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("PUT NOTE", body);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
