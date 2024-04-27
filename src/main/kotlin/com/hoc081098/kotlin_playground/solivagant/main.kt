package com.hoc081098.kotlin_playground.solivagant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.hoc081098.solivagant.lifecycle.LocalLifecycleOwner
import com.hoc081098.solivagant.navigation.ClearOnDispose
import com.hoc081098.solivagant.navigation.NavEventNavigator
import com.hoc081098.solivagant.navigation.NavHost
import com.hoc081098.solivagant.navigation.NavRoot
import com.hoc081098.solivagant.navigation.NavRoute
import com.hoc081098.solivagant.navigation.ProvideCompositionLocals
import com.hoc081098.solivagant.navigation.SavedStateSupport
import com.hoc081098.solivagant.navigation.ScreenDestination
import com.hoc081098.solivagant.navigation.rememberWindowLifecycleOwner
import kotlinx.collections.immutable.persistentSetOf

fun main() {
  val savedStateSupport = SavedStateSupport()

  application {
    savedStateSupport.ClearOnDispose()
    Window(
      onCloseRequest = ::exitApplication,
      title = "Solivagant",
    ) {
      val lifecycleOwner = checkNotNull(rememberWindowLifecycleOwner()) { "rememberWindowLifecycleOwner returns null" }
      savedStateSupport.ProvideCompositionLocals(LocalLifecycleOwner provides lifecycleOwner) { MyApp() }
    }
  }
}

val Navigator by lazy(LazyThreadSafetyMode.NONE) { NavEventNavigator() }

@Composable
fun MyApp() =
  NavHost(
    startRoute = FirstRoute,
    destinations = remember {
      persistentSetOf(
        FirstRouteDestination,
        SecondRouteDestination,
      )
    },
    navEventNavigator = Navigator,
  )

@Immutable
data object FirstRoute : NavRoot

val FirstRouteDestination = ScreenDestination<FirstRoute> { _, modifier ->
  Box(
    modifier = modifier.background(Color.Red.copy(alpha = 0.2f)),
    contentAlignment = Alignment.Center,
  ) {
    Button(onClick = { Navigator.navigateTo(SecondRoute) }) {
      Text("Go to second route")
    }
  }
}


@Immutable
data object SecondRoute : NavRoute

val SecondRouteDestination = ScreenDestination<SecondRoute> { _, modifier ->
  Box(
    modifier = modifier.background(Color.Green.copy(alpha = 0.2f)),
    contentAlignment = Alignment.Center,
  ) {
    Button(onClick = Navigator::navigateBack) {
      Text("Back to first route")
    }
  }
}
