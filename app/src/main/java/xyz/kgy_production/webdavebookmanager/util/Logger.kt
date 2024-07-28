package xyz.kgy_production.webdavebookmanager.util

import android.util.Log
import xyz.kgy_production.webdavebookmanager.BuildConfig
import kotlin.properties.ReadOnlyProperty

sealed class Logger(protected val tag: String) {
    companion object {
        fun of(tag: String) = if (BuildConfig.DEBUG) NormalLogger(tag) else ProdLogger(tag)
        fun delegate(clz: Class<*>) = ReadOnlyProperty<Any?, Logger> { _, _ ->
            of(clz.simpleName)
        }

        fun delegate(tag: String) = ReadOnlyProperty<Any?, Logger> { _, _ ->
            of(tag)
        }
    }

    abstract fun d(msg: String): Int
    abstract fun d(msg: String, th: Throwable): Int
    abstract fun e(msg: String): Int
    abstract fun e(msg: String, th: Throwable): Int
    abstract fun v(msg: String): Int
    abstract fun v(msg: String, th: Throwable): Int
    abstract fun i(msg: String): Int
    abstract fun i(msg: String, th: Throwable): Int
    abstract fun wtf(msg: String): Int
    abstract fun wtf(th: Throwable): Int
    abstract fun wtf(msg: String, th: Throwable): Int
    abstract fun w(msg: String): Int
    abstract fun w(th: Throwable): Int
    abstract fun w(msg: String, th: Throwable): Int

    class NormalLogger(tag: String) : Logger(tag) {
        override fun d(msg: String) = Log.d(tag, msg)
        override fun d(msg: String, th: Throwable) = Log.d(tag, msg, th)
        override fun e(msg: String) = Log.e(tag, msg)
        override fun e(msg: String, th: Throwable) = Log.e(tag, msg, th)
        override fun v(msg: String) = Log.v(tag, msg)
        override fun v(msg: String, th: Throwable) = Log.v(tag, msg, th)
        override fun i(msg: String) = Log.i(tag, msg)
        override fun i(msg: String, th: Throwable) = Log.i(tag, msg, th)
        override fun wtf(msg: String) = Log.wtf(tag, msg)
        override fun wtf(th: Throwable) = Log.wtf(tag, th)
        override fun wtf(msg: String, th: Throwable) = Log.wtf(tag, msg, th)
        override fun w(msg: String) = Log.w(tag, msg)
        override fun w(th: Throwable) = Log.w(tag, th)
        override fun w(msg: String, th: Throwable) = Log.w(tag, msg, th)
    }

    class ProdLogger(tag: String) : Logger(tag) {
        override fun d(msg: String) = 0
        override fun d(msg: String, th: Throwable) = 0
        override fun e(msg: String) = Log.e(tag, msg)
        override fun e(msg: String, th: Throwable) = Log.e(tag, msg, th)
        override fun v(msg: String) = 0
        override fun v(msg: String, th: Throwable) = 0
        override fun i(msg: String) = 0
        override fun i(msg: String, th: Throwable) = 0
        override fun wtf(msg: String) = Log.wtf(tag, msg)
        override fun wtf(th: Throwable) = Log.wtf(tag, th)
        override fun wtf(msg: String, th: Throwable) = Log.wtf(tag, msg, th)
        override fun w(msg: String) = Log.w(tag, msg)
        override fun w(th: Throwable) = Log.w(tag, th)
        override fun w(msg: String, th: Throwable) = Log.w(tag, msg, th)
    }
}