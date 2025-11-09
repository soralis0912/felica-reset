package org.soralis.felica_reset;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class RootUtil {
    
    private static final String TAG = "RootUtil";
    
    /**
     * Root権限が利用可能かチェック
     * suコマンドを発行してみるだけのシンプルな実装
     */
    public static boolean isRootAvailable() {
        Log.d(TAG, "Starting root availability check using su command");
        
        Process process = null;
        DataOutputStream os = null;
        
        try {
            // suコマンドを実行してみる
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            
            // idコマンドを実行してroot権限を確認
            os.writeBytes("id\n");
            os.writeBytes("exit\n");
            os.flush();
            
            // プロセスの終了を待つ（タイムアウト付き）
            boolean finished = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!finished) {
                Log.w(TAG, "su command timed out");
                process.destroyForcibly();
                return false;
            }
            
            int exitCode = process.exitValue();
            boolean result = (exitCode == 0);
            
            Log.d(TAG, "su command exit code: " + exitCode + ", root available: " + result);
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error during root availability check", e);
            return false;
        } finally {
            try {
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (Exception e) {
                Log.w(TAG, "Error cleaning up resources", e);
            }
        }
    }
    
    /**
     * Root管理システムの種類を判定する
     */
    public static String detectRootManager() {
        Log.d(TAG, "Detecting root manager type");
        
        // su -v で Root管理システムを統一的に判定
        try {
            CommandResult suVersion = executeRootCommand("su -v");
            String combined = "";
            if (suVersion.output != null) combined += suVersion.output;
            if (suVersion.error != null) combined += suVersion.error;

            if (suVersion.success && !combined.trim().isEmpty()) {
                String trimmed = combined.trim();
                Log.d(TAG, "su -v output: " + trimmed);
                
                // Magisk判定: "数字.数字:MAGISKSU" または "数字.数字.数字:MAGISKSU" の形式
                if (trimmed.matches(".*\\d+(?:\\.\\d+)*:MAGISKSU.*")) {
                    Log.d(TAG, "Magisk detected via 'su -v': " + trimmed);
                    return "magisk";
                }
                
                // KernelSU判定: "数字.数字.数字:KernelSU" の形式
                if (trimmed.matches(".*\\d+\\.\\d+\\.\\d+:KernelSU.*")) {
                    Log.d(TAG, "KernelSU detected via 'su -v': " + trimmed);
                    return "kernelsu";
                }
                
                // フォールバック: 大文字小文字を区別せずに文字列を検索
                String lower = trimmed.toLowerCase();
                if (lower.contains("magisk")) {
                    Log.d(TAG, "Magisk detected via string match: " + trimmed);
                    return "magisk";
                }
                if (lower.contains("kernelsu")) {
                    Log.d(TAG, "KernelSU detected via string match: " + trimmed);
                    return "kernelsu";
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error while checking 'su -v' for root manager", e);
        }

        // 従来のMagisk判定をフォールバックとして保持
        try {
            CommandResult magiskResult = executeRootCommand("magisk -v");
            if (magiskResult.success && !magiskResult.output.isEmpty()) {
                Log.d(TAG, "Magisk detected via 'magisk -v': " + magiskResult.output.trim());
                return "magisk";
            }
        } catch (Exception e) {
            Log.w(TAG, "Error while checking 'magisk -v'", e);
        }
        
        // その他のRoot管理システム
        Log.d(TAG, "Generic root manager detected");
        return "generic";
    }
    
    /**
     * Root権限でコマンドを実行
     */
    public static CommandResult executeRootCommand(String command) {
        return executeRootCommand(new String[]{command});
    }
    
    /**
     * Root権限で複数のコマンドを実行
     */
    public static CommandResult executeRootCommand(String[] commands) {
        Process process = null;
        DataOutputStream os = null;
        BufferedReader is = null;
        BufferedReader es = null;
        
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            is = new BufferedReader(new InputStreamReader(process.getInputStream()));
            es = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            // コマンドを実行
            for (String command : commands) {
                Log.d(TAG, "Executing: " + command);
                os.writeBytes(command + "\n");
                os.flush();
            }
            
            os.writeBytes("exit\n");
            os.flush();
            
            // 結果を読み取り
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            String line;
            
            while ((line = is.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            while ((line = es.readLine()) != null) {
                error.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            CommandResult result = new CommandResult(
                exitCode == 0,
                exitCode,
                output.toString(),
                error.toString()
            );
            
            Log.d(TAG, "Command result: success=" + result.success + ", exitCode=" + result.exitCode);
            if (!result.output.isEmpty()) {
                Log.d(TAG, "Output: " + result.output);
            }
            if (!result.error.isEmpty()) {
                Log.w(TAG, "Error: " + result.error);
            }
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing root command", e);
            return new CommandResult(false, -1, "", e.getMessage());
        } finally {
            try {
                if (os != null) os.close();
                if (is != null) is.close();
                if (es != null) es.close();
                if (process != null) process.destroy();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }
    
    /**
     * コマンド実行結果を格納するクラス
     */
    public static class CommandResult {
        public final boolean success;
        public final int exitCode;
        public final String output;
        public final String error;
        
        public CommandResult(boolean success, int exitCode, String output, String error) {
            this.success = success;
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }
    }
}
