package com.test.xtest.plugins

import android.app.Activity
import android.content.ContentValues
import android.widget.Toast
import com.gh0u1l5.wechatmagician.spellbook.base.Operation
import com.gh0u1l5.wechatmagician.spellbook.interfaces.IActivityHook
import com.gh0u1l5.wechatmagician.spellbook.interfaces.IDatabaseHook
import de.robv.android.xposed.XposedBridge.log

object Alter: IActivityHook {
    override fun onActivityStarting(activity: Activity) {
        Toast.makeText(activity, "wechat is starting", Toast.LENGTH_LONG).show()
    }
}


object Message : IDatabaseHook {
    override fun onDatabaseInserted(thisObject: Any, table: String, nullColumnHack: String?, initialValues: ContentValues?, conflictAlgorithm: Int, result: Long?): Operation<Long> {
        if (table == "message") {
            log("New Message: $initialValues")
            log("thisObject: $thisObject")
            log("table: $table")
            log("result: $result")
        }
        return super.onDatabaseInserted(thisObject, table, nullColumnHack, initialValues, conflictAlgorithm, result)
    }
}

