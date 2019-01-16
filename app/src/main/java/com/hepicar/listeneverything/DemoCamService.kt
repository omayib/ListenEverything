/*
 * Copyright 2016 Keval Patel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hepicar.listeneverything

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import com.hepicar.listeneverything.hiddencamera.*

import java.io.File

/**
 * Created by Keval on 11-Nov-16.
 *
 * @author [&#39;https://github.com/kevalpatel2106&#39;]['https://github.com/kevalpatel2106']
 */

class DemoCamService : HiddenCameraService() {

    companion object {
        var isRunning:Boolean = false
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        println("democamservice onstartcommand")
        if(intent?.action.equals(Constants.ACTION.STARTFOREGROUND_ACTION)){
            DemoCamService.isRunning = true
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                println("democamservice onstartcommand permission is not granted")
                tearDownService()
            }
            if (HiddenCameraUtils.canOverDrawOtherApps(this)) {
                println("democamservice onstartcommand has acces to draw over apps")
                val cameraConfig = CameraConfig()
                    .getBuilder(this)
                    .setCameraFacing(CameraFacing.REAR_FACING_CAMERA)
                    .setCameraResolution(CameraResolution.MEDIUM_RESOLUTION)
                    .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                    .build()

                startCamera(cameraConfig)


            } else {

                println("democamservice onstartcommand open acces to draw over apps")
                //Open settings to grant permission for "Draw other apps".
                HiddenCameraUtils.openDrawOverPermissionSetting(this)
            }
        }else{
            println("democamservice onstartcommand closed")
            tearDownService()
        }


        return START_STICKY
    }

    private fun tearDownService() {
        DemoCamService.isRunning = false
        stopCamera()
        stopForeground(true)
        stopSelf()
    }

    override fun onImageCapture(imageFile: File) {
        Toast.makeText(
            this,
            "Captured image size is : " + imageFile.length(),
            Toast.LENGTH_SHORT
        )
            .show()

        // Do something with the image...

        stopSelf()
    }

    override fun onCameraError(@CameraError.CameraErrorCodes errorCode: Int) {
        when (errorCode) {
            CameraError.ERROR_CAMERA_OPEN_FAILED -> {
            }
            CameraError.ERROR_IMAGE_WRITE_FAILED -> {
            }
            CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE -> {
            }
            CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION ->
                //Display information dialog to the user with steps to grant "Draw over other app"
                //permission for the app.
                HiddenCameraUtils.openDrawOverPermissionSetting(this)
            CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA -> {
            }
        }//Camera open failed. Probably because another application
        //is using the camera
        //Image write failed. Please check if you have provided WRITE_EXTERNAL_STORAGE permission
        //camera permission is not available
        //Ask for the camera permission before initializing it.

        stopSelf()
    }
}
