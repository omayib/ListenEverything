package com.hepicar.listeneverything.hiddencamera

import java.io.File

interface CameraCallbacks {

    fun onImageCapture(imageFile: File)

    fun onCameraError(@CameraError.CameraErrorCodes errorCode: Int)
}