package com.codebysonu.gitpushpro;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.codebysonu.gitpushpro.databinding.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class MainActivity extends AppCompatActivity {
	
	private MainBinding binding;
	
	private Intent intent = new Intent();
	private SharedPreferences sp;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		sp = getSharedPreferences("sp", Activity.MODE_PRIVATE);
	}
	
	private void initializeLogic() {
		
		String mode = getIntent().getStringExtra("mod");
		boolean isSwitchMode = mode != null && mode.equals("switch");
		
		if (sp.getBoolean("is_connected", false) && !isSwitchMode) {
			intent.setClass(MainActivity.this, CommitprogressActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		
		
		binding.editToken.setText(sp.getString("token", ""));
		binding.editRepo.setText(sp.getString("repo_full_url", ""));
		
		final boolean[] isTokenErrorActive = {false};
		final boolean[] isPathErrorActive = {false};
		
		
		binding.editToken.addTextChangedListener(new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(android.text.Editable s) {
				if (isTokenErrorActive[0]) {
					String input = s.toString().trim();
					if (input.startsWith("ghp_") && !input.isEmpty()) {
						binding.tokenGuideTv.setText("Use a Personal Access Token with 'repo' scope.");
						binding.tokenGuideTv.setTextColor(android.graphics.Color.parseColor("#666666"));
						binding.layoutToken.setBoxStrokeColor(android.graphics.Color.parseColor("#2C2C2C"));
						isTokenErrorActive[0] = false;
					}
				}
			}
		});
		
		
		binding.editRepo.addTextChangedListener(new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(android.text.Editable s) {
				if (isPathErrorActive[0]) {
					String input = s.toString().trim();
					if (input.contains("github.com/")) {
						binding.pathGuideTv.setText("Format: https://github.com/owner/repo/tree/branch/folder");
						binding.pathGuideTv.setTextColor(android.graphics.Color.parseColor("#666666"));
						binding.layoutRepo.setBoxStrokeColor(android.graphics.Color.parseColor("#2C2C2C"));
						isPathErrorActive[0] = false;
					}
				}
			}
		});
		
		
		binding.btnConnect.setOnClickListener(v -> {
			String tokenStr = binding.editToken.getText().toString().trim();
			String rawInput = binding.editRepo.getText().toString().trim();
			
			String decodedInput = android.net.Uri.decode(rawInput);
			
			String repoOnly = "";
			String folderPath = "";
			String branchName = "main";
			boolean isValid = true;
			
			
			if (!decodedInput.contains("github.com/")) {
				binding.pathGuideTv.setText("Error: Full GitHub URL is mandatory!");
				binding.pathGuideTv.setTextColor(android.graphics.Color.parseColor("#FF5252"));
				binding.layoutRepo.setBoxStrokeColor(android.graphics.Color.parseColor("#FF5252"));
				isPathErrorActive[0] = true;
				isValid = false;
			}
			
			
			if (!tokenStr.startsWith("ghp_") || tokenStr.isEmpty()) {
				binding.tokenGuideTv.setText("Invalid Token! It must start with 'ghp_'");
				binding.tokenGuideTv.setTextColor(android.graphics.Color.parseColor("#FF5252"));
				binding.layoutToken.setBoxStrokeColor(android.graphics.Color.parseColor("#FF5252"));
				isTokenErrorActive[0] = true;
				isValid = false;
			}
			
			if (isValid) {
				String cleanPath = decodedInput;
				if (cleanPath.contains("github.com/")) {
					cleanPath = cleanPath.substring(cleanPath.indexOf("github.com/") + 11);
				}
				
				if (cleanPath.contains("/tree/")) {
					repoOnly = cleanPath.substring(0, cleanPath.indexOf("/tree/"));
					String afterTree = cleanPath.substring(cleanPath.indexOf("/tree/") + 6);
					
					if (afterTree.contains("/")) {
						branchName = afterTree.substring(0, afterTree.indexOf("/"));
						folderPath = afterTree.substring(afterTree.indexOf("/") + 1);
					} else {
						branchName = afterTree;
					}
				} else {
					String[] parts = cleanPath.split("/");
					if (parts.length >= 2) {
						repoOnly = parts[0] + "/" + parts[1];
						if (parts.length > 2) {
							folderPath = cleanPath.substring(repoOnly.length() + 1);
						}
					}
				}
				
				if (folderPath.endsWith("/")) folderPath = folderPath.substring(0, folderPath.length() - 1);
				if (repoOnly.endsWith("/")) repoOnly = repoOnly.substring(0, repoOnly.length() - 1);
				
				
				sp.edit()
				.putString("token", tokenStr)
				.putString("repo", repoOnly)
				.putString("folder", folderPath)
				.putString("branch", branchName)
				.putString("repo_full_url", rawInput)
				.putBoolean("is_connected", true)
				.apply();
				
				if (isSwitchMode) {
					intent.setClass(MainActivity.this, GitpushActivity.class);
				} else {
					intent.setClass(MainActivity.this, CommitprogressActivity.class);
				}
				
				startActivity(intent);
				finish();
			} else {
				com.google.android.material.snackbar.Snackbar.make(binding.getRoot(),
				"Validation failed! Check URL or Token.",
				com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
			}
		});
		
	}
	
}