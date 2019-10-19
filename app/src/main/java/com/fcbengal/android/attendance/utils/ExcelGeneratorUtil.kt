package com.fcbengal.android.attendance.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.fcbengal.android.attendance.BuildConfig
import com.fcbengal.android.attendance.R
import com.fcbengal.android.attendance.entity.AttendanceStatus
import com.fcbengal.android.attendance.entity.Player
import com.fcbengal.android.attendance.view.AttendanceView
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ExcelGeneratorUtil(
    playerAttendanceData: AttendanceView,
    playerIdMap: HashMap<String, Player>,
    groupName: String,
    groundName: String,
    monthId: Int,
    private val context: Context,
    listener: ExcelGeneratorListener
) {
    init {
        val TAG = "ExcelGeneratorUtil"
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.e("FileUtils", "Storage not available or read only")
        }


        val monthArray = arrayOf("January","February","March","April","May","June","July","August","September","October","November","December")
        val wb = XSSFWorkbook()
        var c: Cell?
        val sheet = wb.createSheet(monthArray[monthId])
        var rowCount = 0
        var rowCellCount = 0

        val style = wb.createCellStyle() as XSSFCellStyle
        style.borderRight = CellStyle.BORDER_THIN
        style.rightBorderColor = IndexedColors.BLACK.getIndex()
        style.borderBottom = CellStyle.BORDER_THIN
        style.bottomBorderColor = IndexedColors.BLACK.getIndex()
        style.borderLeft = CellStyle.BORDER_THIN
        style.leftBorderColor = IndexedColors.BLACK.getIndex()
        style.borderTop = CellStyle.BORDER_THIN
        style.topBorderColor = IndexedColors.BLACK.getIndex()
        style.alignment = CellStyle.ALIGN_CENTER
        val headerFont = wb.createFont()
        headerFont.boldweight = Font.BOLDWEIGHT_NORMAL
        headerFont.fontHeightInPoints = 12
        style.alignment = CellStyle.ALIGN_CENTER
        style.setFont(headerFont)

        val headerRow = sheet.createRow(rowCount++)

        c = headerRow.createCell(rowCellCount++)
        c.setCellValue("SL NO.")
        c.cellStyle = style

        c = headerRow.createCell(rowCellCount++)
        c.setCellValue("NAME")
        c.cellStyle = style

        c = headerRow.createCell(rowCellCount++)
        c.setCellValue("DOB")
        c.cellStyle = style

        c = headerRow.createCell(rowCellCount++)
        c.setCellValue("Contact No")
        c.cellStyle = style

        c = headerRow.createCell(rowCellCount++)
        c.setCellValue("Ground")
        c.cellStyle = style

        for (x in playerAttendanceData.sortedDateList) {
            c = headerRow.createCell(rowCellCount++)
            c.setCellValue(x.toString())
            c.cellStyle = style
        }

        c = headerRow.createCell(rowCellCount++)
        c.setCellValue("TOTAL PRESENT")
        c.cellStyle = style

        c = headerRow.createCell(rowCellCount++)
        c.setCellValue("%")
        c.cellStyle = style

        val totalRowCellCount = rowCellCount
        var totalPlayedDaysCount = 0
        var playerAttendedTotal = 0
        playerIdMap.forEach { (_, player) ->

            var row: XSSFRow? = null
            for (rowCell in 0 until 6) {
                when (rowCell) {
                    0 -> {
                        //Serial Number
                        rowCellCount = 0
                        row = sheet.createRow(rowCount++)
                        createCell(row, rowCellCount++, (rowCount - 1), style)
                    }
                    1 -> {
                        //Player Name
                        createCell(
                            row,
                            rowCellCount++,
                            StringBuilder().append(player.fName).append(" ").append(player.lName),
                            style
                        )
                    }
                    2 -> {
                        //DOB
                        createCell(row, rowCellCount++, player.dob.replace(
                            DatabaseUtil.constantKeySeparator,
                            DatabaseUtil.constantDateUISeparator
                        ), style)
                    }
                    3 -> {
                        //Contact Number
                        createCell(row, rowCellCount++, player.contactNo, style)
                    }
                    4 -> {
                        //Ground information
                        createCell(row, rowCellCount++, groundName, style)
                    }
                    5 -> {
                        Log.e(TAG, "Starting date data")
                        totalPlayedDaysCount = 0
                        playerAttendedTotal = 0
                        //Attendance Data
                        playerAttendanceData.sortedDateList.forEach{date ->
//                            Log.e("Date", date.toString())
                            val playerAttendanceDataList = playerAttendanceData.datePlayerAttendanceMap[date]
                            playerAttendanceDataList?.forEach { playerAttendance ->
                                if(playerAttendance.playerId == player.id){
                                    Log.e(TAG, "======================= Attendance Data =======================")
                                    Log.e(TAG, "Date $date, attn ${playerAttendance.playerId}")
                                    listener.onProgress("Writing data for ${player.fName} for $date")
                                    totalPlayedDaysCount++
                                    if(playerAttendance.status == AttendanceStatus.PRESENT || playerAttendance.status == AttendanceStatus.PRESENT_DELAYED){
                                        playerAttendedTotal++
                                    }
                                    createCellForAttendanceStatus(row, rowCellCount++, playerAttendance.status, playerAttendance.delayMinutes, style)
                                }
                            }
                        }
                    }
                    6 -> {
                        createCell(row, rowCellCount++, playerAttendedTotal, style)
                    }
                    7 -> {
                        createCell(row, rowCellCount++, (playerAttendedTotal/totalPlayedDaysCount*100), style)
                    }
                }
            }
        }

        val footerRow = sheet.createRow(rowCount+2)

        c = footerRow.createCell(2)
        c.setCellValue("TOTAL DAYS")
        c.cellStyle = style

        c = footerRow.createCell(3)
        c.setCellValue(totalPlayedDaysCount.toString())
        c.cellStyle = style

        //Auto sizing columns
        try{
            for(column in 0..totalRowCellCount){
//                sheet.autoSizeColumn(column)
                sheet.setColumnWidth(column, 20 * 500)
            }
        } catch (e : Exception){
            Log.e(TAG, "Error", e)
        }

        val fileName = StringBuilder().append(groupName.replace(" ", "")).append("_").append(monthArray[monthId]).append(".xlsx").toString()
        val file = File(context.getExternalFilesDir(null), fileName)
        val uri :Uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider",file)
        var os : FileOutputStream? = null
        try{
            os = FileOutputStream(file)
            wb.write(os)
            listener.onSuccess(uri, monthArray[monthId])
        } catch (e : IOException) {
            Log.e("$TAG File", "Error writing $file", e)
            listener.onError()
        } catch (e : Exception) {
            Log.e("$TAG File", "Failed to save file", e)
            listener.onError()
        } finally {
            try {
                os?.close()
            } catch (ex : Exception) {
                Log.e("$TAG File", "Failed close Stream", ex)
                listener.onError()
            }
        }

    }

    private fun isExternalStorageReadOnly(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED_READ_ONLY == extStorageState
    }

    private fun isExternalStorageAvailable(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == extStorageState
    }

    private fun createCell(row: XSSFRow?, rowCellCount: Int, cellValue: Any, style: XSSFCellStyle) {
        row?.let {
            val c = row.createCell(rowCellCount)
            c.setCellValue(cellValue.toString())
            c.cellStyle = style
            Log.e("Value", "Row ${row.firstCellNum} Row Cell $rowCellCount, Value $cellValue")
        }
    }

    private fun createCellForAttendanceStatus(row: XSSFRow?, rowCellCount: Int, status: AttendanceStatus, delay : Int, style: XSSFCellStyle) {
        row?.let {
            val c = row.createCell(rowCellCount)
            when(status){
                AttendanceStatus.PRESENT -> {
                    c.setCellValue("Y")
                }
                AttendanceStatus.PRESENT_DELAYED -> {
                    c.setCellValue("Y - ${context.getString(R.string.text_delay, delay.toString())}")
                }
                AttendanceStatus.ABSENT -> {
                    c.setCellValue("N")
                }
                AttendanceStatus.ABSENT_INFORMED -> {
                    c.setCellValue("I")
                }
            }
            c.cellStyle = style
            Log.e("Value", "Row ${row.firstCellNum} Row Cell $rowCellCount, Value ${status.text}")
        }
    }

    interface ExcelGeneratorListener {
        fun onSuccess(uri : Uri, month : String)
        fun onError()
        fun onProgress(status : String)
    }

}