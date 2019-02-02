package com.test.xtest

import com.gh0u1l5.wechatmagician.spellbook.SpellBook
import com.gh0u1l5.wechatmagician.spellbook.util.BasicUtil
import com.test.xtest.plugins.Log
import dalvik.system.PathClassLoader
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Hook: IXposedHookLoadPackage {
    companion object {
        const val PKG = "com.test.xtest"
        const val HOOK_CLASS = "com.test.xtest.Hook"
        const val HOOK_METHOD = "loadPlugins"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        BasicUtil.tryVerbosely {
            if (SpellBook.isImportantWechatProcess(lpparam)) {
                XposedBridge.log("Hello Wechat!")
                // this.loadPlugins(lpparam)
                this.startupOTA(lpparam)
            }
        }
    }

    fun loadPlugins(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("windy: start to load plugins")
//        SpellBook.startup(lpparam, listOf(Log))
        try {
            Log.hook(lpparam)
        } catch (e: Exception) {
            e.printStackTrace()
            log(e)
            throw e
        }
    }

    private fun startupOTA(lpparam: XC_LoadPackage.LoadPackageParam) {
        val apkPath = SpellBook.getApplicationApkPath(PKG)
        val classLoader = PathClassLoader(apkPath, ClassLoader.getSystemClassLoader())
        try {
            val cls = Class.forName(HOOK_CLASS, true, classLoader)
            val instance = cls.newInstance()
            val method = cls.getDeclaredMethod(HOOK_METHOD, XC_LoadPackage.LoadPackageParam::class.java)
            method.invoke(instance, lpparam)
        } catch (e: Exception) {
            e.printStackTrace()
            log(e)
        }
    }
}