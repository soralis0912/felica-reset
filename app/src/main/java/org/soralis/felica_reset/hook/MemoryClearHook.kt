package org.soralis.felica_reset.hook

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Xposed hook: force MemoryClearActivity#isCallerVerification() to return true
 * for package com.felicanetworks.mfm.
 *
 * Assumptions:
 * - This module will be loaded by Xposed in the target process.
 * - Project will be packaged/installed as an Xposed module (manifest/metadata not added here).
 */
public class MemoryClearHook() : IXposedHookLoadPackage {
    companion object {
        private const val TARGET_PKG = "com.felicanetworks.mfm.main"
        private const val TARGET_CLASS = "com.felicanetworks.mfm.memory_clear.MemoryClearActivity"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PKG) return

        try {
            XposedBridge.log("MemoryClearHook: attempting to hook $TARGET_CLASS#isCallerVerification")

            // Replace the implementation to always return true.
            XposedHelpers.findAndHookMethod(
                TARGET_CLASS,
                lpparam.classLoader,
                "isCallerVerification",
                XC_MethodReplacement.returnConstant(true)
            )

            XposedBridge.log("MemoryClearHook: hook installed successfully")
        } catch (t: Throwable) {
            XposedBridge.log("MemoryClearHook: failed to install hook: " + t.message)
        }
    }
}
