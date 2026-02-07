package com.westcounty.micemice.ui.util

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun writeCsvToAppFiles(context: Context, prefix: String, content: String): Result<String> {
    return runCatching {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val name = "${prefix}_${formatter.format(Date())}.csv"
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, name)
        file.writeText(content)
        file.absolutePath
    }
}

fun writeTextPdfToAppFiles(
    context: Context,
    prefix: String,
    title: String,
    content: String,
): Result<String> {
    return runCatching {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestamp = formatter.format(Date())
        val fileName = "${prefix}_${timestamp}.pdf"
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, fileName)

        val pageWidth = 595
        val pageHeight = 842
        val margin = 40
        val lineHeight = 20
        val maxCharsPerLine = 80

        val titlePaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            textSize = 11f
        }

        val wrappedLines = buildList {
            add(title)
            add("Generated: $timestamp")
            add("")
            content.lineSequence().forEach { line ->
                if (line.length <= maxCharsPerLine) {
                    add(line)
                } else {
                    var start = 0
                    while (start < line.length) {
                        val end = (start + maxCharsPerLine).coerceAtMost(line.length)
                        add(line.substring(start, end))
                        start = end
                    }
                }
            }
        }

        val pdf = PdfDocument()
        var pageNumber = 1
        var lineIndex = 0
        while (lineIndex < wrappedLines.size) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            var y = margin.toFloat()

            while (lineIndex < wrappedLines.size && y <= (pageHeight - margin).toFloat()) {
                val line = wrappedLines[lineIndex]
                val paint = if (lineIndex == 0) titlePaint else bodyPaint
                canvas.drawText(line, margin.toFloat(), y, paint)
                y += lineHeight.toFloat()
                lineIndex += 1
            }

            pdf.finishPage(page)
            pageNumber += 1
        }

        file.outputStream().use { output -> pdf.writeTo(output) }
        pdf.close()
        file.absolutePath
    }
}

fun writeCompliancePackageZip(
    context: Context,
    prefix: String,
    csvContent: String,
    pdfTitle: String,
    pdfContent: String,
): Result<String> {
    return runCatching {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestamp = formatter.format(Date())
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val csvFile = File(dir, "${prefix}_${timestamp}.csv")
        csvFile.writeText(csvContent)

        val pdfPath = writeTextPdfToAppFiles(
            context = context,
            prefix = "${prefix}_${timestamp}",
            title = pdfTitle,
            content = pdfContent,
        ).getOrElse { throwable -> throw throwable }
        val pdfFile = File(pdfPath)
        val zipFile = File(dir, "${prefix}_${timestamp}.zip")

        ZipOutputStream(zipFile.outputStream().buffered()).use { output ->
            listOf(
                csvFile to "compliance.csv",
                pdfFile to "compliance.pdf",
            ).forEach { (source, entryName) ->
                output.putNextEntry(ZipEntry(entryName))
                FileInputStream(source).use { input ->
                    input.copyTo(output)
                }
                output.closeEntry()
            }
        }

        runCatching { csvFile.delete() }
        runCatching { pdfFile.delete() }
        zipFile.absolutePath
    }
}
