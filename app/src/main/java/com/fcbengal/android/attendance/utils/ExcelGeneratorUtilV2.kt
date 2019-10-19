package com.fcbengal.android.attendance.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.fcbengal.android.attendance.BuildConfig
import com.fcbengal.android.attendance.R
import com.fcbengal.android.attendance.entity.AttendanceStatus
import com.fcbengal.android.attendance.entity.Ground
import com.fcbengal.android.attendance.entity.Player
import com.fcbengal.android.attendance.view.AttendanceView
import com.fcbengal.android.attendance.view.GroupAttendanceView
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
import java.text.DecimalFormat


class ExcelGeneratorUtilV2(
    groupAttendanceView : GroupAttendanceView,
    monthId: Int,
    private val context: Context,
    listener: ExcelGeneratorListener
) {
    init {
        val mTAG = "ExcelGeneratorUtil"
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.e(mTAG, "Storage not available or read only")
        }

        val monthArray = arrayOf("January","February","March","April","May","June","July","August","September","October","November","December")
        val wb = XSSFWorkbook()

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

        var playerAttendanceData : AttendanceView
        var playerList : ArrayList<Player>


        groupAttendanceView.groupIdList.forEach { groupId ->
            var c: Cell?
            val sheet = wb.createSheet(groupAttendanceView.groupMap[groupId]!!.name)
            var rowCount = 0
            var rowCellCount = 0

            if(null != groupAttendanceView.groupAttendanceMap[groupId]) {
                playerAttendanceData = groupAttendanceView.groupAttendanceMap[groupId]!!
                playerList = groupAttendanceView.groupPlayerMap[groupId]!!

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
                var totalPlayedDaysCount = 0f
                playerList.forEach { player ->

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
                                createCell(
                                    row, rowCellCount++, player.dob.replace(
                                        DatabaseUtil.constantKeySeparator,
                                        DatabaseUtil.constantDateUISeparator
                                    ), style
                                )
                            }
                            3 -> {
                                //Contact Number
                                createCell(row, rowCellCount++, player.contactNo, style)
                            }
                            4 -> {
                                //Ground information
                                /*val groundName = groupAttendanceView.groundMap[player.groupId]?.name
                                if(null != groundName) {
                                    createCell(row, rowCellCount++, groundName, style)
                                } else {
                                    createCell(row, rowCellCount++, "", style)
                                }*/
                                //Ground data is in player attendance data so handling in next section
                            }
                            5 -> {
                                Log.e(mTAG, "Starting date data")
                                totalPlayedDaysCount = 0f
                                var playerAttendedTotal :Float = 0f
                                rowCellCount++
                                val playerGroundIdList = ArrayList<String>()
                                //Attendance Data
                                playerAttendanceData.sortedDateList.forEach { date ->
                                    val playerAttendanceDataList = playerAttendanceData.datePlayerAttendanceMap[date]
                                    playerAttendanceDataList?.forEach { playerAttendance ->
                                        if (playerAttendance.playerId == player.id) {
                                            Log.e(mTAG,"Date $date, attn ${playerAttendance.playerId}")
                                            listener.onProgress("Writing data for ${player.fName} for $date")
                                            totalPlayedDaysCount++
                                            if (playerAttendance.status == AttendanceStatus.PRESENT || playerAttendance.status == AttendanceStatus.PRESENT_DELAYED) {
                                                playerAttendedTotal++
                                            }
                                            if(!playerGroundIdList.contains(playerAttendance.groundId)){
                                                playerGroundIdList.add(playerAttendance.groundId)
                                            }
                                            createCellForAttendanceStatus(
                                                row,
                                                rowCellCount++,
                                                playerAttendance.status,
                                                playerAttendance.delayMinutes,
                                                style
                                            )
                                        }
                                    }
                                }
                                createCellForGround(row, playerGroundIdList, groupAttendanceView.groundMap, style)
                                createCell(row, rowCellCount++, playerAttendedTotal.toInt(), style)
                                val roundEvenDecimal = DecimalFormat("0.00")
                                createCell(row, rowCellCount++, roundEvenDecimal.format(playerAttendedTotal / totalPlayedDaysCount * 100f), style)
                            }
                        }
                    }
                }

                //Insert empty row
                rowCount++

                val footerMonthRow = sheet.createRow(rowCount++)
                c = footerMonthRow.createCell(2)
                c.setCellValue(monthArray[monthId])
                c.cellStyle = style

                val footerRow = sheet.createRow(rowCount++)
                c = footerRow.createCell(1)
                c.setCellValue("TOTAL DAYS")
                c.cellStyle = style

                c = footerRow.createCell(2)
                c.setCellValue(totalPlayedDaysCount.toString())
                c.cellStyle = style

                //Auto sizing columns
                try {
                    for (column in 0..totalRowCellCount) {
                        sheet.setColumnWidth(column, 20 * 500)
                    }
                } catch (e: Exception) {
                    Log.e(mTAG, "Auto size error", e)
                }
            }
        }


        val fileName = StringBuilder().append("Attendance Report").append("_").append(monthArray[monthId]).append(".xlsx").toString()
        val file = File(context.getExternalFilesDir(null), fileName)
        val uri :Uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider",file)
        var os : FileOutputStream? = null
        try{
            os = FileOutputStream(file)
            wb.write(os)
            listener.onSuccess(uri, monthArray[monthId])
        } catch (e : IOException) {
            Log.e("$mTAG File", "Error writing $file", e)
            listener.onError()
        } catch (e : Exception) {
            Log.e("$mTAG File", "Failed to save file", e)
            listener.onError()
        } finally {
            try {
                os?.close()
            } catch (ex : Exception) {
                Log.e("$mTAG File", "Failed close Stream", ex)
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
                else -> c.setCellValue("NA")
            }
            c.cellStyle = style
            Log.e("Value", "Row ${row.firstCellNum} Row Cell $rowCellCount, Value ${status.text}")
        }
    }

    private fun createCellForGround(row: XSSFRow?,playerGroundIdList : ArrayList<String>, groundMap : HashMap<String, Ground>, style: XSSFCellStyle){
        row?.let {
            val c = row.createCell(4)
            val groundList = StringBuilder()
            playerGroundIdList.forEach {
                if(null != groundMap[it]) {
                    groundList.append(groundMap[it]!!.name)
                    groundList.append(", ")
                }
            }
            if(groundList.toString().isNotEmpty()){
                c.setCellValue(groundList.toString().substring(0, groundList.toString().length - 2))
            }
            c.cellStyle = style
        }
    }

    interface ExcelGeneratorListener {
        fun onSuccess(uri : Uri, month : String)
        fun onError()
        fun onProgress(status : String)
    }

}