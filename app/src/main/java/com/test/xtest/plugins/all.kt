package com.test.xtest.plugins

import android.util.Log
import com.gh0u1l5.wechatmagician.spellbook.C
import com.gh0u1l5.wechatmagician.spellbook.WechatGlobal
import com.gh0u1l5.wechatmagician.spellbook.util.ReflectionUtil
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.*
import kotlin.concurrent.thread

object GlobalContext {
    lateinit var classLoader: ClassLoader
    var sendTextCompenent: Any? = null
    var chattingSmileyPanel: Any? = null
}

object TextMessage {
    var target: String = ""
    var content: String = ""
}

object EmojiMessage {
    var target: String = ""
    var emojiInfo: Any? = null
}

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

    private fun sendTextMessage() {
        try {
            val groupId = "4582929225@chatroom"
            TextMessage.target = "Windy268730"
            TextMessage.content = "自动发送的消息是否会成功呢？"
            callMethod(GlobalContext.sendTextCompenent, "er", TextMessage.content, 0)
        } catch (e: Exception) {
            myLog("call send text message error")
            e.printStackTrace()
        }
    }

    private fun sendEmoji() {
        try {
            val groupId = "4582929225@chatroom"
            EmojiMessage.target = groupId
            callMethod(GlobalContext.chattingSmileyPanel, "p", EmojiMessage.emojiInfo)
        } catch (e: Exception) {
            myLog("call send emoji message error")
            e.printStackTrace()
        }
    }

    private fun hookSend(lpparam: XC_LoadPackage.LoadPackageParam) {
        findAndHookMethod("com.tencent.mm.modelmulti.h", lpparam.classLoader, "a",
            this.findClassIfExists("com.tencent.mm.network.e", lpparam.classLoader),
            this.findClassIfExists("com.tencent.mm.ah.f", lpparam.classLoader),
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    myLog("sending message: ${param.args[0]?.javaClass}, ${param.args[1]?.javaClass}")
                    Thread.dumpStack()
                }
            })

        // 这个函数是文本消息类入队对象的构造方法，可以在这里修改参数，修改参数1则可以修改发送的对象
        findAndHookConstructor("com.tencent.mm.modelmulti.h", lpparam.classLoader,
            C.String, C.String, C.Int, C.Int, Object::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if(TextMessage.target.isNotBlank()) {
                        param.args[0] = TextMessage.target
                        param.args[1] = TextMessage.content
                    }

                    myLog("construct message : ${param.args[0]} ${param.args[1]}, ${param.args[2]} ${param.args[3]} ${param.args[4]}")
                }
            })

        // 这个类是表情类入队对象的构造方法，可以在这里修改参数
        findAndHookConstructor("com.tencent.mm.plugin.emoji.f.r", lpparam.classLoader,
            C.String, C.String, this.findClassIfExists("com.tencent.mm.storage.emotion.EmojiInfo", lpparam.classLoader), C.Long, Byte::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if(EmojiMessage.target.isNotBlank()) {
                        param.args[0] = EmojiMessage.target
                    }
                }
            })

        findAndHookMethod("com.tencent.mm.ah.p", lpparam.classLoader, "a",
            this.findClassIfExists("com.tencent.mm.ah.m", lpparam.classLoader),
            C.Int,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    myLog("add to queue: ${param.args[0]} ${param.args[1]}")
                    Thread.dumpStack()
                }
            })

        // TODO 文本消息的入口可以在这里发送了
        // 发送消息按钮按下后的处理函数，在这里可以修改你要发送的内容
        // 但是不能直接调用这个函数发送给目标用户，需要设置好发送对象才可以（hook另外一个方法修改）
        findAndHookMethod("com.tencent.mm.ui.chatting.c.ai", lpparam.classLoader, "er",
            C.String, C.Int,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // save SendTextComponent object for later use
                    GlobalContext.sendTextCompenent = param.thisObject
                    myLog("[SendTextComponent] send message: ${param.args[0]}, ${param.args[1]}")
                    Thread.dumpStack()
                }

                override fun afterHookedMethod(param: MethodHookParam?) {
                    // 发送完消息后重置修改的内容为空
                    if(TextMessage.target.isNotBlank()) {
                        TextMessage.target = ""
                        TextMessage.content = ""
                    }
                }
            })


        // TODO 发送表情
        findAndHookMethod("com.tencent.mm.ui.chatting.v", lpparam.classLoader, "p",
            this.findClassIfExists("com.tencent.mm.storage.emotion.EmojiInfo", lpparam.classLoader),
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    GlobalContext.chattingSmileyPanel = param.thisObject
                    EmojiMessage.emojiInfo = param.args[0] // copy send
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    if(EmojiMessage.target.isNotBlank() && EmojiMessage.emojiInfo != null) {
                        EmojiMessage.target = ""
                        EmojiMessage.emojiInfo = null
                    }
                }
            })

        findAndHookMethod("com.tencent.mm.ui.chatting.c.ai", lpparam.classLoader, "a",
            this.findClassIfExists("com.tencent.mm.ui.chatting.d.a", lpparam.classLoader),
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    myLog("xHQ ${param.args[0]}")
                    Thread.dumpStack()
                }
            })

        // network
        findAndHookMethod("com.tencent.mm.network.z", lpparam.classLoader, "a",
            this.findClassIfExists("com.tencent.mm.network.r", lpparam.classLoader),
            this.findClassIfExists("com.tencent.mm.network.l", lpparam.classLoader),
            this.findClassIfExists("com.tencent.mm.network.c", lpparam.classLoader),
            C.Int,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    myLog("network: ${param.args[0]} ${param.args[1]} ${param.args[2]} ${param.args[3]}")
                    Thread.dumpStack()
                }
            })

        // network
        findAndHookMethod("com.tencent.mm.ah.r", lpparam.classLoader, "a",
            this.findClassIfExists("com.tencent.mm.network.r", lpparam.classLoader),
            this.findClassIfExists("com.tencent.mm.network.l", lpparam.classLoader),
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    myLog("MicroMsg.RDispatcher")
                    Thread.dumpStack()
                }
            })

        // network
        findAndHookMethod("com.tencent.mm.network.t", lpparam.classLoader, "a",
            this.findClassIfExists("com.tencent.mm.network.r", lpparam.classLoader),
            this.findClassIfExists("com.tencent.mm.network.l", lpparam.classLoader),
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    myLog("MMAutoAuth.send")
                    Thread.dumpStack()
                }
            })

        // protocol
        findAndHookMethod("com.tencent.mm.ah.t", lpparam.classLoader, "YJ",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    myLog("protocol-YJ ${param.result}")
                    Thread.dumpStack()
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
        GlobalContext.classLoader = lpparam.classLoader
        this.hookGetLogLever(lpparam)
        this.hookLogSet(lpparam)

        arrayOf("logV", "logI", "logD", "logW", "logE", "logF").forEach {
            this.hookLogFunctionAboveInfoLevel(loggerImplementClass, it, lpparam)
            this.hookLogFunctionAboveInfoLevel(xloggerClass, it, lpparam)
        }
        arrayOf("d").forEach {
            this.hookLogFunctionBelowInfoLevel(it, lpparam)
        }

        this.hookSend(lpparam)
        this.hookRecv(lpparam)

//        thread(start = true) {
//            while(true) {
//                Thread.sleep(10 * 1000)
//                if(GlobalContext.sendTextCompenent != null) {
//                    myLog("start to to send the text")
//                    sendTextMessage()
//                    break
//                }
//            }
//        }

        thread(start = true) {
            while(true) {
                Thread.sleep(10 * 1000)
                if(GlobalContext.chattingSmileyPanel != null && EmojiMessage.emojiInfo != null) {
                    myLog("start to send emoji message")
                    sendEmoji()
                    break
                }
            }
        }
    }
}

