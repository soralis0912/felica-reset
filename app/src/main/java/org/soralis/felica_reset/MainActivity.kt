package org.soralis.felica_reset

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private var statusText: TextView? = null
    private var resetButton: Button? = null
    private var executor: ExecutorService? = null
    private var mainHandler: Handler? = null
    private val statusLog = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate started")

        try {
            super.onCreate(savedInstanceState)

            setContentView(R.layout.activity_main)

            statusText = findViewById(R.id.status_text)
            resetButton = findViewById(R.id.reset_button)

            if (statusText == null || resetButton == null) {
                Log.e(TAG, "Failed to find UI elements - statusText: $statusText, resetButton: $resetButton")
                finish()
                return
            }

            statusText?.text = "初期化中..."
            resetButton?.isEnabled = false

            executor = Executors.newSingleThreadExecutor()
            mainHandler = Handler(Looper.getMainLooper())

            resetButton?.setOnClickListener {
                Log.d(TAG, "Reset button clicked")
                performReset()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create activity", e)
            try {
                statusText?.text = "エラーが発生しました: ${e.message}"
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor?.shutdown()
        Log.d(TAG, "Activity destroyed")
    }

    private fun appendStatusLog(message: String) {
        if (statusLog.isNotEmpty()) {
            statusLog.append("\n")
        }
        statusLog.append(message)
        statusText?.text = statusLog.toString()
    }

    private fun clearStatusLog() {
        statusLog.setLength(0)
        statusText?.text = ""
    }

    private fun updateStatus() {
        try {
            if (statusText == null || resetButton == null) {
                Log.w(TAG, "UI elements are null - statusText: $statusText, resetButton: $resetButton")
                return
            }
            statusText?.text = "リセット実行可能"
            resetButton?.isEnabled = true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating status", e)
        }
    }

    private fun performReset() {
        Log.d(TAG, "performReset called")

        try {
            if (statusText == null || resetButton == null) {
                Log.e(TAG, "UI elements are null during reset")
                return
            }

            clearStatusLog()
            appendStatusLog("リセットを実行中...")
            resetButton?.isEnabled = false

            if (executor == null || mainHandler == null) {
                Log.e(TAG, "Executor or handler is null during reset")
                statusText?.text = "システムエラーが発生しました"
                resetButton?.isEnabled = true
                return
            }

            executor?.execute {
                try {
                    Log.d(TAG, "Executing reset in background thread")

                    mainHandler?.post {
                        try {
                            appendStatusLog("通常の方法で Activity を起動します...")

                            val intent = Intent()
                            intent.setClassName(
                                "com.felicanetworks.mfm.main",
                                "com.felicanetworks.mfm.memory_clear.MemoryClearActivity"
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                            startActivityForResult(intent, 0)

                            resetButton?.isEnabled = true
                            appendStatusLog("Activity の起動を要求しました（成功）")
                            appendStatusLog("リセットが完了しました。アプリを再起動してください。")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start activity normally", e)
                            resetButton?.isEnabled = true
                            appendStatusLog("Activity 起動に失敗: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during reset execution", e)
                    mainHandler?.post {
                        try {
                            resetButton?.isEnabled = true
                            appendStatusLog("リセット中にエラーが発生しました")
                            appendStatusLog("例外: ${e.message}")
                            if (e.cause != null) {
                                appendStatusLog("原因: ${e.cause?.message}")
                            }
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in performReset", e)
        }
    }

}
