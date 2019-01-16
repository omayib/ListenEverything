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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.hepicar.listeneverything.HeartbeatMeasure

import java.io.IOException
import java.util.Collections

/**
 * Created by Keval on 10-Nov-16.
 * This surface view works as the fake preview for the camera.
 *
 * @author [&#39;https://github.com/kevalpatel2106&#39;]['https://github.com/kevalpatel2106']
 */

@SuppressLint("ViewConstructor")
internal class CameraPreview(context: Context, private val mCameraCallbacks: CameraCallbacks) : SurfaceView(context),
    SurfaceHolder.Callback {

    var measure :HeartbeatMeasure? = null
    private var mHolder: SurfaceHolder? = null
    private var mCamera: Camera? = null

    private var mCameraConfig: CameraConfig? = null

    @Volatile
    var isSafeToTakePictureInternal = false
        private set

    init {

        //Set surface holder
        initSurfaceView()
    }

    /**
     * Initialize the surface view holder.
     */
    private fun initSurfaceView() {
        println("init surface view")
        mHolder = holder
        mHolder!!.addCallback(this)
        mHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }


    override fun onLayout(b: Boolean, i: Int, i1: Int, i2: Int, i3: Int) {
        //Do nothing
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        println("surface created")
        measure = HeartbeatMeasure()
        measure!!.startTimer()
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
        println("surface changed")
        if (mCamera == null) {  //Camera is not initialized yet.
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
            return
        } else if (surfaceHolder.surface == null) { //Surface preview is not initialized yet
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
            return
        }

        // stop preview before making changes
        try {
            mCamera!!.stopPreview()
        } catch (e: Exception) {
            // Ignore: tried to stop a non-existent preview
        }

        // Make changes in preview size
        val parameters = mCamera!!.parameters
        val pictureSizes = mCamera!!.parameters.supportedPictureSizes

        //Sort descending
        Collections.sort<Camera.Size>(pictureSizes, PictureSizeComparator())

        //set the camera image size based on config provided
        val cameraSize: Camera.Size
        when (mCameraConfig!!.getResolution()) {
            CameraResolution.HIGH_RESOLUTION -> cameraSize = pictureSizes[0]   //Highest res
            CameraResolution.MEDIUM_RESOLUTION -> cameraSize =
                    pictureSizes[pictureSizes.size / 2]     //Resolution at the middle
            CameraResolution.LOW_RESOLUTION -> cameraSize = pictureSizes[pictureSizes.size - 1]       //Lowest res
            else -> throw RuntimeException("Invalid camera resolution.")
        }
        parameters.setPictureSize(cameraSize.width, cameraSize.height)

        // Set the focus mode.
        val supportedFocusModes = parameters.supportedFocusModes
        if (supportedFocusModes.contains(mCameraConfig!!.getFocusMode())) {
            parameters.focusMode = mCameraConfig!!.getFocusMode()
        }

        requestLayout()

        mCamera!!.parameters = parameters

        try {
            println("setPreviewCallback")
            mCamera!!.setPreviewCallback(previewCallback)
            mCamera!!.setDisplayOrientation(90)
            mCamera!!.setPreviewDisplay(surfaceHolder)
            mCamera!!.startPreview()

            isSafeToTakePictureInternal = true
        } catch (e: IOException) {
            //Cannot start preview
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
        } catch (e: NullPointerException) {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
        }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        println("surface destroyed")
        // Surface will be destroyed when we return, so stop the preview.
        // Call stopPreview() to stop updating the preview surface.
//        if (mCamera != null) mCamera!!.stopPreview()
        if (mCamera != null) {
            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
        }
    }

    /**
     * Initialize the camera and start the preview of the camera.
     *
     * @param cameraConfig camera config builder.
     */
    fun startCameraInternal(cameraConfig: CameraConfig) {
        mCameraConfig = cameraConfig

        if (safeCameraOpen(mCameraConfig!!.getFacing())) {
            if (mCamera != null) {
                requestLayout()

                try {
                    val p = mCamera!!.getParameters()
                    p.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                    mCamera!!.setParameters(p)
                    mCamera!!.setPreviewDisplay(mHolder)
                    mCamera!!.startPreview()
                    val fps = mCamera!!.parameters.supportedPreviewFpsRange
                    for (f in fps){
                        for (i in f){
                            println("fps {$i}")
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
                }

            }
        } else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
        }
    }

    private fun safeCameraOpen(id: Int): Boolean {
        var qOpened = false

        try {
//            stopPreviewAndFreeCamera()

            mCamera = Camera.open(id)
            qOpened = mCamera != null
        } catch (e: Exception) {
            Log.e("CameraPreview", "failed to open Camera")
            e.printStackTrace()
        }

        return qOpened
    }

    fun takePictureInternal() {
        isSafeToTakePictureInternal = false
        if (mCamera != null) {
            mCamera!!.takePicture(null, null, Camera.PictureCallback { bytes, camera ->
                Thread(Runnable {
                    //Convert byte array to bitmap
                    var bitmap: Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    //Rotate the bitmap
                    val rotatedBitmap: Bitmap?
                    if (mCameraConfig!!.getImageRotation() !== CameraRotation.ROTATION_0) {
                        rotatedBitmap = HiddenCameraUtils.rotateBitmap(bitmap!!, mCameraConfig!!.getImageRotation())


                        bitmap = null
                    } else {
                        rotatedBitmap = bitmap
                    }

                    //Save image to the file.
                    if (HiddenCameraUtils.saveImageFromFile(
                            rotatedBitmap!!,
                            mCameraConfig!!.getImageFile()!!,
                            mCameraConfig!!.getImageFormat()
                        )
                    ) {
                        //Post image file to the main thread
                        android.os.Handler(Looper.getMainLooper())
                            .post { mCameraCallbacks.onImageCapture(mCameraConfig!!.getImageFile()!!) }
                    } else {
                        //Post error to the main thread
                        android.os.Handler(Looper.getMainLooper())
                            .post { mCameraCallbacks.onCameraError(CameraError.ERROR_IMAGE_WRITE_FAILED) }
                    }

                    mCamera!!.startPreview()
                    isSafeToTakePictureInternal = true
                }).start()
            })
        } else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
            isSafeToTakePictureInternal = true
        }
    }

    /**
     * When this function returns, mCamera will be null.
     */
    fun stopPreviewAndFreeCamera() {
        isSafeToTakePictureInternal = false
        this.holder.removeCallback(this)
        if (mCamera != null) {
            mCamera!!.setPreviewCallback(null)
            mCamera!!.stopPreview()
            mCamera!!.release()
        }
    }


    private val previewCallback : Camera.PreviewCallback = object : Camera.PreviewCallback{
        override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
            measure!!.previewFrameCalculation(data,camera)
        }

    }
}

