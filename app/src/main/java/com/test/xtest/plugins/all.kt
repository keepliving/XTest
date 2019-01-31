package com.test.xtest.plugins

import android.util.Log
import com.gh0u1l5.wechatmagician.spellbook.C
import com.gh0u1l5.wechatmagician.spellbook.WechatGlobal
import com.gh0u1l5.wechatmagician.spellbook.util.ReflectionUtil
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.*

object Log {
    private const val loggerClass = "com.tencent.mm.sdk.platformtools.ab"               // 这个类是日志的入口
    private const val xloggerClass = "com.tencent.mars.xlog.Xlog"                       // xlog会在一定时机替换掉下面的类
    private const val loggerImplementClass = "com.tencent.mm.sdk.platformtools.ab\$1"   // 这个类在被xlog替换之前起作用
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

    fun myLog(message: String) {
        Log.i("walle", message)
    }

    // 此hook能打印出Info级别以上的日志
    private fun hookLogFunctionAboveInfoLevel(cls: String, methodName: String, lpparam: XC_LoadPackage.LoadPackageParam) {
        findAndHookMethod( cls, lpparam.classLoader, methodName,
            C.String, C.String, C.String, C.Int, C.Int, C.Long, C.Long, C.String,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val level = methodName.last().toString()
                    val tag = param.args[0]
                    val content = param.args[7]
                    myLog("[$level] [$tag] $content")
                }
            })
    }

    // 这个很关键，必须hook，才能把debug级别的日志打印出来
    private fun hookLogFunctionBelowInfoLevel(level: String, lpparam: XC_LoadPackage.LoadPackageParam) {
        findAndHookMethod(loggerClass, lpparam.classLoader, level,
            C.String, C.String, Array<Any>::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val varargs = if(param.args.size > 2 && param.args[2] != null) {
                        param.args[2] as Array<*>
                    } else {
                        null
                    }

                    val tag = param.args[0]
                    val content= if (varargs != null) String.format(param.args[1] as String, *varargs) else param.args[1]
                    myLog("[${level.toUpperCase()}] [$tag] $content")
                }
            })
    }

    // hook java层的get logger level，仅在xlog未生效以前是有效的
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
                    myLog("set logger: ${param.args[0]}")
                }
            })
    }

    private fun hookRecv(lpparam: XC_LoadPackage.LoadPackageParam) {
        findAndHookMethod("com.tencent.mm.modelmulti.o\$d", lpparam.classLoader, "c", Queue::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    myLog("[modelmulti.o][recv] ${param.args[0]}")
                    Thread.dumpStack()
                }
            })

        // 消息体
        val classAddMsgInfo = this.findClassIfExists("com.tencent.mm.ah.e\$a", lpparam.classLoader)

        // 消息体内容
        //
        // 表情:
        // {
        //	"msg": {
        //		"emoji": {
        //			"_fromusername": "Windy268730",
        //			"_tousername": "wxid_r0um0vz3zt0g22",
        //			"_type": "2",
        //			"_idbuffer": "media:0_0",
        //			"_md5": "cc7a8eb7d6e93fe50217848a2277d5a7",
        //			"_len": "14100",
        //			"_productid": "com.tencent.xin.emoticon.person.stiker_14735911212a7cfbf7afcb83a5",
        //			"_androidmd5": "cc7a8eb7d6e93fe50217848a2277d5a7",
        //			"_androidlen": "14100",
        //			"_s60v3md5": "cc7a8eb7d6e93fe50217848a2277d5a7",
        //			"_s60v3len": "14100",
        //			"_s60v5md5": "cc7a8eb7d6e93fe50217848a2277d5a7",
        //			"_s60v5len": "14100",
        //			"_cdnurl": "http://mmbiz.qpic.cn/mmemoticon/6Vwf8UK179HGRtAUDsAbBsJkf0L1ybOw1icCcd8uOibyFWCZGSDWUbCw/0",
        //			"_designerid": "",
        //			"_thumburl": "http://mmbiz.qpic.cn/mmemoticon/Q3auHgzwzM7SMndia58EU6zSmkwsXF14eBbxfOam953ZJA4leXZhic2qZ4jMVrEiauC/0",
        //			"_encrypturl": "http://emoji.qpic.cn/wx_emoji/m3RdJnCg6kK2qzbmR7d1wr5CXd97bYGUHW8ZS5xpCvwkibv5ibF7w8EQ/",
        //			"_aeskey": "6239dac7e642565c49bccf7d8c065ec8",
        //			"_externurl": "http://emoji.qpic.cn/wx_emoji/syoqtvPiaibaLXooibDDaB5UQKJsup9xBc7LJkURCibRZp5nAAXXQPWXdWErUicy1OwCC/",
        //			"_externmd5": "ce41268ed72b2bbbd3827a55ea6ab804",
        //			"_width": "240",
        //			"_height": "240",
        //			"_tpurl": "",
        //			"_tpauthkey": "",
        //			"_attachedtext": "",
        //			"_attachedtextcolor": "",
        //			"_lensid": ""
        //		},
        //		"gameext": {
        //			"_type": "0",
        //			"_content": "0"
        //		}
        //	}
        //}
        val classsMessageBodyContent = this.findClassIfExists("com.tencent.mm.protocal.protobuf.cj", lpparam.classLoader)



        fun logFouondation(message: String) = myLog("[foundation.c] [recv] $message")

        // 消息接受的核心流程点
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


        // 表情的文件存放地址？
        findAndHookMethod("com.tencent.mm.storage.emotion.EmojiInfo", lpparam.classLoader, "djl",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    myLog("[emotion url] ${param.result}")
                }
            })
    }

    fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        this.hookGetLogLever(lpparam)
        this.hookLogSet(lpparam)

        arrayOf("logV", "logI", "logD", "logW", "logE", "logF").forEach {
            this.hookLogFunctionAboveInfoLevel(loggerImplementClass, it, lpparam)
            this.hookLogFunctionAboveInfoLevel(xloggerClass, it, lpparam)
        }
        arrayOf("d").forEach {
            this.hookLogFunctionBelowInfoLevel(it, lpparam)
        }

        this.hookRecv(lpparam)
    }
}

