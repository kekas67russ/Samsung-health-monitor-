package com.gorba.wcsspoof

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "WCSSpoof"
        private const val TARGET_PACKAGE = "com.samsung.wearable.watchuniteplugin"
        private const val SPOOF_VALUE = "samsung"
        private const val TARGET_CLASS = "com.samsung.android.companionservice.capability.CapabilityExchangeMessage"
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) {
            return
        }

        Log.i(TAG, "handleLoadPackage: setting up method-level spoofing for $TARGET_PACKAGE")

        try {
            hookCapabilityExchangeMessage(lpparam.classLoader)
            Log.i(TAG, "Successfully hooked $TARGET_CLASS")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to hook capability message construction", t)
        }
    }

    private fun hookCapabilityExchangeMessage(classLoader: ClassLoader) {
        try {
            val messageClass = classLoader.loadClass(TARGET_CLASS)
            hookJsonCreation(messageClass, classLoader)
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Could not find $TARGET_CLASS, skipping method hook")
        }
    }

    private fun hookJsonCreation(messageClass: Class<*>, classLoader: ClassLoader) {
        try {
            val pairClass = classLoader.loadClass("kotlin.Pair")

            XposedHelpers.findAndHookMethod(
                pairClass,
                "<init>",
                Object::class.java,
                Object::class.java,
                object : XposedHelpers.MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val first = param.args[0]
                        val second = param.args[1]

                        if (first is String && first == "vender" && second is String) {
                            Log.d(TAG, "Intercepted Pair(\"vender\", \"$second\")")
                            param.args[1] = SPOOF_VALUE
                            Log.i(TAG, "Replaced vender value to \"$SPOOF_VALUE\"")
                        }
                    }
                }
            )

            Log.i(TAG, "Hooked kotlin.Pair constructor for vender substitution")
        } catch (e: Exception) {
            Log.w(TAG, "Could not hook Pair constructor: ${e.message}")
            Log.w(TAG, "Attempting fallback: generic Build.BRAND access hook")
        }
    }
}
