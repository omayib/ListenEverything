package com.hepicar.listeneverything.model

import java.util.*

data class Accelerometer(var x:Float?, var y:Float?, var z:Float?)
data class Geomagnetic(var x:Float?, var y:Float?, var z:Float?)
data class Orientation(var x:Float?, var y:Float?, var z:Float?)
data class Proximity(var values:Float?)
data class Location(var latitude:Double?, var longitude:Double?)
data class Battery(var level:Int?)
data class Microphone(var noice:Float?)
data class HrmRedSensor(var name:String?, var value:Float?)
data class HrmIrSensor(var name:String?, var value:Float?)
data class HrmGreenSensor(var name:String?, var value:Float?)
data class HrmBlueSensor(var name:String?, var value:Float?)