package com.sos.womensafetyapp

import android.content.Context
import java.io.File
import java.io.FileOutputStream

fun copyAssetFolder(context: Context, assetFolderName: String, outPath: String) {
    val assetManager = context.assets
    val files = assetManager.list(assetFolderName) ?: return

    val outDir = File(outPath)
    if (!outDir.exists()) outDir.mkdirs()

    for (file in files) {
        val inPath = "$assetFolderName/$file"
        val outFile = File(outDir, file)

        if (assetManager.list(inPath)?.isNotEmpty() == true) {
            copyAssetFolder(context, inPath, outFile.absolutePath)
        } else {
            assetManager.open(inPath).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}