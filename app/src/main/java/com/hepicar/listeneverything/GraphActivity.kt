package com.hepicar.listeneverything

import android.graphics.Color
import android.icu.util.TimeUnit
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.hepicar.listeneverything.model.HrmBlueSensor
import com.hepicar.listeneverything.model.HrmGreenSensor
import com.hepicar.listeneverything.model.HrmIrSensor
import com.hepicar.listeneverything.model.HrmRedSensor
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_graph.*
import kotlinx.android.synthetic.main.item_hrm.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.sql.Timestamp

class GraphActivity:AppCompatActivity(){
    var seriesDataIr:LineGraphSeries<DataPoint> = LineGraphSeries()
    var seriesDataRed:LineGraphSeries<DataPoint> = LineGraphSeries()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)

        graphView.addSeries(seriesDataIr)
        graphView.addSeries(seriesDataRed)
        graphView.viewport.setMinX(0.0)
        graphView.viewport.isXAxisBoundsManual = true
        graphView.viewport.isScalable = true
        graphView.viewport.isScrollable = true
        graphView.viewport.setScalableY(true)
        graphView.viewport.setScrollableY(true)

        seriesDataRed.color = Color.RED
        seriesDataIr.color = Color.GREEN
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHrmIrSensorChanged(callback: HrmIrSensor){
        var ts:Double= (System.currentTimeMillis()).toDouble()
        var yValue = callback.value!!.toDouble()
        var dt = DataPoint(ts, yValue)
        println("ir data line {${ts}}, {${yValue}}")
        seriesDataIr.appendData(dt,true,1000)
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHrmRedSensorChanged(callback: HrmRedSensor){
        var ts:Double= (System.currentTimeMillis()).toDouble()
        var yValue = callback.value!!.toDouble()
        var dt = DataPoint(ts, yValue)
        println("red data line {${ts}}, {${yValue}}")
        seriesDataRed.appendData(dt,true,1000)
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHrmGreenSensorChanged(callback: HrmGreenSensor){
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHrmBlueSensorChanged(callback: HrmBlueSensor){
    }

}