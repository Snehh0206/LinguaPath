package com.example.linguapath.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

// Score Mode matches between two friends. The pair id is the chat room id without "dm_".
public class ScoreSessionRepository {

    public static final long DURATION_MS = 5 * 60 * 1000L;
    public static final long COOLDOWN_MS = 5 * 60 * 60 * 1000L; // one match per pair every 5 hours
    public static final int TARGET = 20;

    private static final String TAG = "ScoreSession";

    public static class Session {
        public String state;      // requested, active, declined
        public String requester;
        public String language;
        public String scenario;   // topic id
        public String sid;        // one id per match
        public long requestedAt;
        public long startedAt;
        public Map<String, Integer> pts = new HashMap<>(); // uid -> match points
    }

    public interface SessionListener {
        void onSession(Session session); // null when there is none
    }

    public interface OffsetListener {
        void onOffset(long offsetMs);
    }

    public interface TimeCallback {
        void onTime(Long timestamp); // null when there is none
    }

    private static DatabaseReference root() {
        return FirebaseDatabase.getInstance().getReference();
    }

    private static DatabaseReference ref(String pair) {
        return root().child("sessions").child(pair);
    }

    // Difference between the server clock and this phone's clock
    public static ChatRepository.Registration listenOffset(OffsetListener listener) {
        DatabaseReference r = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        ValueEventListener v = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long value = snapshot.getValue(Long.class);
                listener.onOffset(value == null ? 0 : value);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        r.addValueEventListener(v);
        return () -> r.removeEventListener(v);
    }

    public static ChatRepository.Registration listen(String pair, SessionListener listener) {
        DatabaseReference r = ref(pair);
        ValueEventListener v = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) {
                    listener.onSession(null);
                    return;
                }
                Session s = new Session();
                s.state = snap.child("state").getValue(String.class);
                s.requester = snap.child("req").child("requester").getValue(String.class);
                s.language = snap.child("req").child("language").getValue(String.class);
                s.scenario = snap.child("req").child("scenario").getValue(String.class);
                s.sid = snap.child("req").child("sid").getValue(String.class);
                Long requestedAt = snap.child("req").child("requestedAt").getValue(Long.class);
                Long startedAt = snap.child("startedAt").getValue(Long.class);
                s.requestedAt = requestedAt == null ? 0 : requestedAt;
                s.startedAt = startedAt == null ? 0 : startedAt;
                if (s.sid != null) {
                    for (DataSnapshot c : snap.child("pts").child(s.sid).getChildren()) {
                        Long points = c.getValue(Long.class);
                        if (c.getKey() != null && points != null) {
                            s.pts.put(c.getKey(), points.intValue());
                        }
                    }
                }
                listener.onSession(s);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Session cancelled: " + error.getMessage());
            }
        };
        r.addValueEventListener(v);
        return () -> r.removeEventListener(v);
    }

    public static void loadCooldownStart(String pair, TimeCallback callback) {
        root().child("scoreCooldown").child(pair).get().addOnCompleteListener(task ->
                callback.onTime(task.isSuccessful() ? task.getResult().getValue(Long.class) : null));
    }

    public static void request(String pair, String language, String scenario,
                               ChatRepository.DoneCallback callback) {
        String me = ChatRepository.currentUid();
        String sid = ref(pair).child("pts").push().getKey();
        if (me == null || sid == null) {
            callback.onDone(false);
            return;
        }
        Map<String, Object> req = new HashMap<>();
        req.put("requester", me);
        req.put("language", language);
        req.put("scenario", scenario);
        req.put("sid", sid);
        req.put("requestedAt", ServerValue.TIMESTAMP);

        Map<String, Object> update = new HashMap<>();
        update.put("sessions/" + pair + "/state", "requested");
        update.put("sessions/" + pair + "/req", req);
        root().updateChildren(update).addOnCompleteListener(task -> callback.onDone(task.isSuccessful()));
    }

    // Start time and cooldown start are written together, with the server's clock
    public static void accept(String pair, ChatRepository.DoneCallback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("sessions/" + pair + "/state", "active");
        update.put("sessions/" + pair + "/startedAt", ServerValue.TIMESTAMP);
        update.put("scoreCooldown/" + pair, ServerValue.TIMESTAMP);
        root().updateChildren(update).addOnCompleteListener(task -> callback.onDone(task.isSuccessful()));
    }

    // Also used by the requester to cancel
    public static void decline(String pair, ChatRepository.DoneCallback callback) {
        ref(pair).child("state").setValue("declined")
                .addOnCompleteListener(task -> callback.onDone(task.isSuccessful()));
    }

    public static void addPoints(String pair, String sid, int gained) {
        String me = ChatRepository.currentUid();
        if (me == null || gained <= 0) {
            return;
        }
        ref(pair).child("pts").child(sid).child(me).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Long current = data.getValue(Long.class);
                data.setValue(Math.min(TARGET, (current == null ? 0L : current) + gained));
                return Transaction.success(data);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) {
                    Log.w(TAG, "Could not add match points: " + error.getMessage());
                }
            }
        });
    }

    // true only for the one call that really claims the reward (never twice for the same match)
    public static void claimReward(String pair, String sid, ChatRepository.DoneCallback callback) {
        String me = ChatRepository.currentUid();
        if (me == null) {
            callback.onDone(false);
            return;
        }
        ref(pair).child("paid").child(sid).child(me).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                if (data.getValue() != null) {
                    return Transaction.abort();
                }
                data.setValue(true);
                return Transaction.success(data);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                callback.onDone(error == null && committed);
            }
        });
    }
}