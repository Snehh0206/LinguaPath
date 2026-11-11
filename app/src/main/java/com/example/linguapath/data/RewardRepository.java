package com.example.linguapath.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

// XP and levels. XP itself still lives in ProgressStore; this class mirrors it to Firebase.
public class RewardRepository {

    public static final int XP_PER_LEVEL = 50;
    private static final String TAG = "RewardRepository";

    public static int levelForXp(int xp) {
        return xp / XP_PER_LEVEL + 1;
    }

    public static int xpIntoLevel(int xp) {
        return xp % XP_PER_LEVEL;
    }

    // Writes my XP and finished stages to users/{uid}. XP can only go up (a transaction keeps the higher number).
    public static void syncMine(Context context) {
        String uid = ChatRepository.currentUid();
        if (uid == null) {
            return;
        }
        ProgressStore store = new ProgressStore(context.getApplicationContext());
        final long xp = store.getXp();
        DatabaseReference me = FirebaseDatabase.getInstance().getReference("users").child(uid);
        me.child("stages").setValue(store.getTotalCompleted());
        me.child("xp").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Long current = data.getValue(Long.class);
                if (current == null || xp > current) {
                    data.setValue(xp);
                }
                return Transaction.success(data);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) {
                    Log.w(TAG, "Could not sync XP: " + error.getMessage());
                }
            }
        });
    }
}