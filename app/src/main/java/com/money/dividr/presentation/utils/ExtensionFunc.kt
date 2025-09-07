package com.money.dividr.presentation.utils

import android.util.Log
import androidx.compose.ui.text.intl.Locale
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt
import kotlin.text.format

fun Double.roundToTwoDecimals(): Double {
    return (this * 100).roundToInt() / 100.0
}
fun Date?.formatFirebaseTimestamp(): String {
     
    return try {
        val sdf = SimpleDateFormat("MM/dd/yy", java.util.Locale.US)
        sdf.format(this ?: Date())
    } catch (e: Exception) {
        Log.e("FormatTimestamp", "Error formatting timestamp: $this", e)
        "Invalid Date"
    }
}