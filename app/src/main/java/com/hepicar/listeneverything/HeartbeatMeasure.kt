package com.hepicar.listeneverything

import android.hardware.Camera
import android.util.Log
import java.sql.Timestamp
import java.util.concurrent.atomic.AtomicBoolean

class HeartbeatMeasure{
    private val TAG = this.javaClass.canonicalName
    private val processing = AtomicBoolean(false)

    private var beats = 0.0
    private var startTime: Long = 0
    private var beatsIndex = 0
    private val beatsArraySize = 3
    private val beatsArray = IntArray(beatsArraySize)
    private val currentType = TYPE.GREEN
    private var averageIndex = 0
    private val averageArraySize = 4
    private val averageArray = IntArray(averageArraySize)



    enum class TYPE {
        GREEN, RED
    }

    fun getCurrent(): TYPE {
        return currentType
    }
    companion object {
        var isRunning:Boolean = false
    }

    fun startTimer(){
        println("starting timer")
        startTime = System.currentTimeMillis()
    }

    fun previewFrameCalculation(data: ByteArray?, camera: Camera?){
        Log.d(TAG, "scan...")
        if (data == null) throw NullPointerException()
        val size = camera?.getParameters()!!.previewSize ?: throw NullPointerException()

        if (!processing.compareAndSet(false, true)) return

        val width = size.width
        val height = size.height

        val imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(), height, width)
        if (imgAvg == 0 || imgAvg == 255) {
            processing.set(false)
            return
        }

        var averageArrayAvg = 0
        var averageArrayCnt = 0
        for (i in averageArray.indices) {
            if (averageArray[i] > 0) {
                averageArrayAvg += averageArray[i]
                averageArrayCnt++
            }
        }

        val rollingAverage = if (averageArrayCnt > 0) averageArrayAvg / averageArrayCnt else 0

        var newType = currentType

        if (imgAvg < rollingAverage) {
            newType = TYPE.RED
            if (newType != currentType) {
                beats++
                Log.d(TAG, "detak!  timestamp :" + Timestamp(System.currentTimeMillis()))
            }
        } else if (imgAvg > rollingAverage) {
            newType = TYPE.GREEN
        }

        if (averageIndex == averageArraySize) averageIndex = 0
        averageArray[averageIndex] = imgAvg
        averageIndex++

        // Transitioned from one state to another to the same
        /*if (newType != currentType) {
            currentType = newType;
        }*/

        val endTime = System.currentTimeMillis()
        val totalTimeInSecs = (endTime - startTime) / 1000.0
        if (totalTimeInSecs >= 10) {
            val bps = beats / totalTimeInSecs
            val dpm = (bps * 60.0).toInt()
            if (dpm < 30 || dpm > 180) {
                startTime = System.currentTimeMillis()
                beats = 0.0
                processing.set(false)
                return
            }


            if (beatsIndex == beatsArraySize) beatsIndex = 0
            beatsArray[beatsIndex] = dpm
            beatsIndex++

            var beatsArrayAvg = 0
            var beatsArrayCnt = 0
            for (i in beatsArray.indices) {
                if (beatsArray[i] > 0) {
                    beatsArrayAvg += beatsArray[i]
                    beatsArrayCnt++
                }
            }
            val beatsAvg = beatsArrayAvg / beatsArrayCnt
//            Log.d(TAG, "estimasi denyut per menit=$beatsAvg")

            startTime = System.currentTimeMillis()
            beats = 0.0
        }
        processing.set(false)
    }

}