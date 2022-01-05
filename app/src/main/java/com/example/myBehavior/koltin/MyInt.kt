package com.example.myBehavior.koltin

import kotlin.math.abs


/**
 * int 取反
 */
fun Int.negate():Int{
    return if(this>0){
        -this
    }else{
        abs(this)
    }
}

fun main() {
    val a=-45
    val b=45

    println(a.negate())
    println(b.negate())
}