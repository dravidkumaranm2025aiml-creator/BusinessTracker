package com.dravid.businesstracker.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dravid.businesstracker.ui.screens.business.BusinessScreen
import com.dravid.businesstracker.ui.screens.calendar.CalendarScreen
import com.dravid.businesstracker.ui.screens.home.HomeScreen
import com.dravid.businesstracker.ui.screens.reports.ReportsScreen
import com.dravid.businesstracker.ui.screens.settings.SettingsScreen
import com.dravid.businesstracker.ui.screens.sources.SourcesScreen

/**
 * Top-level navigation host providing instant, YouTube-like 60 FPS tab switching.
 * Visited tabs are preserved in composition to eliminate recreation/re-layout overhead,
 * while inactive tabs pause Flow collection via scoped lifecycle states.
 */
@Composable
fun AppNavHost(
    currentRoute: String,
    modifier: Modifier = Modifier,
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val parentLifecycle = LocalLifecycleOwner.current.lifecycle

    // Track which screens have been visited so we instantiate lazily on demand
    var visitedRoutes by rememberSaveable { mutableStateOf(setOf(Screen.Home.route)) }

    LaunchedEffect(currentRoute) {
        if (currentRoute !in visitedRoutes) {
            visitedRoutes += currentRoute
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Screen.bottomNavItems.forEach { screen ->
            if (screen.route in visitedRoutes) {
                key(screen.route) {
                    TabScreenSlot(
                        screen = screen,
                        isSelected = currentRoute == screen.route,
                        parentLifecycle = parentLifecycle,
                        saveableStateHolder = saveableStateHolder,
                    )
                }
            }
        }
    }
}

/**
 * Isolated composable slot for a single tab screen.
 * Forms a skippable recomposition boundary so untouched inactive tabs are completely skipped.
 */
@Composable
private fun TabScreenSlot(
    screen: Screen,
    isSelected: Boolean,
    parentLifecycle: Lifecycle,
    saveableStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    modifier: Modifier = Modifier,
) {
    // Scoped LifecycleOwner for this tab (RESUMED when selected, CREATED when inactive)
    val tabLifecycleOwner = remember(screen.route) {
        TabLifecycleOwner(parentLifecycle)
    }

    DisposableEffect(isSelected) {
        tabLifecycleOwner.setActive(isSelected, parentLifecycle.currentState)
        onDispose { }
    }

    DisposableEffect(screen.route) {
        onDispose {
            tabLifecycleOwner.cleanup()
        }
    }

    // Scoped ViewModelStoreOwner so each screen retains its ViewModel independently
    val tabViewModelStoreOwner = remember(screen.route) {
        TabViewModelStoreOwner()
    }
    DisposableEffect(screen.route) {
        onDispose {
            tabViewModelStoreOwner.dispose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .tabVisibility(isSelected),
    ) {
        CompositionLocalProvider(
            LocalLifecycleOwner provides tabLifecycleOwner,
            LocalViewModelStoreOwner provides tabViewModelStoreOwner,
        ) {
            saveableStateHolder.SaveableStateProvider(screen.route) {
                when (screen) {
                    Screen.Home -> HomeScreen()
                    Screen.Business -> BusinessScreen()
                    Screen.Sources -> SourcesScreen()
                    Screen.Calendar -> CalendarScreen()
                    Screen.Reports -> ReportsScreen()
                    Screen.Settings -> SettingsScreen()
                }
            }
        }
    }
}

/**
 * Compatibility overload for existing invocations passing NavHostController.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    AppNavHost(
        currentRoute = currentRoute,
        modifier = modifier,
    )
}

/**
 * Skips drawing, layout measuring, and touch/accessibility hit-testing for inactive tabs
 * while keeping their composition alive.
 */
private fun Modifier.tabVisibility(visible: Boolean): Modifier = this
    .semantics {
        if (!visible) {
            hideFromAccessibility()
        }
    }
    .layout { measurable, constraints ->
        if (visible) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        } else {
            // Inactive tab: constrain to 0x0 so child hierarchies are not measured
            measurable.measure(Constraints(0, 0, 0, 0))
            layout(0, 0) {
                // Not placed
            }
        }
    }

/**
 * Manages an isolated lifecycle for a navigation tab.
 * When active, it reflects the parent activity's lifecycle state (STARTED/RESUMED).
 * When inactive, it drops to CREATED, automatically stopping Flow collections.
 */
private class TabLifecycleOwner(
    private val parentLifecycle: Lifecycle,
) : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    private var isTabActive: Boolean = false

    private val observer = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            if (isTabActive) registry.currentState = Lifecycle.State.STARTED
        }
        override fun onResume(owner: LifecycleOwner) {
            if (isTabActive) registry.currentState = Lifecycle.State.RESUMED
        }
        override fun onPause(owner: LifecycleOwner) {
            if (isTabActive) registry.currentState = Lifecycle.State.STARTED
        }
        override fun onStop(owner: LifecycleOwner) {
            registry.currentState = Lifecycle.State.CREATED
        }
        override fun onDestroy(owner: LifecycleOwner) {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    init {
        registry.currentState = Lifecycle.State.CREATED
        parentLifecycle.addObserver(observer)
    }

    fun cleanup() {
        parentLifecycle.removeObserver(observer)
        registry.currentState = Lifecycle.State.DESTROYED
    }

    fun setActive(active: Boolean, parentState: Lifecycle.State) {
        isTabActive = active
        if (parentState == Lifecycle.State.DESTROYED) {
            registry.currentState = Lifecycle.State.DESTROYED
            return
        }
        if (active) {
            when {
                parentState.isAtLeast(Lifecycle.State.RESUMED) -> registry.currentState = Lifecycle.State.RESUMED
                parentState.isAtLeast(Lifecycle.State.STARTED) -> registry.currentState = Lifecycle.State.STARTED
                else -> registry.currentState = Lifecycle.State.CREATED
            }
        } else {
            registry.currentState = Lifecycle.State.CREATED
        }
    }
}

/**
 * Provides an isolated ViewModelStore for each tab so screen ViewModels are properly scoped.
 */
private class TabViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()

    fun dispose() {
        viewModelStore.clear()
    }
}
