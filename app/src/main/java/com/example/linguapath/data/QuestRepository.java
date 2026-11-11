package com.example.linguapath.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.linguapath.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Two daily quests. Progress is counted here, the XP is paid once per quest per day.
public class QuestRepository {

    public static class Quest {
        public final String id;
        public final int titleRes;
        public final int target;
        public final int rewardXp;
        public final boolean countsMinutes; // false: counts finished quizzes

        Quest(String id, int titleRes, int target, int rewardXp, boolean countsMinutes) {
            this.id = id;
            this.titleRes = titleRes;
            this.target = target;
            this.rewardXp = rewardXp;
            this.countsMinutes = countsMinutes;
        }
    }

    public interface Done {
        void onDone(List<Quest> paidNow);
    }

    public static final Quest[] DAILY = {
            new Quest("quizzes", R.string.quest_quizzes_title, 2, 20, false),
            new Quest("minutes", R.string.quest_minutes_title, 10, 15, true)};

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                "linguapath_quests_" + QuizHistoryRepository.currentUserId(), Context.MODE_PRIVATE);
    }

    private static String today() {
        return LocalDate.now().toString(); // a new day means fresh quests
    }

    private static String claimKey(Quest q) {
        return "claimed_" + today() + "_" + q.id;
    }

    public static int progress(Context context, Quest q) {
        if (q.countsMinutes) {
            return (int) (new ProgressStore(context.getApplicationContext()).getStudyMillisToday() / 60000);
        }
        return prefs(context).getInt("quizzes_" + today(), 0);
    }

    public static boolean isClaimed(Context context, Quest q) {
        return prefs(context).getBoolean(claimKey(q), false);
    }

    // Called when a quiz ends
    public static void onQuizFinished(Context context) {
        SharedPreferences p = prefs(context);
        String key = "quizzes_" + today();
        p.edit().putInt(key, p.getInt(key, 0) + 1).apply();
        refresh(context);
    }

    // Pays every finished quest that was not paid yet. Returns the quests paid just now.
    public static List<Quest> refresh(Context context) {
        Context app = context.getApplicationContext();
        List<Quest> paid = new ArrayList<>();
        ProgressStore store = new ProgressStore(app);
        for (Quest q : DAILY) {
            if (!isClaimed(app, q) && progress(app, q) >= q.target) {
                prefs(app).edit().putBoolean(claimKey(q), true).apply(); // marked first: never paid twice
                store.addBonusXp(q.rewardXp);
                String uid = ChatRepository.currentUid();
                if (uid != null) {
                    FirebaseDatabase.getInstance().getReference("quests")
                            .child(uid).child(today()).child(q.id).setValue(true);
                }
                paid.add(q);
            }
        }
        RewardRepository.syncMine(app);
        return paid;
    }

    // First ask Firebase which quests were already paid today (maybe on another phone), then pay what is due
    public static void restoreAndRefresh(Context context, Done done) {
        Context app = context.getApplicationContext();
        String uid = ChatRepository.currentUid();
        if (uid == null) {
            done.onDone(refresh(app));
            return;
        }
        FirebaseDatabase.getInstance().getReference("quests").child(uid).child(today()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DataSnapshot child : task.getResult().getChildren()) {
                            if (child.getKey() != null && Boolean.TRUE.equals(child.getValue(Boolean.class))) {
                                prefs(app).edit()
                                        .putBoolean("claimed_" + today() + "_" + child.getKey(), true)
                                        .apply();
                            }
                        }
                    }
                    done.onDone(refresh(app));
                });
    }
}