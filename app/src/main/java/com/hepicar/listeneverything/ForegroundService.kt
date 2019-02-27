package com.hepicar.listeneverything

import android.Manifest
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.*
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.os.PowerManager
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import com.hepicar.listeneverything.model.*
import com.samsung.android.sdk.sensorextension.*
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


class ForegroundService: Service(), SensorEventListener {



    private val TAG = "ForegroundService"

    var manager: SensorManager? = null
    var accelerometerSensor: Sensor? = null
    var proximitySensor: Sensor? = null
    var geomagneticSensor: Sensor? = null
    var gpsSensor:GPSService? = null
    var hrSensorManager:SsensorManager? = null
    var hrSensorExtention:SsensorExtension? = null
    var hrSensorListener:SsensorEventListener? =null

    var hrIr:Ssensor? = null
    var hrRed:Ssensor? = null
    var hrGreen:Ssensor? = null
    var hrBlue:Ssensor? = null


    private var accelerometerData = FloatArray(3)
    private var magnetometerData = FloatArray(3)
    var pm: PowerManager? = null
    var wl: PowerManager.WakeLock? = null

    var handler = Handler()

    private val periodicUpdate = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 10 * 1000)
        }
    }


    companion object {
        var isRunning:Boolean = false
    }


    override fun onCreate() {
        super.onCreate()
        pm = getSystemService(Context.POWER_SERVICE) as PowerManager?;
        wl = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"eee:aaa")
        wl?.acquire()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action.equals(Constants.ACTION.STARTFOREGROUND_ACTION)){
            ForegroundService.isRunning = true
            val icon = BitmapFactory.decodeResource(resources,R.drawable.ic_launcher_background)

            val notification = NotificationCompat.Builder(this,"212")
                .setContentTitle("Service sensor")
                .setContentText("Listening any sensor")
                .setOngoing(true).build()
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,notification)
            setupSensor()
        }else{
            ForegroundService.isRunning = false
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }




    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        manager?.unregisterListener(this,accelerometerSensor)
        manager?.unregisterListener(this,proximitySensor)
        manager?.unregisterListener(this,geomagneticSensor)
        if (hrIr != null)hrSensorManager?.unregisterListener(hrSensorListener,hrIr)
        if (hrRed != null)hrSensorManager?.unregisterListener(hrSensorListener,hrRed)
        if (hrGreen != null)hrSensorManager?.unregisterListener(hrSensorListener,hrGreen)
        if (hrBlue != null)hrSensorManager?.unregisterListener(hrSensorListener,hrBlue)
        try {
            this.unregisterReceiver(batteryBroadcastReceiver)
        }catch (e:java.lang.Exception){

        }

        wl?.release()
        handler.removeCallbacks(periodicUpdate)
    }

    private fun setupSensor() {
        manager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        try {
            accelerometerSensor= manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            proximitySensor = manager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            geomagneticSensor = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            hrSensorExtention = SsensorExtension()
            hrSensorExtention!!.initialize(this)
            hrSensorManager = SsensorManager(this,hrSensorExtention)
            hrIr = hrSensorManager!!.getDefaultSensor(Ssensor.TYPE_HRM_LED_IR)
            hrRed = hrSensorManager!!.getDefaultSensor(Ssensor.TYPE_HRM_LED_RED)
            hrGreen = hrSensorManager!!.getDefaultSensor(Ssensor.TYPE_HRM_LED_GREEN)
            hrBlue = hrSensorManager!!.getDefaultSensor(Ssensor.TYPE_HRM_LED_BLUE)
            hrSensorListener = HrSensorEventListener

            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            this.registerReceiver(batteryBroadcastReceiver,intentFilter)

            gpsSensor = GPSService(this)
            val permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            if(permission == PackageManager.PERMISSION_GRANTED){
                gpsSensor?.detectDeviceLocation()
            }else{
                println("permission not granted")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            println("error log sensor")
        }
        manager?.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        manager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        manager?.registerListener(this, geomagneticSensor, SensorManager.SENSOR_DELAY_NORMAL)
        if(hrIr != null)
            hrSensorManager!!.registerListener(hrSensorListener,hrIr,SensorManager.SENSOR_DELAY_NORMAL)
        if(hrRed != null)
            hrSensorManager!!.registerListener(hrSensorListener,hrRed,SensorManager.SENSOR_DELAY_NORMAL)
        if(hrGreen != null)
            hrSensorManager!!.registerListener(hrSensorListener,hrGreen,SensorManager.SENSOR_DELAY_NORMAL)
        if(hrBlue != null)
            hrSensorManager!!.registerListener(hrSensorListener,hrBlue,SensorManager.SENSOR_DELAY_NORMAL)

        handler.post { periodicUpdate.run() }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    val HrSensorEventListener = object : SsensorEventListener {
        override fun OnSensorChanged(event: SsensorEvent?) {
            print("onsensor hrm changed")
            val hrSensor:Ssensor = event!!.sensor

            when (event.sensor.type){
                Ssensor.TYPE_HRM_LED_IR -> {
                    println("sensor name {${hrSensor.name} \n " +
                            "ir raw data {${event.values[0]}}")
                    EventBus.getDefault().post(HrmIrSensor(hrSensor.name,event.values[0]))
                }
                Ssensor.TYPE_HRM_LED_RED -> {
                    println("sensor name {${hrSensor.name} \n " +
                            "red raw data {${event.values[0]}}")
                    EventBus.getDefault().post(HrmRedSensor(hrSensor.name,event.values[0]))
                }
                Ssensor.TYPE_HRM_LED_GREEN-> {
                    println("sensor name {${hrSensor.name} \n " +
                            "green raw data {${event.values[0]}}")
                    EventBus.getDefault().post(HrmGreenSensor(hrSensor.name,event.values[0]))
                }
                Ssensor.TYPE_HRM_LED_BLUE -> {
                    println("sensor name {${hrSensor.name} \n " +
                            "blue raw data {${event.values[0]}}")
                    EventBus.getDefault().post(HrmBlueSensor(hrSensor.name,event.values[0]))
                }
            }

        }

        override fun OnAccuracyChanged(event: Ssensor?, p1: Int) {
        }

    }
    val batteryBroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            var level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)
            EventBus.getDefault().post(Battery(level))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when(event?.sensor?.type){
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerData = event.values.clone()
                EventBus.getDefault().post(Accelerometer(event?.values?.get(0),event?.values?.get(1),event?.values?.get(2)))
            }
            Sensor.TYPE_PROXIMITY ->{
                EventBus.getDefault().post(Proximity(event?.values?.get(0)))
            }
            Sensor.TYPE_MAGNETIC_FIELD ->{
                magnetometerData = event.values.clone()
                EventBus.getDefault().post(Geomagnetic(event?.values?.get(0),event?.values?.get(1),event?.values?.get(2)))
            }
        }
        processRotation()
        processLocation()
    }

    private fun processLocation() {
        EventBus.getDefault().post(Location(gpsSensor?.latitude,gpsSensor?.longitude))
    }

    private fun processRotation() {

        var rotationMatrix = FloatArray(9)
        var orientationValues = FloatArray(9)
        val rotationOK = SensorManager.getRotationMatrix(
            rotationMatrix,
            null, accelerometerData, magnetometerData
        )
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrix, orientationValues);
            val azimuth = orientationValues[0]
            val pitch = orientationValues[1]
            val roll = orientationValues[2]
            EventBus.getDefault().post(Orientation(azimuth,pitch,roll))
        }else{
            EventBus.getDefault().post(Orientation(0.toFloat(),0.toFloat(),0.toFloat()))
        }
    }
}