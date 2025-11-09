package org.soralis.felica_reset;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    
    private TextView statusText;
    private Button resetButton;
    private boolean isRootAvailable = false;
    private ExecutorService executor;
    private Handler mainHandler;
    private StringBuilder statusLog = new StringBuilder();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate started");
        
        try {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "super.onCreate completed");
            
            setContentView(R.layout.activity_main);
            Log.d(TAG, "setContentView completed");
            
            // UI要素の初期化
            statusText = findViewById(R.id.status_text);
            resetButton = findViewById(R.id.reset_button);
            Log.d(TAG, "findViewById completed - statusText: " + (statusText != null) + ", resetButton: " + (resetButton != null));
            
            if (statusText == null || resetButton == null) {
                Log.e(TAG, "Failed to find UI elements - statusText: " + statusText + ", resetButton: " + resetButton);
                finish();
                return;
            }
            
            // 初期状態の設定
            statusText.setText("初期化中...");
            resetButton.setEnabled(false);
            Log.d(TAG, "UI elements initialized");
            
            // Executorの初期化
            executor = Executors.newSingleThreadExecutor();
            mainHandler = new Handler(Looper.getMainLooper());
            Log.d(TAG, "Executor and handler initialized");
            
            // ボタンのクリックリスナー
            resetButton.setOnClickListener(v -> {
                Log.d(TAG, "Reset button clicked");
                performReset();
            });
            Log.d(TAG, "Click listener set");
            
            // Root権限チェックを遅延実行
            mainHandler.postDelayed(() -> {
                Log.d(TAG, "Starting delayed root check");
                checkRootAccess();
            }, 1000);
            
            Log.d(TAG, "onCreate completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create activity", e);
            try {
                if (statusText != null) {
                    statusText.setText("エラーが発生しました: " + e.getMessage());
                }
            } catch (Exception ignored) {
                // Ignore secondary errors
            }
            // finish(); // Don't finish immediately, let user see the error
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
        Log.d(TAG, "Activity destroyed");
    }
    
    private void appendStatusLog(String message) {
        if (statusLog.length() > 0) {
            statusLog.append("\n");
        }
        statusLog.append(message);
        
        if (statusText != null) {
            statusText.setText(statusLog.toString());
        }
    }
    
    private void clearStatusLog() {
        statusLog.setLength(0);
        if (statusText != null) {
            statusText.setText("");
        }
    }
    
    private void checkRootAccess() {
        Log.d(TAG, "checkRootAccess started");
        
        if (executor == null || mainHandler == null) {
            Log.e(TAG, "Executor or handler is null");
            updateStatus();
            return;
        }
        
        executor.execute(() -> {
            try {
                Log.d(TAG, "Checking root access in background thread");
                boolean rootAvailable = RootUtil.isRootAvailable();
                Log.d(TAG, "Root check completed: " + rootAvailable);
                
                mainHandler.post(() -> {
                    try {
                        isRootAvailable = rootAvailable;
                        Log.d(TAG, "Updating UI with root status: " + rootAvailable);
                        updateStatus();
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI with root status", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error checking root access", e);
                mainHandler.post(() -> {
                    try {
                        isRootAvailable = false;
                        updateStatus();
                        if (statusText != null) {
                            statusText.setText("Root権限チェックでエラーが発生しました");
                        }
                    } catch (Exception ignored) {
                        // Ignore secondary errors
                    }
                });
            }
        });
    }
    
    private void updateStatus() {
        Log.d(TAG, "updateStatus called");
        
        try {
            if (statusText == null || resetButton == null) {
                Log.w(TAG, "UI elements are null - statusText: " + statusText + ", resetButton: " + resetButton);
                return;
            }
            
            if (isRootAvailable) {
                statusText.setText("Root権限が利用可能です - リセット実行可能");
                resetButton.setEnabled(true);
                Log.d(TAG, "Status updated: Root available");
            } else {
                statusText.setText("Root権限が必要です - デバイスをroot化してください");
                resetButton.setEnabled(false);
                Log.d(TAG, "Status updated: Root not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating status", e);
        }
    }
    
    private void performReset() {
        Log.d(TAG, "performReset called");
        
        try {
            if (!isRootAvailable) {
                Toast.makeText(this, "Root権限が必要です", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (statusText == null || resetButton == null) {
                Log.e(TAG, "UI elements are null during reset");
                return;
            }
            
            clearStatusLog();
            appendStatusLog("リセットを実行中...");
            resetButton.setEnabled(false);
            
            if (executor == null || mainHandler == null) {
                Log.e(TAG, "Executor or handler is null during reset");
                statusText.setText("システムエラーが発生しました");
                resetButton.setEnabled(true);
                return;
            }
            
            executor.execute(() -> {
                try {
                    Log.d(TAG, "Executing reset in background thread");
                    RootUtil.CommandResult result = performSystemLevelReset();
                    
                    mainHandler.post(() -> {
                        try {
                            if (resetButton != null) {
                                resetButton.setEnabled(true);
                            }
                            
                            if (result.success) {
                                appendStatusLog("リセットが完了しました");
                                Toast.makeText(MainActivity.this, "リセットが完了しました", Toast.LENGTH_LONG).show();
                            } else {
                                appendStatusLog("リセットに失敗しました");
                                appendStatusLog("エラー: " + result.error);
                                if (!result.output.isEmpty()) {
                                    appendStatusLog("出力: " + result.output);
                                }
                                Toast.makeText(MainActivity.this, "リセットに失敗しました: " + result.error, Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Reset failed: " + result.error);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI after reset", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error during reset execution", e);
                    mainHandler.post(() -> {
                        try {
                            if (resetButton != null) resetButton.setEnabled(true);
                            appendStatusLog("リセット中にエラーが発生しました");
                            appendStatusLog("例外: " + e.getMessage());
                            if (e.getCause() != null) {
                                appendStatusLog("原因: " + e.getCause().getMessage());
                            }
                        } catch (Exception ignored) {
                            // Ignore secondary errors
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in performReset", e);
        }
    }
    
    private RootUtil.CommandResult performSystemLevelReset() {
        // Root権限を使用してFeliCaメモリクリアActivityを起動
        
        Log.d(TAG, "Performing FeliCa memory clear with root privileges");
        
        // UI更新: Root権限昇格の通知
        mainHandler.post(() -> {
            appendStatusLog("su権限に昇格しました");
        });
        
        // Root管理システムの種類を判定
        String rootManager = RootUtil.detectRootManager();
        Log.d(TAG, "Root manager detected: " + rootManager);
        
        // UI更新: Root管理システムの表示
        mainHandler.post(() -> {
            appendStatusLog("Root管理システム: " + rootManager);
        });
        
        // com.android.settingsのUIDを取得（appIdフィールドから）
        RootUtil.CommandResult uidResult = RootUtil.executeRootCommand(
            "dumpsys package com.android.settings | grep -E 'userId|appId' | head -1 | sed 's/.*appId=\\([0-9]*\\).*/\\1/'"
        );
        
        if (!uidResult.success || uidResult.output.trim().isEmpty()) {
            Log.e(TAG, "Failed to get settings UID: " + uidResult.error);
            
            // UI更新: UID取得失敗の通知
            mainHandler.post(() -> {
                appendStatusLog("SettingsアプリのUID取得に失敗");
                appendStatusLog("フォールバック実行中...");
            });
            
            // フォールバックとして直接実行
            return RootUtil.executeRootCommand("am start -a android.intent.action.MAIN -n com.felicanetworks.mfm.main/com.felicanetworks.mfm.memory_clear.MemoryClearActivity --activity-brought-to-front");
        }
        
        String settingsUid = uidResult.output.trim();
        Log.d(TAG, "Settings UID: " + settingsUid);
        
        // UI更新: UID取得成功の通知
        mainHandler.post(() -> {
            appendStatusLog("SettingsアプリのUID: " + settingsUid);
        });
        
        // Root管理システムに応じてコマンドを実行
        switch (rootManager) {
            case "magisk":
                Log.d(TAG, "Using Magisk su method with UID switching");
                
                // UI更新: Magisk使用とUID切り替えの通知
                mainHandler.post(() -> {
                    appendStatusLog("Magisk経由でUID " + settingsUid + " に切り替え中...");
                });
                
                // Magiskの場合は su $UID -c の形式で特定ユーザーとして実行
                RootUtil.CommandResult magiskResult = RootUtil.executeRootCommand(
                    "su " + settingsUid + " -c \"am start -a android.intent.action.MAIN -n com.felicanetworks.mfm.main/com.felicanetworks.mfm.memory_clear.MemoryClearActivity --activity-brought-to-front\""
                );
                
                // UI更新: Magisk実行結果の通知
                final String magiskStatus = magiskResult.success ? "成功" : "失敗";
                mainHandler.post(() -> {
                    appendStatusLog("UID切り替え: " + magiskStatus);
                });
                
                return magiskResult;
                
            case "kernelsu":
                Log.d(TAG, "Using KernelSU method");
                
                // UI更新: KernelSU使用の通知
                mainHandler.post(() -> {
                    appendStatusLog("KernelSU経由でUID " + settingsUid + " に切り替え中...");
                });
                
                RootUtil.CommandResult kernelsuResult = RootUtil.executeRootCommand(
                    "su -c \"am start -a android.intent.action.MAIN -n com.felicanetworks.mfm.main/com.felicanetworks.mfm.memory_clear.MemoryClearActivity --activity-brought-to-front\" -u " + settingsUid
                );
                
                // UI更新: KernelSU実行結果の通知
                final String kernelsuStatus = kernelsuResult.success ? "成功" : "失敗";
                mainHandler.post(() -> {
                    appendStatusLog("UID切り替え: " + kernelsuStatus);
                });
                
                return kernelsuResult;
                
            default:
                Log.d(TAG, "Using generic root method");
                
                // UI更新: 汎用Root方式の通知
                mainHandler.post(() -> {
                    appendStatusLog("汎用Root方式でUID切り替え試行中...");
                });
                
                // runuserやsetuidgidを試行
                RootUtil.CommandResult runuserResult = RootUtil.executeRootCommand("command -v runuser");
                if (runuserResult.success && !runuserResult.output.trim().isEmpty()) {
                    mainHandler.post(() -> {
                        appendStatusLog("runuserコマンドでUID切り替え中...");
                    });
                    
                    RootUtil.CommandResult result = RootUtil.executeRootCommand(
                        "runuser -u " + settingsUid + " -- am start -a android.intent.action.MAIN -n com.felicanetworks.mfm.main/com.felicanetworks.mfm.memory_clear.MemoryClearActivity --activity-brought-to-front"
                    );
                    
                    final String runuserStatus = result.success ? "成功" : "失敗";
                    mainHandler.post(() -> {
                        appendStatusLog("runuser UID切り替え: " + runuserStatus);
                    });
                    
                    return result;
                }
                
                RootUtil.CommandResult setuidgidResult = RootUtil.executeRootCommand("command -v setuidgid");
                if (setuidgidResult.success && !setuidgidResult.output.trim().isEmpty()) {
                    mainHandler.post(() -> {
                        appendStatusLog("setuidgidコマンドでUID切り替え中...");
                    });
                    
                    RootUtil.CommandResult result = RootUtil.executeRootCommand(
                        "setuidgid " + settingsUid + " am start -a android.intent.action.MAIN -n com.felicanetworks.mfm.main/com.felicanetworks.mfm.memory_clear.MemoryClearActivity --activity-brought-to-front"
                    );
                    
                    final String setuidgidStatus = result.success ? "成功" : "失敗";
                    mainHandler.post(() -> {
                        appendStatusLog("setuidgid UID切り替え: " + setuidgidStatus);
                    });
                    
                    return result;
                }
                
                // 最終フォールバック
                Log.w(TAG, "No UID switching method available, running as root");
                
                mainHandler.post(() -> {
                    appendStatusLog("UID切り替え方法が見つからない");
                    appendStatusLog("Root権限で直接実行中...");
                });
                
                RootUtil.CommandResult fallbackResult = RootUtil.executeRootCommand(
                    "am start -a android.intent.action.MAIN -n com.felicanetworks.mfm.main/com.felicanetworks.mfm.memory_clear.MemoryClearActivity --activity-brought-to-front"
                );
                
                final String fallbackStatus = fallbackResult.success ? "成功" : "失敗";
                mainHandler.post(() -> {
                    appendStatusLog("Root直接実行: " + fallbackStatus);
                });
                
                return fallbackResult;
        }
    }
}
