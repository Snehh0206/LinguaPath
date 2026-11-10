package com.example.linguapath.system;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.linguapath.R;
import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.data.TranslationRepository;
import com.example.linguapath.service.SyncProgressService;
import com.example.linguapath.service.WordOfDayService;

// Everything that was waiting for the internet is tried again when the connection returns
public class PendingWork {

    private static final MutableLiveData<String> RESULT = new MutableLiveData<>();

    // A short message about what was retried, for the banner
    public static LiveData<String> result() {
        return RESULT;
    }

    public static void retryAll(Context context) {
        Context app = context.getApplicationContext();

        // 1. Progress that was not uploaded yet: the background sync service does it again
        ProgressStore store = new ProgressStore(app);
        int syncs = 0;
        for (String language : new String[]{"Spanish", "Korean"}) {
            if (store.isSyncPending(language)) {
                app.startService(new Intent(app, SyncProgressService.class)
                        .putExtra(SyncProgressService.EXTRA_LANGUAGE, language));
                syncs++;
            }
        }

        // 2. Translations that could not be done offline
        final int syncCount = syncs;
        TranslationRepository.retryPending(app, translated -> {
            if (syncCount + translated > 0) {
                RESULT.postValue(app.getString(R.string.banner_retry_done, syncCount, translated));
            }
        });

        // 3. The optional word of the day (it skips itself when battery saver is on)
        WordOfDayService.requestIfNeeded(app);
    }
}