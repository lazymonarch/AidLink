package com.aidlink.data

import android.content.Context
import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.search.ResponseInfo
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class MapboxGeocodingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "MapboxGeoRepo"

    // ✅ Fixed: Use createSearchEngineWithBuiltInDataProviders without LocationProvider
    private val searchEngine: SearchEngine by lazy {
        SearchEngine.createSearchEngineWithBuiltInDataProviders(
            SearchEngineSettings()
        )
    }

    /**
     * Reverse geocode: coordinates → address or place name
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        return suspendCoroutine { continuation ->
            val options = ReverseGeoOptions(
                center = Point.fromLngLat(longitude, latitude),
                limit = 1
            )

            searchEngine.search(options, object : SearchCallback {
                override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                    if (results.isEmpty()) {
                        Log.d(tag, "Reverse geocode returned no results")
                        continuation.resume(null)
                        return
                    }

                    val result = results.first()
                    val formatted = result.address?.formattedAddress()
                        ?: result.name
                        ?: result.descriptionText
                        ?: "Unnamed area"

                    Log.d(tag, "Reverse geocode → $formatted")
                    continuation.resume(formatted)
                }

                override fun onError(e: Exception) {
                    Log.e(tag, "Reverse geocode failed", e)
                    continuation.resume(null)
                }
            })
        }
    }

    /**
     * Forward geocode: text → list of area suggestions
     */
    suspend fun forwardGeocode(query: String): List<Pair<String, String>> {
        return suspendCoroutine { continuation ->
            val options = SearchOptions(
                limit = 5
            )

            searchEngine.search(query, options, object : com.mapbox.search.SearchSuggestionsCallback {
                override fun onSuggestions(
                    suggestions: List<SearchSuggestion>,
                    responseInfo: ResponseInfo
                ) {
                    val results = suggestions.map {
                        it.name to (it.address?.formattedAddress() ?: "Unnamed area")
                    }
                    Log.d(tag, "Forward geocode found ${results.size} suggestions")
                    continuation.resume(results)
                }

                override fun onError(e: Exception) {
                    Log.e(tag, "Forward geocode failed", e)
                    continuation.resume(emptyList())
                }
            })
        }
    }
}