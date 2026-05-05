package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Light wrapper around Material3's [PullToRefreshBox] that handles the
 * "trigger → show spinner briefly → hide" lifecycle for screens whose data is
 * driven by hot Firestore Flows (where there's no real network call to await).
 *
 * Usage:
 * ```
 * AppPullToRefresh(onRefresh = { vm.loadForAdmin(adminId) }) {
 *     LazyColumn { ... }
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPullToRefresh(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            // Show spinner briefly so the gesture feels acknowledged even though
            // Firestore listeners stream updates instantly.
            delay(700L)
            isRefreshing = false
        }
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh    = {
            isRefreshing = true
            onRefresh()
        },
        modifier = modifier,
    ) {
        Box { content() }
    }
}

