package com.example.linguapath.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.linguapath.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

// The only class that talks to the Realtime Database for chat.
// All callbacks arrive on the main thread.
public class ChatRepository {

    public interface NameCallback {
        // name is null when the user has none yet. failed = true when we could not ask.
        void onResult(String name, boolean failed);
    }

    public interface DoneCallback {
        void onDone(boolean success);
    }

    public interface MessageListener {
        void onMessage(ChatMessage message);
    }

    public interface ConnectionListener {
        void onChanged(boolean connected);
    }

    // Call remove() to stop listening (in onStop), so nothing keeps running in the background
    public interface Registration {
        void remove();
    }

    private static final String TAG = "ChatRepository";
    private static final int LAST_MESSAGES = 50;

    private static FirebaseDatabase db() {
        return FirebaseDatabase.getInstance();
    }

    public static String currentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user == null) ? null : user.getUid();
    }

    // ---- Nickname ----

    public static void loadNickname(NameCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onResult(null, true);
            return;
        }
        db().getReference("profiles").child(uid).child("name").get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onResult(null, true);
                        return;
                    }
                    DataSnapshot snapshot = task.getResult();
                    callback.onResult(snapshot.exists() ? snapshot.getValue(String.class) : null, false);
                });
    }

    public static void saveNickname(String name, DoneCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onDone(false);
            return;
        }
        db().getReference("profiles").child(uid).child("name").setValue(name)
                .addOnCompleteListener(task -> callback.onDone(task.isSuccessful()));
    }

    // ---- Messages ----

    public static void sendMessage(String roomId, String name, String text, DoneCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onDone(false);
            return;
        }
        DatabaseReference room = db().getReference("messages").child(roomId);
        String key = room.push().getKey(); // keys sort by time, so order is automatic
        if (key == null) {
            callback.onDone(false);
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("uid", uid);
        message.put("name", name);
        message.put("text", text);
        message.put("ts", ServerValue.TIMESTAMP); // the server writes the real time

        room.child(key).setValue(message)
                .addOnCompleteListener(task -> callback.onDone(task.isSuccessful()));
    }

    // Delivers the last 50 messages, then every new one as it arrives
    public static Registration listen(String roomId, MessageListener listener) {
        Query query = db().getReference("messages").child(roomId)
                .orderByKey().limitToLast(LAST_MESSAGES);

        ChildEventListener childListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String uid = snapshot.child("uid").getValue(String.class);
                String name = snapshot.child("name").getValue(String.class);
                String text = snapshot.child("text").getValue(String.class);
                Long ts = snapshot.child("ts").getValue(Long.class);
                if (uid == null || name == null || text == null || snapshot.getKey() == null) {
                    return;
                }
                listener.onMessage(new ChatMessage(snapshot.getKey(), uid, name, text,
                        ts == null ? System.currentTimeMillis() : ts));
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Messages cancelled: " + error.getMessage());
            }
        };
        query.addChildEventListener(childListener);
        return () -> query.removeEventListener(childListener);
    }

    // Tells us when the phone is connected to the database or not
    public static Registration listenConnection(ConnectionListener listener) {
        DatabaseReference ref = db().getReference(".info/connected");
        ValueEventListener valueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onChanged(Boolean.TRUE.equals(snapshot.getValue(Boolean.class)));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Connection cancelled: " + error.getMessage());
            }
        };
        ref.addValueEventListener(valueListener);
        return () -> ref.removeEventListener(valueListener);
    }

    // ---- Reports (only you can read them, in the Firebase console) ----

    public static void report(String roomId, String messageId, DoneCallback callback) {
        Map<String, Object> report = new HashMap<>();
        report.put("room", roomId);
        report.put("msgId", messageId);
        report.put("reporter", currentUid());
        report.put("ts", ServerValue.TIMESTAMP);

        db().getReference("reports").push().setValue(report)
                .addOnCompleteListener(task -> callback.onDone(task.isSuccessful()));
    }
}