package com.hoc081098.kotlin_playground.solivagant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.hoc081098.solivagant.lifecycle.LocalLifecycleOwner
import com.hoc081098.solivagant.navigation.ClearOnDispose
import com.hoc081098.solivagant.navigation.ExperimentalSolivagantApi
import com.hoc081098.solivagant.navigation.NavEventNavigator
import com.hoc081098.solivagant.navigation.NavHost
import com.hoc081098.solivagant.navigation.NavRoot
import com.hoc081098.solivagant.navigation.NavRoute
import com.hoc081098.solivagant.navigation.ProvideCompositionLocals
import com.hoc081098.solivagant.navigation.SavedStateSupport
import com.hoc081098.solivagant.navigation.ScreenDestination
import com.hoc081098.solivagant.navigation.rememberWindowLifecycleOwner
import kotlin.LazyThreadSafetyMode.NONE
import kotlinx.collections.immutable.persistentSetOf

@OptIn(ExperimentalSolivagantApi::class)
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

@Stable
val Navigator by lazy(NONE) { NavEventNavigator() }

@Composable
fun MyApp(modifier: Modifier = Modifier) {
  Scaffold { paddingValues ->
    NavHost(
      modifier = modifier
        .padding(paddingValues)
        .consumeWindowInsets(paddingValues),
      startRoute = FirstRoute,
      destinations = remember {
        persistentSetOf(
          FirstRoute.Destination,
          SecondRoute.Destination,
        )
      },
      navEventNavigator = Navigator,
    )
  }
}

@Immutable
data object FirstRoute : NavRoot {
  @JvmStatic
  @Stable
  val Destination = ScreenDestination<FirstRoute> { _, modifier ->
    Box(
      modifier = modifier.background(Color.Red.copy(alpha = 0.2f)),
      contentAlignment = Alignment.Center,
    ) {
      Button(
        onClick = {
          Navigator.navigateTo(
            SecondRoute(
              id = "123",
              otherIds = listOf("456", "789"),
            )
          )
        }
      ) {
        Text("Go to second route")
      }
    }
  }
}

@Immutable
data class SecondRoute(
  val id: String,
  val otherIds: List<String>
) : NavRoute {
  companion object {
    @JvmStatic
    @Stable
    val Destination = ScreenDestination<SecondRoute> { route, modifier ->
      Box(
        modifier = modifier.background(Color.Green.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
      ) {
        Column {
          Text(
            text = route.toString(),
            style = MaterialTheme.typography.displayMedium,
          )
          Spacer(Modifier.height(16.dp))
          Button(onClick = Navigator::navigateBack) {
            Text("Back to first route")
          }
        }
      }
    }
  }
}
