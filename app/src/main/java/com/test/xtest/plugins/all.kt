package com.test.xtest.plugins

import android.content.ContentValues
import com.gh0u1l5.wechatmagician.spellbook.C
import com.gh0u1l5.wechatmagician.spellbook.base.Operation
import com.gh0u1l5.wechatmagician.spellbook.interfaces.IDatabaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage

fun mylog(message: String) {
    XposedBridge.log("windy: $message")
}

//object Message : IDatabaseHook {
//    override fun onDatabaseInserted(thisObject: Any, table: String, nullColumnHack: String?, initialValues: ContentValues?, conflictAlgorithm: Int, result: Long?): Operation<Long> {
//        if (table == "message") {
//            mylog("initialValues: $initialValues")
//            mylog("thisObject: $thisObject")
//            mylog("table: $table")
//            mylog("result: $result")
//        }
//        return super.onDatabaseInserted(thisObject, table, nullColumnHack, initialValues, conflictAlgorithm, result)
//    }
//}

object Log {

    private fun hookLog(methodName: String, lpparam: XC_LoadPackage.LoadPackageParam) {
        findAndHookMethod(
            "com.tencent.mm.sdk.platformtools.ab\$1",
            lpparam.classLoader,
            methodName,
            C.String, C.String, C.String, C.Int, C.Int, C.Long, C.Long, C.String, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    mylog("$methodName: ${param.args[0]} | ${param.args[1]} | ${param.args[2]} | ${param.args[3]} | " +
                            "${param.args[4]} | ${param.args[5]} | ${param.args[6]} | ${param.args[7]}")
                }
            })
    }

    private fun hookLogLever(lpparam: XC_LoadPackage.LoadPackageParam) {
        findAndHookMethod("com.tencent.mm.sdk.platformtools.ab\$1",
            lpparam.classLoader,
            "getLogLevel", object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = 0
                }
            })
    }

    fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        this.hookLog("logV", lpparam)
        this.hookLog("logI", lpparam)
        this.hookLog("logD", lpparam)
        this.hookLog("logW", lpparam)
        this.hookLog("logE", lpparam)
        this.hookLog("logF", lpparam)
        this.hookLogLever(lpparam)
    }
}

