package com.hepicar.listeneverything

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.hepicar.listeneverything.model.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_accelerometer.*
import kotlinx.android.synthetic.main.item_battery.*
import kotlinx.android.synthetic.main.item_geomagnetic.*
import kotlinx.android.synthetic.main.item_location.*
import kotlinx.android.synthetic.main.item_proximity.*
import kotlinx.android.synthetic.main.item_rotation.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
        updateButton()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }


    private fun updateButton() {
        var text = if (ForegroundService.isRunning) "STOP" else "START"
        start_button.text =text
    }

    fun onClickedStartButton(view: View){
        println("click start button ${ForegroundService.isRunning}")
        val startINtent = Intent(this,ForegroundService::class.java)
        if (!ForegroundService.isRunning){
            startINtent.action = Constants.ACTION.STARTFOREGROUND_ACTION
        }else{
            startINtent.action = Constants.ACTION.STOPFOREGROUND_ACTION
        }
        startService(startINtent)
        start_button.text = "wait..."
        Handler().postDelayed({
            updateButton()
        },1000)

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccelerometerChanged(callback : Accelerometer) {
        accelerometer_x_values.text = "x : ${callback.x}"
        accelerometer_y_values.text = "y : ${callback.y}"
        accelerometer_z_values.text = "z : ${callback.z}"
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProximityChanged(callback: Proximity){
        proximity_values.text = "${callback.values}"
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMagnetometerChanged(callback: Geomagnetic){
        geomagnetic_x_values.text = "x : ${callback?.x}"
        geomagnetic_y_values.text = "y : ${callback?.y}"
        geomagnetic_z_values.text = "z : ${callback?.y}"
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocatoinChanged(callback: Location){
        location_latitude_values.text = "latitude : ${callback.latitude}"
        location_longitude_values.text = "longitude : ${callback.longitude}"
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRotationChanged(callback: Orientation){
        rotation_azimuth_values.text = "azimuth : ${callback.x}"
        rotation_pich_values.text = "pitch : ${callback.y}"
        rotation_roll_values.text = "roll : ${callback.z}"
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBaterryChanged(callback:Battery){
        baterry_level_values.text = "level : ${callback.level}"
    }



}