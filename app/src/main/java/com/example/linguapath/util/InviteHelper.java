package com.example.linguapath.util;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.linguapath.R;

// Invite a friend: asks for contacts permission, lets the user pick a phone number,
// then opens the normal message app with the invite already written.
// Create it in onCreate, then call start().
public class InviteHelper {

    private final AppCompatActivity activity;
    private final ActivityResultLauncher<String> askContacts;
    private final ActivityResultLauncher<Intent> pickNumber;

    public InviteHelper(AppCompatActivity activity) {
        this.activity = activity;
        askContacts = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        openPicker();
                    } else if (!activity.shouldShowRequestPermissionRationale(
                            Manifest.permission.READ_CONTACTS)) {
                        showSettingsDialog(); // "don't ask again" or turned off in Settings
                    } else {
                        Toast.makeText(activity, R.string.contacts_denied_soft,
                                Toast.LENGTH_SHORT).show();
                    }
                });
        pickNumber = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    Intent data = result.getData();
                    if (result.getResultCode() == Activity.RESULT_OK
                            && data != null && data.getData() != null) {
                        readAndSend(data.getData());
                    }
                });
    }

    // Checks the permission every time, so turning it off in Settings asks again
    public void start() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            openPicker();
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.contacts_why_title)
                .setMessage(R.string.contacts_why_message)
                .setPositiveButton(R.string.reminder_why_continue,
                        (d, w) -> askContacts.launch(Manifest.permission.READ_CONTACTS))
                .setNegativeButton(R.string.reminder_not_now, null)
                .show();
    }

    private void openPicker() {
        Intent pick = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        try {
            pickNumber.launch(pick);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.invite_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void readAndSend(Uri uri) {
        String number = null;
        String name = "";
        try (Cursor c = activity.getContentResolver().query(uri,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                null, null, null)) {
            if (c != null && c.moveToFirst()) {
                number = c.getString(0);
                name = c.getString(1) == null ? "" : c.getString(1).trim();
            }
        } catch (Exception e) {
            Log.w("InviteHelper", "Could not read contact", e);
        }
        String first = name.isEmpty() ? "there" : name.split(" ")[0];
        String text = activity.getString(R.string.hub_invite_text, first,
                activity.getString(R.string.invite_link));

        if (number != null) {
            String clean = number.replaceAll("[^0-9+]", "");
            if (!clean.isEmpty()) {
                Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + clean));
                sms.putExtra("sms_body", text);
                try {
                    activity.startActivity(sms); // the normal message app, number filled in
                    return;
                } catch (ActivityNotFoundException ignored) {
                    // fall through to the share sheet
                }
            }
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        try {
            activity.startActivity(Intent.createChooser(send,
                    activity.getString(R.string.invite_chooser)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.invite_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.reminder_settings_title_perm)
                .setMessage(R.string.perm_settings_contacts)
                .setPositiveButton(R.string.reminder_open_settings, (d, w) -> {
                    Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", activity.getPackageName(), null));
                    activity.startActivity(i);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}