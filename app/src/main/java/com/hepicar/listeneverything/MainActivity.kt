package com.hepicar.listeneverything

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.hepicar.listeneverything.model.*
import com.zhaoxiaodan.miband.ActionCallback
import com.zhaoxiaodan.miband.MiBand
import com.zhaoxiaodan.miband.listeners.HeartRateNotifyListener
import com.zhaoxiaodan.miband.listeners.NotifyListener
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
import java.util.*


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    var miBand:MiBand? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupPermission()
//        openCamera()
    }

    private fun setupPermission() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this,Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BODY_SENSORS),
                91)
        }
    }

    private fun setupMiband(){
        println("setup miband")
        miBand = MiBand(this)
        miBand!!.setDisconnectedListener(disconnectedListener)
        miBand!!.setHeartRateScanListener(heartRateNotifyListener)
        MiBand.startScan(scanCalback)
    }
    lateinit var device: BluetoothDevice
    val scanCalback:ScanCallback = object:ScanCallback(){
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            device = result!!.device
            println("miband scan.. {${device.name}}")
            if(device.name ==null )return
            if (device.name.equals("MI Band 2")){
                miBand!!.connect(device,connectCallback)
                MiBand.stopScan(this)
            }
        }
    }

    val disconnectedListener:NotifyListener = object : NotifyListener{
        override fun onNotify(data: ByteArray?) {
            println("miband disconnected")
            MiBand.startScan(scanCalback)
        }

    }
    val pairingCallback: ActionCallback = object:ActionCallback{
        override fun onSuccess(data: Any?) {
            println("miband pairing success, heartRate scanning..")
            miBand!!.startHeartRateScan()
        }

        override fun onFail(errorCode: Int, msg: String?) {
            println("miband pairing fail {$msg}")
        }

    }
    val connectCallback: ActionCallback = object:ActionCallback{
        override fun onSuccess(data: Any?) {
            println("miband connect success")
            miBand!!.pair(pairingCallback)
        }

        override fun onFail(errorCode: Int, msg: String?) {
            println("miband connect fail {$msg}")
        }

    }

    val heartRateNotifyListener:HeartRateNotifyListener = object:HeartRateNotifyListener{
        override fun onNotify(heartRate: Int) {
            println("miband heartrate {$heartRate}")
        }
    }


    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 90)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 90){
            Log.d(TAG, "permission camera granted ")
        }else if (requestCode == 91){
//            setupMiband()
        }
    }


    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
        updateButton()
        //openCamera()
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
//        val cameraHiddenIntent = Intent(this,DemoCamService::class.java)
//        if(!DemoCamService.isRunning){
//            cameraHiddenIntent.action = Constants.ACTION.STARTFOREGROUND_ACTION
//        }else{
//            cameraHiddenIntent.action = Constants.ACTION.STOPFOREGROUND_ACTION
//        }
//        startService(cameraHiddenIntent)

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