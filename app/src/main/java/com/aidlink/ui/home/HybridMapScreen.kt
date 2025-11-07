// In: aidlink/ui/home/HybridMapScreen.kt (NEW FILE)
package com.aidlink.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.aidlink.viewmodel.HomeViewModel
import com.mapbox.maps.extension.compose.MapboxMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridMapScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    // TODO: Add BottomSheetScaffold here in Phase 2
    Scaffold { padding ->
        MapboxMap(modifier = Modifier.padding(padding))
    }
}