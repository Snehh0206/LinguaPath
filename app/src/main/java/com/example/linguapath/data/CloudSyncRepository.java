package com.example.linguapath.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

// The only class that talks to Firestore.
// Cloud path: users / <user id> / progress / <language>   ->   { completed: 3 }
public class CloudSyncRepository {

    public interface DownloadCallback {
        // reachedServer = false means we are offline (or it failed). completed = -1 means "nothing found".
        void onResult(boolean reachedServer, int completed);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DocumentReference progressDoc(String userId, String language) {
        return db.collection("users").document(userId)
                .collection("progress").document(language);
    }

    // Fire and forget. If the phone is offline, Firestore keeps the write and sends it later.
    public void uploadProgress(String language, int completed) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("completed", completed);
        data.put("updated", FieldValue.serverTimestamp());
        progressDoc(user.getUid(), language).set(data);
    }

    public void downloadProgress(String language, DownloadCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onResult(false, -1);
            return;
        }
        progressDoc(user.getUid(), language).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                callback.onResult(false, -1);
                return;
            }
            DocumentSnapshot snapshot = task.getResult();
            // fromCache = true means the answer came from the phone, not the server
            boolean reachedServer = !snapshot.getMetadata().isFromCache();
            Long value = snapshot.exists() ? snapshot.getLong("completed") : null;
            callback.onResult(reachedServer, value == null ? -1 : value.intValue());
        });
    }
}