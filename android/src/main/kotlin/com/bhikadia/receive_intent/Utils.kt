package com.bhikadia.receive_intent

import androidx.core.content.FileProvider;
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
// import android.os.Parcelable
// import android.util.Log
// import org.json.JSONArray
// import org.json.JSONException
// import org.json.JSONObject
import java.security.MessageDigest
import java.util.ArrayList
import java.io.File


fun mapToIntent(context: Context, map: Map<*, *>): Intent {

    val intent = Intent()

    (map["action"] as? String)?.let {
        intent.action = it
    }

    (map["data"] as? String)?.let { data ->
        intent.data =
            if (data.startsWith("/")) {
                FileProvider.getUriForFile(
                    context,
                    context.packageName + ".fileprovider",
                    File(data)
                )
            } else {
                Uri.parse(data)
            }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    (map["clipData"] as? List<*>)?.let { files ->

        var clip: ClipData? = null

        files.forEach { item ->

            val path = item as? String ?: return@forEach

            val uri =
                if (path.startsWith("/")) {
                    FileProvider.getUriForFile(
                        context,
                        context.packageName + ".fileprovider",
                        File(path)
                    )
                } else {
                    Uri.parse(path)
                }

            if (clip == null) {
                clip = ClipData.newUri(
                    context.contentResolver,
                    "",
                    uri
                )
            } else {
                clip!!.addItem(ClipData.Item(uri))
            }
        }

        clip?.let {
            intent.clipData = it

            if (intent.data == null)
                intent.data = it.getItemAt(0).uri

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    (map["flags"] as? Number)?.let {
        intent.flags = it.toInt()
    }

    (map["categories"] as? List<*>)?.forEach {
        if (it is String)
            intent.addCategory(it)
    }

    (map["extra"] as? Map<*, *>)?.forEach { (key, value) ->
        if (key is String)
            putExtra(intent, key, value)
    }

    (map["componentClassName"] as? String)?.let {
        intent.setClassName(
            intent.`package` ?: "",
            it
        )
    }
    
    return intent
}

fun intentToMap(
    context: Context,
    intent: Intent,
    fromPackageName: String?
): Map<String, Any?> {

    return mapOf(
        "componentClassName" to intent.component?.className,
        "fromPackageName" to fromPackageName,
        "fromSignatures" to fromPackageName?.let {
            getApplicationSignature(context, it)
        },
        "action" to intent.action,
        "data" to intent.dataString,
        "clipData" to intent.clipData?.let { clip ->
            List(clip.itemCount) { i ->
                clip.getItemAt(i).uri?.toString()
            }.filterNotNull()
        },
        "flags" to intent.flags,
        "categories" to intent.categories?.toList(),
        "extra" to intent.extras?.let {
            bundleToMap(it)
        }
    )
}

fun bundleToMap(bundle: Bundle): Map<String, Any?> {

    val map = HashMap<String, Any?>()

    for (key in bundle.keySet()) {
        map[key] = bundleValue(bundle[key])
    }

    return map
}

fun bundleValue(value: Any?): Any? {

    return when (value) {

        null -> null

        is String,
        is Boolean,
        is Byte,
        is Short,
        is Int,
        is Long,
        is Float,
        is Double -> value

        is Char -> value.toString()

        is Uri -> value.toString()

        is ArrayList<*> -> value

        is IntArray -> value.toList()

        is LongArray -> value.toList()

        is FloatArray -> value.toList()

        is DoubleArray -> value.toList()

        is BooleanArray -> value.toList()

        is ByteArray -> value.toList()

        is ShortArray -> value.toList()

        is CharArray -> value.map { it.toString() }

        is Bundle -> bundleToMap(value)

        else -> value.toString()
    }
}

fun mapToBundle(map: Map<*, *>): Bundle {
    val bundle = Bundle()

    map.forEach { (key, value) ->
        if (key is String) {
            putBundleValue(bundle, key, value)
        }
    }

    return bundle
}

fun putBundleValue(bundle: Bundle, key: String, value: Any?) {
    when (value) {
        null -> bundle.putString(key, null)
        is String -> bundle.putString(key, value)
        is Boolean -> bundle.putBoolean(key, value)
        is Byte -> bundle.putByte(key, value)
        is Short -> bundle.putShort(key, value)
        is Int -> bundle.putInt(key, value)
        is Long -> bundle.putLong(key, value)
        is Float -> bundle.putFloat(key, value)
        is Double -> bundle.putDouble(key, value)
        is Map<*, *> -> bundle.putBundle(key, mapToBundle(value))
        else -> bundle.putString(key, value.toString())
    }
}

fun putExtra(
    intent: Intent,
    key: String,
    value: Any?
) {

    when (value) {

        null ->
            intent.putExtra(key, null as String?)

        is String ->
            intent.putExtra(key, value)

        is Boolean ->
            intent.putExtra(key, value)

        is Byte ->
            intent.putExtra(key, value)

        is Short ->
            intent.putExtra(key, value)

        is Int ->
            intent.putExtra(key, value)

        is Long ->
            intent.putExtra(key, value)

        is Float ->
            intent.putExtra(key, value)

        is Double ->
            intent.putExtra(key, value)

        is List<*> ->
            intent.putStringArrayListExtra(
                key,
                ArrayList(value.map { it.toString() })
            )

        is Map<*, *> ->
            intent.putExtra(key, mapToBundle(value))

        else ->
            intent.putExtra(
                key,
                value.toString()
            )
    }
}

fun getApplicationSignature(context: Context, packageName: String): List<String> {
    val signatureList: List<String>
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // New signature
            val sig = context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
                ?: throw IllegalStateException("no signature found")
            signatureList = if (sig.hasMultipleSigners()) {
                // Send all with apkContentsSigners
                sig.apkContentsSigners.map {
                    val digest = MessageDigest.getInstance("SHA-256")
                    digest.update(it.toByteArray())
                    bytesToHex(digest.digest())
                }
            } else {
                // Send one with signingCertificateHistory
                sig.signingCertificateHistory.map {
                    val digest = MessageDigest.getInstance("SHA-256")
                    digest.update(it.toByteArray())
                    bytesToHex(digest.digest())
                }
            }
        } else {
            val sig = context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
                ?: throw IllegalStateException("no signature found")
            signatureList = sig.map {
                val digest = MessageDigest.getInstance("SHA-256")
                digest.update(it.toByteArray())
                bytesToHex(digest.digest())
            }
        }

        return signatureList
    } catch (e: Exception) {
        // Handle error
    }
    return emptyList()
}

fun bytesToHex(bytes: ByteArray): String {
    val hexArray = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    val hexChars = CharArray(bytes.size * 2)
    var v: Int
    for (j in bytes.indices) {
        v = bytes[j].toInt() and 0xFF
        hexChars[j * 2] = hexArray[v.ushr(4)]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}
