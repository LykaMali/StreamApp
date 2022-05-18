package com.mali.streamapp.utils

import android.util.Log

inline val <reified T> T.TAG: String
    get() = T::class.java.simpleName

inline fun <reified T> T.log_v(message: String) = Log.v(TAG, message)
inline fun <reified T> T.log_i(message: String) = Log.i(TAG, message)
inline fun <reified T> T.log_w(message: String) = Log.w(TAG, message)
inline fun <reified T> T.log_d(message: String) = Log.d(TAG, message)
inline fun <reified T> T.log_e(message: String) = Log.e(TAG, message)