package com.example.linguapath;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.AuthRepository;
import com.example.linguapath.databinding.ActivityMenuBinding;
import com.example.linguapath.databinding.ItemHubBinding;
import com.example.linguapath.view.Motion;
import com.example.linguapath.view.TouchBounce;

// The menu: its own page with sections, instead of a small popup
public class MenuActivity extends AppCompatActivity {

    private ActivityMenuBinding binding;
    private AuthRepository authRepository;
    private int delay = 60;

    // Pick one contact (no contact list is read or uploaded)
    private final ActivityResultLauncher<Void> pickContact = registerForActivityResult(
            new ActivityResultContracts.PickContact(), uri -> {
                if (uri != null) {
                    shareInvite(contactName(uri));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository();
        String email = authRepository.getEmail();
        binding.textSignedIn.setText(getString(R.string.signed_in_as, email == null ? "" : email));
        binding.buttonBack.setOnClickListener(v -> finish());

        addSection(R.string.hub_sec_learning);
        addItem("🌐", R.string.hub_translator, R.string.hub_translator_sub, R.drawable.bg_card_sage,
                () -> open(TranslatorActivity.class));
        addItem("📷", R.string.hub_sign, R.string.hub_sign_sub, R.drawable.bg_card_sage,
                () -> open(SignTranslateActivity.class));
        addItem("🔖", R.string.hub_saved, R.string.hub_saved_sub, R.drawable.bg_card_sage,
                () -> open(SavedWordsActivity.class));
        addItem("📈", R.string.hub_progress, R.string.hub_progress_sub, R.drawable.bg_card_sage,
                () -> open(ProgressActivity.class));
        addItem("🎯", R.string.hub_quests, R.string.hub_quests_sub, R.drawable.bg_card_sage,
                () -> open(QuestsActivity.class));
        addItem("⏰", R.string.hub_reminder, R.string.hub_reminder_sub, R.drawable.bg_card_sage,
                () -> open(ReminderActivity.class));

        addSection(R.string.hub_sec_friends);
        addItem("👥", R.string.hub_friends, R.string.hub_friends_sub, R.drawable.bg_card_peach,
                () -> open(FriendsActivity.class));
        addItem("💬", R.string.hub_chat, R.string.hub_chat_sub, R.drawable.bg_card_peach,
                () -> open(ChatRoomsActivity.class));
        addItem("🏆", R.string.hub_leaderboard, R.string.hub_leaderboard_sub, R.drawable.bg_card_peach,
                () -> open(LeaderboardActivity.class));
        addItem("💌", R.string.hub_invite, R.string.hub_invite_sub, R.drawable.bg_card_peach,
                () -> pickContact.launch(null));

        addSection(R.string.hub_sec_account);
        addItem("🙋", R.string.hub_account, R.string.hub_account_sub, R.drawable.bg_card_sky,
                () -> open(AccountActivity.class));
        addItem("🌱", R.string.hub_credits, R.string.hub_credits_sub, R.drawable.bg_card_sky,
                () -> open(CreditsActivity.class));
        addItem("🚪", R.string.hub_logout, R.string.hub_logout_sub, R.drawable.bg_card_sky,
                this::confirmLogout);
    }

    private void open(Class<?> screen) {
        startActivity(new Intent(this, screen));
    }

    // ---- Building the page ----

    private void addSection(int titleRes) {
        TextView header = new TextView(this);
        header.setText(titleRes);
        header.setTextColor(getColor(R.color.ink));
        header.setTextSize(18);
        header.setTypeface(null, Typeface.BOLD);
        header.setBackgroundResource(R.drawable.bg_card_butter);
        header.setElevation(dp(4));
        int h = dp(16);
        int v = dp(6);
        header.setPadding(h, v, h, v);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(24);
        header.setLayoutParams(params);
        binding.containerMenu.addView(header);
        Motion.fadeSlideIn(header, delay);
        delay += 50;
    }

    private void addItem(String emoji, int titleRes, int subRes, int backgroundRes, Runnable action) {
        ItemHubBinding row = ItemHubBinding.inflate(getLayoutInflater(), binding.containerMenu, false);
        row.textEmoji.setText(emoji);
        row.textTitle.setText(titleRes);
        row.textSub.setText(subRes);
        row.getRoot().setBackgroundResource(backgroundRes);
        row.getRoot().setOnClickListener(v -> action.run());
        binding.containerMenu.addView(row.getRoot());
        Motion.fadeSlideIn(row.getRoot(), delay);
        delay += 50;
        TouchBounce.attach(row.getRoot());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    // ---- Invite a friend ----

    private String contactName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                return name == null ? "" : name;
            }
        } catch (RuntimeException ignored) {
            // no name: the message just says "there"
        }
        return "";
    }

    private void shareInvite(String name) {
        String text = getString(R.string.hub_invite_text,
                name.isEmpty() ? "there" : name, getString(R.string.invite_link));
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        try {
            startActivity(Intent.createChooser(send, getString(R.string.invite_chooser)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.invite_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    // ---- Log out ----

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.menu_logout, (dialog, which) -> {
                    com.example.linguapath.service.StudyTimerController.endSession();
                    authRepository.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.scenery.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.scenery.stop();
    }
}