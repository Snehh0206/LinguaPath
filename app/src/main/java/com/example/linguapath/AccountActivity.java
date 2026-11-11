package com.example.linguapath;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.AuthRepository;
import com.example.linguapath.data.ChatRepository;
import com.example.linguapath.data.ChatScoreRepository;
import com.example.linguapath.data.ChatSettings;
import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.data.QuizHistoryRepository;
import com.example.linguapath.data.RewardRepository;
import com.example.linguapath.databinding.ActivityAccountBinding;
import com.example.linguapath.util.AvatarStore;
import com.example.linguapath.util.GalleryHelper;
import com.example.linguapath.view.Motion;

import java.util.Map;
import java.util.regex.Pattern;

// Profile, XP, reward points, level and progress. Separate from the learning screens.
public class AccountActivity extends AppCompatActivity {

    private static final Pattern NICKNAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private ActivityAccountBinding binding;
    private ChatRepository.Registration scoreRegistration;
    private GalleryHelper gallery;
    private String myUid;
    private String currentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        myUid = ChatRepository.currentUid();
        String email = new AuthRepository().getEmail();
        binding.textEmail.setText(email == null ? "" : email);
        binding.buttonBack.setOnClickListener(v -> finish());
        binding.buttonQuests.setOnClickListener(v ->
                startActivity(new Intent(this, QuestsActivity.class)));

        // Profile picture: the phone's gallery, after asking for photo access
        binding.imageAvatar.setClipToOutline(true);
        gallery = new GalleryHelper(this, uri -> new Thread(() -> {
            boolean ok = myUid != null && AvatarStore.save(this, myUid, uri);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (ok) {
                    showAvatar();
                } else {
                    Toast.makeText(this, R.string.avatar_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }).start());
        binding.avatarBox.setOnClickListener(v -> showAvatarOptions());
        binding.buttonEditName.setOnClickListener(v -> showNicknameDialog());
        binding.textName.setOnClickListener(v -> showNicknameDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProgressStore store = new ProgressStore(this);
        int xp = store.getXp();
        int level = RewardRepository.levelForXp(xp);
        int into = RewardRepository.xpIntoLevel(xp);

        binding.textLevel.setText(getString(R.string.account_level, level));
        Motion.animateProgress(binding.progressLevel, into, RewardRepository.XP_PER_LEVEL);
        binding.textLevelHint.setText(getString(R.string.account_to_next,
                into, RewardRepository.XP_PER_LEVEL, level + 1));
        binding.textXp.setText(getString(R.string.account_xp, xp));
        binding.textStages.setText(getString(R.string.account_stages, store.getTotalCompleted()));
        showLanguage("Korean", binding.textLangKorean);
        showLanguage("Spanish", binding.textLangSpanish);
        showLanguage("Bangla", binding.textLangBangla);
        showLanguage("Japanese", binding.textLangJapanese);
        showLanguage("French", binding.textLangFrench);

        RewardRepository.syncMine(this);
        showAvatar();

        ChatRepository.loadNickname((name, failed) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            currentName = (name != null && !name.isEmpty()) ? name : null;
            showName();
        });

        // Reward points (chat), updated live
        scoreRegistration = ChatScoreRepository.listenMyScore(points ->
                binding.textPoints.setText(getString(R.string.account_points, points)));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (scoreRegistration != null) {
            scoreRegistration.remove();
            scoreRegistration = null;
        }
    }

    private void showName() {
        binding.textName.setText(currentName != null
                ? currentName : getString(R.string.account_no_name));
        binding.textAvatar.setText(currentName != null
                ? currentName.substring(0, 1).toUpperCase() : "?");
    }

    // A picture if the user chose one, otherwise the first letter of the nickname
    private void showAvatar() {
        Bitmap bmp = myUid == null ? null : AvatarStore.load(this, myUid);
        if (bmp != null) {
            binding.imageAvatar.setImageBitmap(bmp);
            binding.imageAvatar.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.imageAvatar.setVisibility(android.view.View.GONE);
        }
    }

    private void showAvatarOptions() {
        String[] items = {getString(R.string.avatar_choose), getString(R.string.avatar_letter)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.avatar_title)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        gallery.open();
                    } else if (myUid != null) {
                        AvatarStore.delete(this, myUid);
                        showAvatar();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showNicknameDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.nickname_hint);
        input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        if (currentName != null) {
            input.setText(currentName);
        }
        FrameLayout box = new FrameLayout(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 3, pad, 0);
        box.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.nickname_title)
                .setMessage(R.string.nickname_message)
                .setView(box)
                .setPositiveButton(R.string.nickname_save, null) // set below, so errors keep it open
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = input.getText().toString().trim();
                    if (!NICKNAME.matcher(name).matches()) {
                        input.setError(getString(R.string.nickname_invalid));
                        return;
                    }
                    ChatRepository.saveNickname(name, ok -> {
                        if (!ok) {
                            input.setError(getString(R.string.nickname_error));
                            return;
                        }
                        if (myUid != null) {
                            new ChatSettings(this).setNickname(myUid, name);
                        }
                        currentName = name;
                        showName();
                        dialog.dismiss();
                    });
                }));
        dialog.show();
    }

    private void showLanguage(String language, TextView view) {
        int stages = new ProgressStore(this).getCompletedCount(language);
        QuizHistoryRepository.loadBestScores(this, language, best -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            int sum = 0;
            int count = 0;
            for (Map.Entry<Integer, Integer> entry : best.entrySet()) {
                if (entry.getKey() >= 0) { // stage quizzes only, not revision or topic quizzes
                    sum += entry.getValue();
                    count++;
                }
            }
            if (stages == 0 && count == 0) {
                view.setText(getString(R.string.account_lang_none, language));
            } else {
                view.setText(getString(R.string.account_lang_row,
                        language, stages, count == 0 ? 0 : sum / count));
            }
        });
    }
}