package com.example.linguapath;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.linguapath.adapter.CreditAdapter;
import com.example.linguapath.databinding.ActivityCreditsBinding;
import com.example.linguapath.model.Credit;

import java.util.ArrayList;
import java.util.List;

public class CreditsActivity extends AppCompatActivity {

    private ActivityCreditsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreditsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBack.setOnClickListener(v -> finish());
        binding.textVersion.setText(getString(R.string.credits_version, versionName()));

        CreditAdapter adapter = new CreditAdapter(this::openLink);
        binding.recyclerCredits.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCredits.setAdapter(adapter);

        List<Credit> credits = new ArrayList<>();
        credits.add(new Credit(getString(R.string.credit_content_title),
                getString(R.string.credit_content_body), null));
        credits.add(new Credit(getString(R.string.credit_translate_title),
                getString(R.string.credit_translate_body), getString(R.string.credit_translate_url)));
        credits.add(new Credit(getString(R.string.credit_firebase_title),
                getString(R.string.credit_firebase_body), getString(R.string.credit_firebase_url)));
        credits.add(new Credit(getString(R.string.credit_speech_title),
                getString(R.string.credit_speech_body), null));
        credits.add(new Credit(getString(R.string.credit_ui_title),
                getString(R.string.credit_ui_body), getString(R.string.credit_ui_url)));
        credits.add(new Credit(getString(R.string.credit_pictures_title),
                getString(R.string.credit_pictures_body), null));
        credits.add(new Credit(getString(R.string.credit_author_title),
                getString(R.string.credit_author_body), null));
        adapter.setItems(credits);
    }

    // Implicit Intent: we only say "show this web address", and Android picks a browser
    private void openLink(String url) {
        binding.textMessage.setText("");
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            binding.textMessage.setText(R.string.credits_no_browser); // no browser installed
        }
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }
}