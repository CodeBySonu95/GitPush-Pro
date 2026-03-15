package com.codebysonu.gitpushpro;

import android.content.Context;
import android.net.Uri;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class GitHubHelper {
    
    public interface GitCallback {
        void onUpdate(String message);
        void onProgress(int percent);
        void onComplete();
        void onError(String error);
    }
    
    private final OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(120, TimeUnit.SECONDS)
    .writeTimeout(120, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)
    .build();
    
    private Context context;
    private String token, repo, folder, branch;
    private GitCallback callback;
    private long totalBytesQueue = 0;
    private long alreadyUploadedBytes = 0;
    
    public GitHubHelper(Context context, String token, String repo, String folder, String branch, GitCallback callback) {
        this.context = context;
        this.token = token.replace("\"", "").trim();
        this.repo = repo.trim();
        this.branch = branch.trim();
        this.callback = callback;
        this.folder = (folder != null) ? folder.replaceAll("^/+|/+$", "").trim() : "";
    }
    
    public void startPush(ArrayList<HashMap<String, Object>> listMap) {
        new Thread(() -> {
            try {
                callback.onUpdate("[$] Initializing SketchCode Engine...");
                callback.onUpdate("[$] Repository: " + repo);
                callback.onUpdate("[$] Target Branch: " + branch);
                
                totalBytesQueue = 0;
                alreadyUploadedBytes = 0;
                for (HashMap<String, Object> map : listMap) {
                    totalBytesQueue += getFileSize(Uri.parse((String) map.get("uri")));
                }
                
                for (int i = 0; i < listMap.size(); i++) {
                    HashMap<String, Object> fileData = listMap.get(i);
                    String fileName = (String) fileData.get("name");
                    Uri fileUri = Uri.parse((String) fileData.get("uri"));
                    long currentFileOriginalSize = getFileSize(fileUri);
                    
                    callback.onUpdate("[$] Staging: " + fileName);
                    
                    
                    String encodedFolder = folder.isEmpty() ? "" : Uri.encode(folder, "/");
                    String encodedFileName = Uri.encode(fileName);
                    String fullPath = encodedFolder.isEmpty() ? encodedFileName : encodedFolder + "/" + encodedFileName;
                    
                    String apiUrl = "https://api.github.com/repos/" + repo + "/contents/" + fullPath;
                    callback.onUpdate("[$] URL: " + apiUrl);
                    
                    callback.onUpdate("[$] Syncing SHA (Checking remote...)");
                    String sha = getFileSha(fullPath);
                    
                    callback.onUpdate("[$] Uploading data stream...");
                    boolean success = performStreamingUpload(apiUrl, fileUri, fileName, sha, currentFileOriginalSize);
                    
                    if (success) {
                        alreadyUploadedBytes += currentFileOriginalSize;
                        callback.onUpdate("[$] OK: " + fileName + " is live.");
                    } else {
                        throw new Exception("Push Failed for: " + fileName);
                    }
                }
                callback.onComplete();
            } catch (Exception e) {
                callback.onUpdate("[$] FATAL: " + e.toString());
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    private String getFileSha(String fullPath) throws IOException {
        String url = "https://api.github.com/repos/" + repo + "/contents/" + fullPath + "?ref=" + branch;
        Request request = new Request.Builder()
        .url(url)
        .header("Authorization", "token " + token)
        .header("User-Agent", "SketchCode-App")
        .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JSONObject json = new JSONObject(response.body().string());
                return json.getString("sha");
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    private boolean performStreamingUpload(String url, Uri uri, String name, String sha, long fileSize) throws Exception {
        GitHubStreamRequestBody streamBody = new GitHubStreamRequestBody(context, uri, "Commit: " + name, branch, sha, (bytesWritten, totalFileBytes) -> {
            if (totalBytesQueue == 0) return;
            long totalPushedSoFar = alreadyUploadedBytes + bytesWritten;
            int totalProgress = (int) ((totalPushedSoFar * 100) / totalBytesQueue);
            if (totalProgress > 100) totalProgress = 100;
            callback.onProgress(totalProgress);
        });
        
        Request request = new Request.Builder()
        .url(url)
        .put(streamBody)
        .header("Authorization", "token " + token)
        .header("User-Agent", "SketchCode-App")
        .build();
        
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful() || response.code() == 422;
        }
    }
    
    private long getFileSize(Uri uri) {
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(android.provider.OpenableColumns.SIZE));
            }
        } catch (Exception e) {}
        return 0;
    }
}
