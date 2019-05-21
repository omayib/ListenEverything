package com.hepicar.listeneverything.model

class HeartRate(
    var timestampString: Long) {
    var hrm_ir: Float = 0F
    var hrm_red: Float = 0F
    override fun toString(): String {
        return "timestamp:${timestampString} , ir:${hrm_ir}, red: ${hrm_red}\n"
    }
}