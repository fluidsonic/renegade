package ccr.math

import kotlin.math.PI

// C++: DEG_TO_RADF(x) macro — converts degrees to radians as float
fun degToRadF(degrees: Float): Float = (degrees * PI / 180.0).toFloat()

// C++: RAD_TO_DEGF(x) macro — converts radians to degrees as float
fun radToDegF(radians: Float): Float = (radians * 180.0 / PI).toFloat()
