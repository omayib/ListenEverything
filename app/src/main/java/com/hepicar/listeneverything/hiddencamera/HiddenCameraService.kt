/*
 * Copyright 2017 Keval Patel.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hepicar.listeneverything.hiddencamera

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.support.annotation.RequiresPermission
import android.support.v4.app.ActivityCompat
import android.view.ViewGroup
import android.view.WindowManager

/**
 * Created by Keval on 27-Oct-16.
 * This abstract class provides ability to handle background camera to the service in which it is
 * extended.
 *
 * @author [&#39;https://github.com/kevalpatel2106&#39;]['https://github.com/kevalpatel2106']
 */

abstract class HiddenCameraService : Service(), CameraCallbacks {

    private var mWindowManager: WindowManager? = null
    private var mCameraPreview: CameraPreview? = null

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * Start the hidden camera. Make sure that you check for the runtime permissions before you start
     * the camera.
     *
     *
     * <B>Note: </B>Developer has to check if the "Draw over other apps" permission is available by
     * calling [HiddenCameraUtils.canOverDrawOtherApps] before staring the camera.
     *
     * @param cameraConfig camera configuration [CameraConfig]
     */
    @RequiresPermission(allOf = arrayOf(Manifest.permission.CAMERA, Manifest.permission.SYSTEM_ALERT_WINDOW))
    protected fun startCamera(cameraConfig: CameraConfig) {
        println("start camera")

        if (!HiddenCameraUtils.canOverDrawOtherApps(this)) {    //Check if the draw over other app permission is available.

            onCameraError(CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION)
        } else if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) { //check if the camera permission is available

            //Throw error if the camera permission not available
            onCameraError(CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE)
        } else if (cameraConfig.getFacing() === CameraFacing.FRONT_FACING_CAMERA && !HiddenCameraUtils.isFrontCameraAvailable(
                this
            )
        ) {   //Check if for the front camera

            onCameraError(CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA)
        } else {

            //Add the camera preview surface to the root of the activity view.
            println("attach the camerapreview")
            if (mCameraPreview == null) mCameraPreview = addPreView()
            mCameraPreview!!.startCameraInternal(cameraConfig)
        }
    }

    /**
     * Call this method to capture the image using the camera you initialized. Don't forget to
     * initialize the camera using [.startCamera] before using this function.
     */
    protected fun takePicture() {
        if (mCameraPreview != null) {
            if (mCameraPreview!!.isSafeToTakePictureInternal) {
                mCameraPreview!!.takePictureInternal()
            }
        } else {
            throw RuntimeException("Background camera not initialized. Call startCamera() to initialize the camera.")
        }
    }

    /**
     * Stop and release the camera forcefully.
     */
    protected fun stopCamera() {
        println("stopCamera")
        if (mCameraPreview != null) {
            mWindowManager!!.removeView(mCameraPreview)
            mCameraPreview!!.stopPreviewAndFreeCamera()
        }
    }

    /**
     * Add camera preview to the root of the activity layout.
     *
     * @return [CameraPreview] that was added to the view.
     */
    private fun addPreView(): CameraPreview {
        //create fake camera view
        val cameraSourceCameraPreview = CameraPreview(this, this)
        cameraSourceCameraPreview.layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            1, 1,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        mWindowManager!!.addView(cameraSourceCameraPreview, params)
        return cameraSourceCameraPreview
    }
}
