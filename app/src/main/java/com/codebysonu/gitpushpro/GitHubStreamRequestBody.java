package com.codebysonu.gitpushpro;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Base64OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class GitHubStreamRequestBody extends RequestBody {
    private final Context context;
    private final Uri fileUri;
    private final String message, branch, sha;
    private final ProgressListener listener;
    
    public interface ProgressListener {
        void onProgress(long bytesWritten, long totalBytes);
    }
    
    public GitHubStreamRequestBody(Context context, Uri fileUri, String message, String branch, String sha, ProgressListener listener) {
        this.context = context;
        this.fileUri = fileUri;
        this.message = message;
        this.branch = branch;
        this.sha = sha;
        this.listener = listener;
    }
    
    @Override
    public MediaType contentType() {
        return MediaType.parse("application/json; charset=utf-8");
    }
    
    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        OutputStream os = sink.outputStream();
        
        
        os.write("{\"message\":\"".getBytes());
        os.write(message.getBytes());
        os.write("\",\"branch\":\"".getBytes());
        os.write(branch.getBytes());
        if (sha != null) {
            os.write("\",\"sha\":\"".getBytes());
            os.write(sha.getBytes());
        }
        os.write("\",\"content\":\"".getBytes());
        
        long totalFileBytes = getFileSize();
        long bytesProcessed = 0;
        
        
        try (InputStream is = context.getContentResolver().openInputStream(fileUri)) {
            
            Base64OutputStream b64os = new Base64OutputStream(os, Base64.NO_WRAP);
            
            byte[] buffer = new byte[102400];
            int read;
            while ((read = is.read(buffer)) != -1) {
                b64os.write(buffer, 0, read);
                bytesProcessed += read;
                listener.onProgress(bytesProcessed, totalFileBytes);
            }
            b64os.flush();
            
        }
        
        os.write("\"}".getBytes());
        os.flush();
    }
    
    private long getFileSize() {
        try (android.database.Cursor cursor = context.getContentResolver().query(fileUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(android.provider.OpenableColumns.SIZE));
            }
        } catch (Exception e) {}
        return 0;
    }
}
