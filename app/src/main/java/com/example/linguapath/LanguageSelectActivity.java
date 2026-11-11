package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.animation.ObjectAnimator;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.example.linguapath.data.AuthRepository;
import com.example.linguapath.databinding.ActivityLanguageSelectBinding;
import com.example.linguapath.view.Motion;
import com.example.linguapath.view.TouchBounce;
import com.example.linguapath.databinding.DialogLanguageChoiceBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.ActivityNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class LanguageSelectActivity extends AppCompatActivity {

    private static final String TAG = "LanguageSelect";
    private ActivityResultLauncher<Void> pickContact;
    private ActivityLanguageSelectBinding binding;
    private AuthRepository authRepository;
    private ObjectAnimator floatKorean;
    private ObjectAnimator floatSpanish;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivityLanguageSelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository();

        // The system contact picker. The app never sees the whole contact list.
        pickContact = registerForActivityResult(new ActivityResultContracts.PickContact(), uri -> {
            if (uri != null) {
                shareInvite(contactName(uri));
            }
        });
        binding.cardKorean.setOnClickListener(v -> onLanguagePicked("Korean"));
        binding.cardSpanish.setOnClickListener(v -> onLanguagePicked("Spanish"));
        binding.buttonMenu.setOnClickListener(v -> startActivity(new Intent(this, MenuActivity.class)));

        // Cards float in, and tilt in 3D when touched
        Motion.fadeSlideIn(binding.cardKorean, 100);
        Motion.fadeSlideIn(binding.cardSpanish, 220);
        TouchBounce.attach(binding.cardKorean);
        TouchBounce.attach(binding.cardSpanish);

    }

    private void onLanguagePicked(String languageName) {
        Log.d(TAG, "Picked: " + languageName);
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        DialogLanguageChoiceBinding b = DialogLanguageChoiceBinding.inflate(getLayoutInflater());
        sheet.setContentView(b.getRoot());
        b.textChoiceTitle.setText(languageName);

        b.cardChoiceStages.setOnClickListener(v -> {
            sheet.dismiss();
            Intent intent = new Intent(this, LanguageDashboardActivity.class);
            intent.putExtra(LanguageDashboardActivity.EXTRA_LANGUAGE, languageName);
            startActivity(intent);
        });
        b.cardChoiceTopics.setOnClickListener(v -> {
            sheet.dismiss();
            Intent intent = new Intent(this, TopicsActivity.class);
            intent.putExtra(TopicsActivity.EXTRA_LANGUAGE, languageName);
            startActivity(intent);
        });
        sheet.show();
    }

    // The three-dots menu: shows who is signed in, and offers Log out
    private void showAccountMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_account, popup.getMenu());

        String email = authRepository.getEmail();
        popup.getMenu().findItem(R.id.menu_user)
                .setTitle(getString(R.string.signed_in_as, email == null ? "" : email));

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_friends) {
                startActivity(new Intent(this, FriendsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_credits) {
                startActivity(new Intent(this, CreditsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_account_page) {
                startActivity(new Intent(this, AccountActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_quests) {
                startActivity(new Intent(this, QuestsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_chat) {
                startActivity(new Intent(this, ChatRoomsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_progress) {
                startActivity(new Intent(this, ProgressActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_sign) {
                startActivity(new Intent(this, SignTranslateActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_invite) {
                pickContact.launch(null);
                return true;
            }
            if (item.getItemId() == R.id.menu_translator) {
                startActivity(new Intent(this, TranslatorActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_saved) {
                startActivity(new Intent(this, SavedWordsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_reminder) {
                startActivity(new Intent(this, ReminderActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_logout) {
                confirmLogout();
                return true;
            }
            return false;
        });
        popup.show();
    }
    // Reads only the name of the one contact the user picked
    private String contactName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && c.getString(0) != null) {
                return c.getString(0);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not read contact name", e);
        }
        return "";
    }

    // Implicit Intent: the user chooses SMS, WhatsApp, Messenger and so on
    private void shareInvite(String name) {
        String greeting = name.isEmpty() ? "Hi!" : "Hi " + name + "!";
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.invite_message, greeting, getString(R.string.invite_link)));
        try {
            startActivity(Intent.createChooser(send, getString(R.string.invite_chooser)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.invite_no_app, Toast.LENGTH_SHORT).show();
        }
    }
    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.menu_logout, (dialog, which) -> {
                    //stopService(new Intent(this, com.example.linguapath.service.StudySessionService.class));
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
        // The cards hover gently, one slightly after the other
        if (floatKorean == null) {
            floatKorean = Motion.startFloat(binding.cardKorean, 3, 900);
            floatSpanish = Motion.startFloat(binding.cardSpanish, 3, 1300);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.scenery.stop();
        Motion.stopFloat(floatKorean, binding.cardKorean);
        Motion.stopFloat(floatSpanish, binding.cardSpanish);
        floatKorean = null;
        floatSpanish = null;
    }


}