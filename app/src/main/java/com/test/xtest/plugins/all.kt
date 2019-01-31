package com.test.xtest.plugins

import android.util.Log
import com.gh0u1l5.wechatmagician.spellbook.C
import com.gh0u1l5.wechatmagician.spellbook.WechatGlobal
import com.gh0u1l5.wechatmagician.spellbook.util.ReflectionUtil
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.*

fun mylog(message: String) {
    Log.i("walle", message)
}


object Log {
    private const val loggerClass = "com.tencent.mm.sdk.platformtools.ab"
    private const val xloggerClass = "com.tencent.mars.xlog.Xlog"
    private const val loggerImplementClass = "com.tencent.mm.sdk.platformtools.ab\$1"
    private const val loggerInterface = "com.tencent.mm.sdk.platformtools.ab\$a"

    @JvmStatic fun findClassIfExists(className: String, classLoader: ClassLoader): Class<*>? {
        try {
            return Class.forName(className, false, classLoader)
        } catch (throwable: Throwable) {
            if (WechatGlobal.wxUnitTestMode) {
                throw throwable
            }
        }
        return null
    }

    private fun hookLogFunction(cls: String, methodName: String, lpparam: XC_LoadPackage.LoadPackageParam) {
        findAndHookMethod( cls, lpparam.classLoader, methodName,
            C.String, C.String, C.String, C.Int, C.Int, C.Long, C.Long, C.String,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    mylog("$methodName: [${param.args[0]}] ${param.args[7]}")
                }
            })
    }

    private fun hookGetLogLever(lpparam: XC_LoadPackage.LoadPackageParam) {
        findAndHookMethod( loggerImplementClass, lpparam.classLoader, "getLogLevel",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = 0
                }
            })
    }

    private fun hookLogSet(lpparam: XC_LoadPackage.LoadPackageParam) {
        findAndHookMethod( loggerClass, lpparam.classLoader, "a", this.findClassIfExists(loggerInterface, lpparam.classLoader),
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    mylog("set logger: ${param.args[0]}")
                }
            })
    }

    private fun hookRecv(lpparam: XC_LoadPackage.LoadPackageParam) {
        findAndHookMethod("com.tencent.mm.modelmulti.o\$d", lpparam.classLoader, "c", Queue::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    mylog("[modelmulti.o][recv] ${param.args[0]}")
                    Thread.dumpStack()
                }
            })

        val classAddMsgInfo = this.findClassIfExists("com.tencent.mm.ah.e\$a", lpparam.classLoader)
        val classsMessageBodyContent = this.findClassIfExists("com.tencent.mm.protocal.protobuf.cj", lpparam.classLoader)
        fun logFouondation(message: String) = mylog("[foundation.c] [recv] $message")
        findAndHookMethod("com.tencent.mm.plugin.messenger.foundation.c", lpparam.classLoader, "a",
            classAddMsgInfo,
            this.findClassIfExists("com.tencent.mm.plugin.messenger.foundation.a.t", lpparam.classLoader),
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    Thread.dumpStack()
                    logFouondation("${param.args[0]} ${param.args[1]}")
                    val p1 = param.args[0]
                    val messageBody = ReflectionUtil.findFieldIfExists(classAddMsgInfo!!, "eiM")?.get(p1)
                    if(messageBody == null) {
                        logFouondation("eiM not found.")
                        return
                    }
                    if(classsMessageBodyContent == null) {
                        logFouondation("classMessageBodyContent not found")
                        return
                    }
                    val messageBodyContent = ReflectionUtil.findFieldIfExists(classsMessageBodyContent, "uzW")?.get(messageBody)
                    if(messageBodyContent == null) {
                        logFouondation("uzW not found.")
                        return
                    }
                    logFouondation("$messageBodyContent")
                }
            })
    }

    fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        this.hookGetLogLever(lpparam)
        this.hookLogSet(lpparam)

        arrayOf("logV", "logI", "logD", "logW", "logE", "logF").forEach {
            this.hookLogFunction(loggerImplementClass, it, lpparam)
            this.hookLogFunction(xloggerClass, it, lpparam)
        }

        this.hookRecv(lpparam)
    }
}

