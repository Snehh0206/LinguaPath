package com.example.linguapath.data;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Friends, requests, search and the friends leaderboard. Callbacks arrive on the main thread.
public class FriendRepository {

    public static class Person {
        public final String uid;
        public final String name;

        public Person(String uid, String name) {
            this.uid = uid;
            this.name = name;
        }
    }

    public interface PeopleCallback {
        void onLoaded(List<Person> people);

        void onFailed();
    }

    private static DatabaseReference root() {
        return FirebaseDatabase.getInstance().getReference();
    }

    // Same id for both friends: the two uids, sorted
    public static String dmRoomId(String a, String b) {
        return a.compareTo(b) < 0 ? "dm_" + a + "_" + b : "dm_" + b + "_" + a;
    }

    // ---- Search ----

    public static void searchByName(String text, PeopleCallback callback) {
        String me = ChatRepository.currentUid();
        String q = text.toLowerCase(Locale.ROOT);
        root().child("profiles").orderByChild("nameLower").startAt(q).endAt(q + "\uf8ff")
                .limitToFirst(10).get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        callback.onFailed();
                        return;
                    }
                    List<Person> found = new ArrayList<>();
                    for (DataSnapshot child : task.getResult().getChildren()) {
                        String name = child.child("name").getValue(String.class);
                        if (name != null && child.getKey() != null && !child.getKey().equals(me)) {
                            found.add(new Person(child.getKey(), name));
                        }
                    }
                    callback.onLoaded(found);
                });
    }

    // ---- Requests ----

    public static void sendRequest(String toUid, String myName, ChatRepository.DoneCallback callback) {
        String me = ChatRepository.currentUid();
        if (me == null) {
            callback.onDone(false);
            return;
        }
        Map<String, Object> request = new HashMap<>();
        request.put("name", myName);
        request.put("ts", ServerValue.TIMESTAMP);
        root().child("friendRequests").child(toUid).child(me).setValue(request)
                .addOnCompleteListener(task -> callback.onDone(task.isSuccessful()));
    }

    public static void loadRequests(PeopleCallback callback) {
        String me = ChatRepository.currentUid();
        if (me == null) {
            callback.onFailed();
            return;
        }
        root().child("friendRequests").child(me).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                callback.onFailed();
                return;
            }
            List<Person> people = new ArrayList<>();
            for (DataSnapshot child : task.getResult().getChildren()) {
                String name = child.child("name").getValue(String.class);
                if (name != null && child.getKey() != null) {
                    people.add(new Person(child.getKey(), name));
                }
            }
            callback.onLoaded(people);
        });
    }

    // Both friend lists and the deleted request change in ONE write, so it all happens or nothing does
    public static void accept(String fromUid, ChatRepository.DoneCallback callback) {
        String me = ChatRepository.currentUid();
        if (me == null) {
            callback.onDone(false);
            return;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("friends/" + me + "/" + fromUid, true);
        update.put("friends/" + fromUid + "/" + me, true);
        update.put("friendRequests/" + me + "/" + fromUid, null);
        root().updateChildren(update).addOnCompleteListener(task -> callback.onDone(task.isSuccessful()));
    }

    public static void decline(String fromUid, ChatRepository.DoneCallback callback) {
        String me = ChatRepository.currentUid();
        if (me == null) {
            callback.onDone(false);
            return;
        }
        root().child("friendRequests").child(me).child(fromUid).removeValue()
                .addOnCompleteListener(task -> callback.onDone(task.isSuccessful()));
    }

    public static void remove(String friendUid, ChatRepository.DoneCallback callback) {
        String me = ChatRepository.currentUid();
        if (me == null) {
            callback.onDone(false);
            return;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("friends/" + me + "/" + friendUid, null);
        update.put("friends/" + friendUid + "/" + me, null);
        root().updateChildren(update).addOnCompleteListener(task -> callback.onDone(task.isSuccessful()));
    }

    // ---- Friends list ----

    public static void loadFriends(PeopleCallback callback) {
        String me = ChatRepository.currentUid();
        if (me == null) {
            callback.onFailed();
            return;
        }
        root().child("friends").child(me).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                callback.onFailed();
                return;
            }
            List<String> uids = new ArrayList<>();
            for (DataSnapshot child : task.getResult().getChildren()) {
                if (child.getKey() != null) {
                    uids.add(child.getKey());
                }
            }
            if (uids.isEmpty()) {
                callback.onLoaded(new ArrayList<>());
                return;
            }
            List<Person> people = new ArrayList<>();
            int[] left = {uids.size()};
            for (String uid : uids) {
                root().child("profiles").child(uid).child("name").get().addOnCompleteListener(nameTask -> {
                    String name = nameTask.isSuccessful() ? nameTask.getResult().getValue(String.class) : null;
                    people.add(new Person(uid, name == null ? "?" : name));
                    if (--left[0] == 0) {
                        Collections.sort(people, (a, b) -> a.name.compareToIgnoreCase(b.name));
                        callback.onLoaded(people);
                    }
                });
            }
        });
    }

    // ---- Friends leaderboard: me + my friends, by XP or by chat points ----

    public static void loadLeaderboard(boolean byXp, ChatScoreRepository.LeaderboardCallback callback) {
        String me = ChatRepository.currentUid();
        if (me == null) {
            callback.onFailed();
            return;
        }
        root().child("friends").child(me).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                callback.onFailed();
                return;
            }
            List<String> uids = new ArrayList<>();
            uids.add(me);
            for (DataSnapshot child : task.getResult().getChildren()) {
                if (child.getKey() != null) {
                    uids.add(child.getKey());
                }
            }
            List<ChatScoreRepository.Entry> entries = new ArrayList<>();
            int[] left = {uids.size()};
            for (String uid : uids) {
                root().child("profiles").child(uid).child("name").get().addOnCompleteListener(nameTask -> {
                    String name = nameTask.isSuccessful() ? nameTask.getResult().getValue(String.class) : null;
                    DatabaseReference valueRef = byXp
                            ? root().child("users").child(uid).child("xp")
                            : root().child("scores").child(uid).child("points");
                    valueRef.get().addOnCompleteListener(valueTask -> {
                        Long value = valueTask.isSuccessful() ? valueTask.getResult().getValue(Long.class) : null;
                        entries.add(new ChatScoreRepository.Entry(
                                uid, name == null ? "?" : name, value == null ? 0 : value.intValue()));
                        if (--left[0] == 0) {
                            Collections.sort(entries, (a, b) -> Integer.compare(b.points, a.points));
                            callback.onLoaded(entries);
                        }
                    });
                });
            }
        });
    }
}