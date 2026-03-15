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
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.codebysonu.gitpushpro.databinding.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class CommitprogressActivity extends AppCompatActivity {
	
	private CommitprogressBinding binding;
	ArrayList<HashMap<String, Object>> listMapHistory;
	ArrayList<HashMap<String, Object>> fullHistoryList;
	
	private Intent intent = new Intent();
	private SharedPreferences sp;
	private SharedPreferences spHistory;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = CommitprogressBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		sp = getSharedPreferences("sp", Activity.MODE_PRIVATE);
		spHistory = getSharedPreferences("spHistory", Activity.MODE_PRIVATE);
		
		binding.imgMenu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				
				if (!binding.drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
					binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
				}
				
				
				final android.view.View drawerView = findViewById(R.id.linear_root);
				
				
				String fullRepo = sp.getString("repo", "");
				String username = "User";
				String repoNameOnly = "Repository";
				
				if (fullRepo.contains("/")) {
					String[] parts = fullRepo.split("/");
					username = parts[0];
					repoNameOnly = parts[1];
				}
				
				String folderPath = sp.getString("folder", "");
				
				String displayPath = repoNameOnly + (folderPath.isEmpty() ? "" : "/" + folderPath);
				
				android.widget.TextView usernameTv = drawerView.findViewById(R.id.username_tv);
				android.widget.TextView repoPathTv = drawerView.findViewById(R.id.repo_path_tv);
				
				usernameTv.setText(username);
				repoPathTv.setText(displayPath);
				
				
				com.google.android.material.card.MaterialCardView navTerminal = drawerView.findViewById(R.id.nav_terminal);
				android.widget.ImageView imgTerminal = drawerView.findViewById(R.id.img_terminal);
				android.widget.TextView txtTerminal = drawerView.findViewById(R.id.txt_terminal);
				
				navTerminal.setCardBackgroundColor(android.graphics.Color.parseColor("#1B2A16"));
				navTerminal.setStrokeWidth(1);
				navTerminal.setStrokeColor(android.graphics.Color.parseColor("#2E4D25"));
				
				imgTerminal.setColorFilter(android.graphics.Color.parseColor("#52D02E"));
				txtTerminal.setTextColor(android.graphics.Color.parseColor("#52D02E"));
				txtTerminal.setTypeface(null, android.graphics.Typeface.BOLD);
				
				
				
				
				drawerView.findViewById(R.id.nav_select_files).setOnClickListener(v -> {
					intent.setClass(getApplicationContext(), GitpushActivity.class);
					startActivity(intent);
				});
				
				
				drawerView.findViewById(R.id.nav_switch_repo).setOnClickListener(v -> {
					intent.setClass(getApplicationContext(), MainActivity.class);
					intent.putExtra("mod", "switch");
					startActivity(intent);
					finish();
				});
				
				
				drawerView.findViewById(R.id.nav_download_swb).setOnClickListener(v -> {
					android.net.Uri uri = android.net.Uri.parse("https://codebysonu95-rgb.github.io/SketchCode");
					Intent webIntent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(webIntent);
				});
				
				
				drawerView.findViewById(R.id.nav_about).setOnClickListener(v -> {
					new com.google.android.material.dialog.MaterialAlertDialogBuilder(CommitprogressActivity.this)
					.setTitle("GitPush Pro")
					.setMessage("Developed by CodeBySonu\n\nThis app was created to demonstrate advanced GitHub push functionality on mobile.\n\nYou can download the complete working source (.SWB) from SketchCode.\n\nIncludes full UI, logic, and documentation.\n\n© CodeBySonu")
					.setPositiveButton("OK", null)
					.show();
				});
				
			}
		});
		
		binding.imageviewFabIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), GitpushActivity.class);
				startActivity(intent);
			}
		});
	}
	
	private void initializeLogic() {
		
		String token = sp.getString("token", "");
		String repo = sp.getString("repo", "");
		String folder = sp.getString("folder", "");
		String branch = sp.getString("branch", "main");
		
		final ArrayList<HashMap<String, Object>> filesToPush = (ArrayList<HashMap<String, Object>>) getIntent().getSerializableExtra("files");
		
		
		String savedHistory = spHistory.getString("history_json", "[]");
		try {
			java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType();
			fullHistoryList = new com.google.gson.Gson().fromJson(savedHistory, type);
			listMapHistory = new ArrayList<>(fullHistoryList);
		} catch (Exception e) {
			fullHistoryList = new ArrayList<>();
			listMapHistory = new ArrayList<>();
		}
		
		
		binding.repoConnectedTv.setText("CONNECTED");
		binding.websiteBranchTv.setText("github.com/" + branch);
		binding.branchInfoTv.setText("Branch: " + branch);
		binding.progressbar1.setProgress(0);
		binding.percentageTv.setText("0%");
		
		final String terminalHeader = "[$] Welcome back.\n[$] System ready. Waiting for files...";
		binding.terminalOutputTv.setText(terminalHeader);
		
		if (filesToPush == null || filesToPush.isEmpty()) {
			binding.tvPushStatus.setText("NO ACTIVE PUSH");
			binding.tvPushStatus.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
			binding.stutusDot.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"));
			binding.relativelayout4.setVisibility(android.view.View.GONE);
		} else {
			binding.tvPushStatus.setText("ACTIVE PUSHES");
			binding.tvPushStatus.setTextColor(android.graphics.Color.parseColor("#52D02E"));
			binding.stutusDot.setBackgroundColor(android.graphics.Color.parseColor("#52D02E"));
			binding.relativelayout4.setVisibility(android.view.View.VISIBLE);
			binding.txtFileName.setText(String.valueOf(filesToPush.get(0).get("name")));
		}
		
		
		binding.recyclerview1.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
		
		class CommitAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CommitAdapter.ViewHolder> {
			@Override
			public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
				android.view.View v = getLayoutInflater().inflate(R.layout.item_commit, parent, false);
				return new ViewHolder(v);
			}
			
			@Override
			public void onBindViewHolder(ViewHolder holder, int position) {
				final int itemPos = position;
				HashMap<String, Object> map = listMapHistory.get(itemPos);
				boolean isSuccess = "success".equals(map.get("status"));
				
				holder.statusTv.setText(String.valueOf(map.get("message")));
				holder.timeTv.setText(String.valueOf(map.get("time")));
				holder.branchTag.setText(String.valueOf(map.get("branch")));
				holder.hashTag.setText(String.valueOf(map.get("hash")));
				
				if (isSuccess) {
					holder.statusIconBg.setCardBackgroundColor(android.graphics.Color.parseColor("#1B2A16"));
					holder.statusImg.setImageResource(R.drawable.icon_check_circle);
					holder.statusImg.setColorFilter(android.graphics.Color.parseColor("#52D02E"));
					holder.errorMsg.setVisibility(android.view.View.GONE);
				} else {
					holder.statusIconBg.setCardBackgroundColor(android.graphics.Color.parseColor("#2A1616"));
					holder.statusImg.setImageResource(R.drawable.icon_error);
					holder.statusImg.setColorFilter(android.graphics.Color.parseColor("#FF5252"));
					holder.errorMsg.setVisibility(android.view.View.VISIBLE);
					holder.errorMsg.setText(String.valueOf(map.get("error")));
				}
				
				int count = (int)((double)map.get("count"));
				holder.fileCountTv.setText(count + " files changed");
				
				String fName = String.valueOf(map.get("fileName")).toLowerCase();
				if (count > 1) {
					holder.fileImg.setImageResource(R.drawable.icon_merge);
				} else {
					if (fName.endsWith(".apk")) holder.fileImg.setImageResource(R.drawable.icon_android);
					else if (fName.endsWith(".png") || fName.endsWith(".jpg") || fName.endsWith(".jpeg")) holder.fileImg.setImageResource(R.drawable.icon_image);
					else holder.fileImg.setImageResource(R.drawable.icon_insert_drive_file);
				}
				
				
				holder.itemView.setOnLongClickListener(v -> {
					HashMap<String, Object> targetMap = listMapHistory.get(itemPos);
					
					
					fullHistoryList.remove(targetMap);
					listMapHistory.remove(itemPos);
					
					
					spHistory.edit().putString("history_json", new com.google.gson.Gson().toJson(fullHistoryList)).apply();
					notifyDataSetChanged();
					
					com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Commit history deleted", 2000).show();
					return true;
				});
			}
			
			@Override
			public int getItemCount() { return listMapHistory.size(); }
			
			class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
				android.widget.TextView statusTv, timeTv, fileCountTv, branchTag, hashTag, errorMsg;
				android.widget.ImageView statusImg, fileImg;
				com.google.android.material.card.MaterialCardView statusIconBg;
				ViewHolder(android.view.View v) {
					super(v);
					statusTv = v.findViewById(R.id.stutus_tv);
					timeTv = v.findViewById(R.id.txt_time);
					fileCountTv = v.findViewById(R.id.file_count_tv);
					branchTag = v.findViewById(R.id.txt_tag_branch);
					hashTag = v.findViewById(R.id.txt_tag_hash);
					errorMsg = v.findViewById(R.id.txt_error_msg);
					statusImg = v.findViewById(R.id.img_status);
					fileImg = v.findViewById(R.id.file_icon);
					statusIconBg = v.findViewById(R.id.card_status_icon);
				}
			}
		}
		
		final CommitAdapter historyAdapter = new CommitAdapter();
		binding.recyclerview1.setAdapter(historyAdapter);
		
		
		binding.btnClear.setOnClickListener(v -> {
			binding.terminalOutputTv.setText(terminalHeader);
			com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Terminal logs cleared", 0).show();
		});
		
		
		binding.btnShort.setOnClickListener(v -> {
			binding.imgFilter.animate().rotation(180f).setDuration(300).start();
			
			final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(CommitprogressActivity.this);
			android.view.View bottomSheetView = getLayoutInflater().inflate(R.layout.short_bottom_sheet, null);
			bottomSheet.setContentView(bottomSheetView);
			
			android.widget.LinearLayout layoutNewest = bottomSheetView.findViewById(R.id.layout_newest);
			android.widget.LinearLayout layoutSuccess = bottomSheetView.findViewById(R.id.layout_sucess);
			android.widget.LinearLayout layoutFailed = bottomSheetView.findViewById(R.id.layout_failed);
			android.widget.RadioButton rbNewest = bottomSheetView.findViewById(R.id.rb_newest);
			android.widget.RadioButton rbSuccess = bottomSheetView.findViewById(R.id.rb_sucess);
			android.widget.RadioButton rbFailed = bottomSheetView.findViewById(R.id.rb_failed);
			
			String currentFilter = binding.filterTv.getText().toString();
			rbNewest.setChecked(currentFilter.equals("Newest First"));
			rbSuccess.setChecked(currentFilter.equals("Successfully Pushed"));
			rbFailed.setChecked(currentFilter.equals("Failed Pushes Only"));
			
			bottomSheet.setOnDismissListener(dialog -> {
				binding.imgFilter.animate().rotation(0f).setDuration(300).start();
			});
			
			layoutNewest.setOnClickListener(v1 -> {
				binding.filterTv.setText("Newest First");
				listMapHistory.clear();
				listMapHistory.addAll(fullHistoryList);
				historyAdapter.notifyDataSetChanged();
				bottomSheet.dismiss();
			});
			
			layoutSuccess.setOnClickListener(v1 -> {
				binding.filterTv.setText("Successfully Pushed");
				listMapHistory.clear();
				for (java.util.HashMap<String, Object> map : fullHistoryList) {
					if ("success".equals(map.get("status"))) listMapHistory.add(map);
				}
				historyAdapter.notifyDataSetChanged();
				bottomSheet.dismiss();
			});
			
			layoutFailed.setOnClickListener(v1 -> {
				binding.filterTv.setText("Failed Pushes Only");
				listMapHistory.clear();
				for (java.util.HashMap<String, Object> map : fullHistoryList) {
					if ("error".equals(map.get("status"))) listMapHistory.add(map);
				}
				historyAdapter.notifyDataSetChanged();
				bottomSheet.dismiss();
			});
			
			bottomSheet.show();
		});
		
		
		if (filesToPush != null && !filesToPush.isEmpty()) {
			GitHubHelper helper = new GitHubHelper(this, token, repo, folder, branch, new GitHubHelper.GitCallback() {
				@Override
				public void onUpdate(String message) {
					runOnUiThread(() -> {
						binding.terminalOutputTv.append("\n" + message);
						if (message.contains("[$] Staging: ")) {
							binding.txtFileName.setText(message.replace("[$] Staging: ", ""));
						}
						binding.vscroll2.post(() -> binding.vscroll2.fullScroll(android.view.View.FOCUS_DOWN));
					});
				}
				
				@Override public void onProgress(int percent) {
					runOnUiThread(() -> {
						binding.progressbar1.setProgress(percent);
						binding.percentageTv.setText(percent + "%");
					});
				}
				
				@Override
				public void onComplete() {
					runOnUiThread(() -> {
						binding.tvPushStatus.setText("PUSH COMPLETED");
						binding.tvPushStatus.setTextColor(android.graphics.Color.parseColor("#52D02E"));
						binding.stutusDot.setBackgroundColor(android.graphics.Color.parseColor("#52D02E"));
						binding.relativelayout4.setVisibility(android.view.View.GONE);
						
						java.util.Calendar cal = java.util.Calendar.getInstance();
						String time = String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
						
						HashMap<String, Object> newItem = new HashMap<>();
						newItem.put("status", "success");
						newItem.put("message", "Commit Push successful");
						newItem.put("time", time);
						newItem.put("branch", branch);
						newItem.put("hash", "sha-" + String.valueOf(System.currentTimeMillis()).substring(7));
						newItem.put("count", (double)filesToPush.size());
						newItem.put("fileName", filesToPush.get(0).get("name"));
						
						fullHistoryList.add(0, newItem);
						listMapHistory.add(0, newItem);
						
						spHistory.edit().putString("history_json", new com.google.gson.Gson().toJson(fullHistoryList)).apply();
						historyAdapter.notifyDataSetChanged();
					});
				}
				
				@Override
				public void onError(String error) {
					runOnUiThread(() -> {
						binding.tvPushStatus.setText("FAILED PUSH");
						binding.tvPushStatus.setTextColor(android.graphics.Color.parseColor("#FF5252"));
						binding.stutusDot.setBackgroundColor(android.graphics.Color.parseColor("#FF5252"));
						
						java.util.Calendar cal = java.util.Calendar.getInstance();
						String time = String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
						
						HashMap<String, Object> newItem = new HashMap<>();
						newItem.put("status", "error");
						newItem.put("message", "Push Interrupted");
						newItem.put("error", error);
						newItem.put("time", time);
						newItem.put("branch", branch);
						newItem.put("hash", "------");
						newItem.put("count", (double)filesToPush.size());
						newItem.put("fileName", "");
						
						fullHistoryList.add(0, newItem);
						listMapHistory.add(0, newItem);
						
						spHistory.edit().putString("history_json", new com.google.gson.Gson().toJson(fullHistoryList)).apply();
						historyAdapter.notifyDataSetChanged();
					});
				}
			});
			helper.startPush(filesToPush);
		}
		
	}
	
	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Recyclerview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.item_commit, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			ItemCommitBinding binding = ItemCommitBinding.bind(_view);
		}
		
		@Override
		public int getItemCount() {
			return _data.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}
}