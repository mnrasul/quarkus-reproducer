package com.sunrun.pricing

import io.kotest.core.spec.style.FreeSpec
import org.drools.decisiontable.SpreadsheetCompiler
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class PrintDrlTest : FreeSpec({

    fun getSheets(fileName: String, worksheetName: String) {
        val dtf = File(fileName)
        val inputStream: InputStream
        try {
            inputStream = FileInputStream(dtf)
            val ssComp = SpreadsheetCompiler()
            val s: String = ssComp.compile(inputStream, worksheetName)
            val tempFileName = fileName.substringAfterLast("/").substringBefore(".")
            File("src/test/resources/$tempFileName" + "_$worksheetName").writeText(s)

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    "print each worksheet" - {
        listOf(
            "Monthly",
        ).forEach {
            getSheets("src/main/resources/rules/sheet.drl.xlsx", it)
        }
    }
})
