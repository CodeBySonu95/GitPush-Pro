package com.codebysonu.gitpushpro;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
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

public class GitpushActivity extends AppCompatActivity {
	
	public final int REQ_CD_FILEPICKER = 101;
	
	private GitpushBinding binding;
	private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
	
	private Intent intent = new Intent();
	private Intent FilePicker = new Intent(Intent.ACTION_GET_CONTENT);
	private SharedPreferences sp;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = GitpushBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
		} else {
			initializeLogic();
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1000) {
			initializeLogic();
		}
	}
	
	private void initialize(Bundle _savedInstanceState) {
		FilePicker.setType("*/*");
		FilePicker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		sp = getSharedPreferences("sp", Activity.MODE_PRIVATE);
		
		binding.imgBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), CommitprogressActivity.class);
				startActivity(intent);
			}
		});
		
		binding.btnSwitchRepo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), MainActivity.class);
				intent.putExtra("mod", "switch");
				startActivity(intent);
			}
		});
	}
	
	private void initializeLogic() {
		
		String repoPath = sp.getString("repo", "No Repository");
		String folderPathStr = sp.getString("folder", "");
		String branchNameStr = sp.getString("branch", "main");
		
		binding.txtRepoName.setText(repoPath);
		
		
		if (folderPathStr.isEmpty()) {
			binding.txtBranch.setText("Branch: " + branchNameStr + " (Root)");
		} else {
			binding.txtBranch.setText("Branch: " + branchNameStr + " | Path: /" + folderPathStr);
		}
		
		
		binding.recyclerviewFiles.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
		
		
		class LocalFileAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<LocalFileAdapter.ViewHolder> {
			@Override
			public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
				android.view.View v = getLayoutInflater().inflate(R.layout.item_staged_file, parent, false);
				return new ViewHolder(v);
			}
			
			@Override
			public void onBindViewHolder(ViewHolder holder, int position) {
				java.util.HashMap<String, Object> map = listMap.get(position);
				String fName = String.valueOf(map.get("name"));
				holder.name.setText(fName);
				holder.size.setText(String.valueOf(map.get("size")));
				
				String ext = fName.toLowerCase();
				if (ext.endsWith(".apk")) {
					holder.icon.setImageResource(R.drawable.icon_android);
				} else if (ext.endsWith(".jpg") || ext.endsWith(".jpeg") || ext.endsWith(".png") || ext.endsWith(".webp")) {
					holder.icon.setImageResource(R.drawable.icon_image);
				} else {
					holder.icon.setImageResource(R.drawable.icon_insert_drive_file);
				}
				
				holder.remove.setOnClickListener(v -> {
					listMap.remove(position);
					notifyDataSetChanged();
					binding.txtStagedCount.setText("STAGED FILES (" + listMap.size() + ")");
				});
			}
			
			@Override
			public int getItemCount() {
				return listMap.size();
			}
			
			class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
				android.widget.TextView name, size;
				android.widget.ImageView icon, remove;
				ViewHolder(android.view.View v) {
					super(v);
					name = v.findViewById(R.id.txt_file_name);
					size = v.findViewById(R.id.txt_file_size);
					icon = v.findViewById(R.id.file_type_icon);
					remove = v.findViewById(R.id.btn_remove);
				}
			}
		}
		
		final LocalFileAdapter localAdapter = new LocalFileAdapter();
		binding.recyclerviewFiles.setAdapter(localAdapter);
		
		
		binding.btnAddFiles.setOnClickListener(v -> {
			FilePicker.setType("*/*");
			FilePicker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
			startActivityForResult(FilePicker, REQ_CD_FILEPICKER);
		});
		
		binding.layoutUpload.setOnClickListener(v -> binding.btnAddFiles.performClick());
		
		
		binding.btnClearAll.setOnClickListener(v -> {
			listMap.clear();
			localAdapter.notifyDataSetChanged();
			binding.txtStagedCount.setText("STAGED FILES (0)");
			
			
			binding.guideTv.setText("Images, APKs, and other binary files are supported");
			binding.guideTv.setTextColor(android.graphics.Color.parseColor("#666666"));
		});
		
		
		binding.btnCommit.setOnClickListener(v -> {
			if (listMap.isEmpty()) {
				com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Stage some files first!", 0).show();
				return;
			}
			
			intent.setClass(getApplicationContext(), CommitprogressActivity.class);
			intent.putExtra("files", listMap);
			startActivity(intent);
		});
		
	}
	
	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);
		if (_requestCode == REQ_CD_FILEPICKER && _resultCode == Activity.RESULT_OK && _data != null) {
			long MAX_SIZE_BYTES = 25 * 1024 * 1024;
			boolean hasLargeFile = false;
			
			
			binding.guideTv.setText("Images, APKs, and other binary files are supported");
			binding.guideTv.setTextColor(android.graphics.Color.parseColor("#666666"));
			
			if (_data.getClipData() != null) {
				int count = _data.getClipData().getItemCount();
				for (int i = 0; i < count; i++) {
					android.net.Uri uri = _data.getClipData().getItemAt(i).getUri();
					
					String name = "unknown";
					String sizeStr = "0 B";
					android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
					if (cursor != null && cursor.moveToFirst()) {
						name = cursor.getString(cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME));
						long bytes = cursor.getLong(cursor.getColumnIndex(android.provider.OpenableColumns.SIZE));
						
						if (bytes > MAX_SIZE_BYTES) {
							hasLargeFile = true;
						} else {
							
							if (bytes < 1024) {
								sizeStr = bytes + " B";
							} else {
								int exp = (int) (Math.log(bytes) / Math.log(1024));
								char pre = "KMGTPE".charAt(exp - 1);
								sizeStr = String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
							}
							
							java.util.HashMap<String, Object> map = new java.util.HashMap<>();
							map.put("name", name);
							map.put("size", sizeStr);
							map.put("uri", uri.toString());
							listMap.add(map);
						}
						cursor.close();
					}
				}
			} else if (_data.getData() != null) {
				android.net.Uri uri = _data.getData();
				String name = "unknown";
				String sizeStr = "0 B";
				android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					name = cursor.getString(cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME));
					long bytes = cursor.getLong(cursor.getColumnIndex(android.provider.OpenableColumns.SIZE));
					
					if (bytes > MAX_SIZE_BYTES) {
						hasLargeFile = true;
					} else {
						if (bytes < 1024) {
							sizeStr = bytes + " B";
						} else {
							int exp = (int) (Math.log(bytes) / Math.log(1024));
							char pre = "KMGTPE".charAt(exp - 1);
							sizeStr = String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
						}
						
						java.util.HashMap<String, Object> map = new java.util.HashMap<>();
						map.put("name", name);
						map.put("size", sizeStr);
						map.put("uri", uri.toString());
						listMap.add(map);
					}
					cursor.close();
				}
			}
			
			if (hasLargeFile) {
				binding.guideTv.setText("Action Denied: One or more files exceed the 25MB limit. Please select smaller files.");
				binding.guideTv.setTextColor(android.graphics.Color.parseColor("#FF5252"));
				com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Some files were skipped (Too large)", 0).show();
			}
			
			if (binding.recyclerviewFiles.getAdapter() != null) {
				binding.recyclerviewFiles.getAdapter().notifyDataSetChanged();
			}
			binding.txtStagedCount.setText("STAGED FILES (" + listMap.size() + ")");
		}
		
		switch (_requestCode) {
			
			default:
			break;
		}
	}
	
	@Override
	public void onBackPressed() {
		
	}
}