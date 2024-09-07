package com.ani.map

data class GoogleMapDTO(
    val routes: List<Route>
)

data class Route(
    val legs: List<Leg>
)

data class Leg(
    val steps: List<Step>
)

data class Step(
    val polyline: Polyline
)

data class Polyline(
    val points: String
)
