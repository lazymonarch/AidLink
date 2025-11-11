package com.aidlink.data

import android.content.Context
import com.aidlink.BuildConfig
import com.mapbox.search.ApiType
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchSuggestionsCallback
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.mapbox.geojson.Point
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapboxGeocodingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Correctly initialize the SearchEngine using the non-deprecated method,
    // specifying the ApiType. The SDK automatically uses the token from the manifest.
    private val searchEngine: SearchEngine = SearchEngine.createSearchEngine(
        apiType = ApiType.GEOCODING,
        settings = SearchEngineSettings()
    )

    fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        callback: (address: String?) -> Unit
    ) {
        val options = ReverseGeoOptions.Builder(Point.fromLngLat(longitude, latitude))
            .limit(1)
            .build()

        searchEngine.search(
            options,
            object : SearchCallback {
                override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                    callback(results.firstOrNull()?.name)
                }

                override fun onError(e: Exception) {
                    callback(null)
                }
            }
        )
    }

    fun forwardGeocode(
        query: String,
        callback: (results: List<Pair<String, String>>) -> Unit
    ) {
        val options = SearchOptions.Builder()
            .limit(5)
            .build()

        searchEngine.search(
            query,
            options,
            object : SearchSuggestionsCallback {
                override fun onSuggestions(
                    suggestions: List<SearchSuggestion>,
                    responseInfo: ResponseInfo
                ) {
                    val results = suggestions.mapNotNull { suggestion ->
                        val name = suggestion.name
                        val address = suggestion.address?.formattedAddress()
                        if (name != null && address != null) {
                            name to address
                        } else {
                            null
                        }
                    }
                    callback(results)
                }
                override fun onError(e: Exception) {
                    callback(emptyList())
                }
            }
        )
    }
}