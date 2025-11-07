package com.aidlink.utils

import kotlin.math.*

object Geometries {
    data class Point(val lat: Double, val lon: Double) {
        fun distance(other: Point): Double {
            // Haversine distance in kilometers
            val r = 6371.0
            val dLat = Math.toRadians(other.lat - lat)
            val dLon = Math.toRadians(other.lon - lon)
            val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(dLon / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }

    data class BoundingBox(val topLeft: Point, val bottomRight: Point)

    fun point(lat: Double, lon: Double): Point = Point(lat, lon)

    fun getBoundingBox(centerLat: Double, centerLon: Double, radiusKm: Double): BoundingBox {
        // Approximate degrees to km conversion
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(centerLat)).coerceAtLeast(1e-6))

        val topLeft = Point(centerLat + latDelta, centerLon - lonDelta)
        val bottomRight = Point(centerLat - latDelta, centerLon + lonDelta)
        return BoundingBox(topLeft, bottomRight)
    }
}
