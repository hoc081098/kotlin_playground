package com.hoc081098.kotlin_playground.solivagant

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.hoc081098.channeleventbus.ChannelEventBus
import com.hoc081098.solivagant.lifecycle.LocalLifecycleOwner
import com.hoc081098.solivagant.navigation.ClearOnDispose
import com.hoc081098.solivagant.navigation.ExperimentalSolivagantApi
import com.hoc081098.solivagant.navigation.NavEventNavigator
import com.hoc081098.solivagant.navigation.NavHost
import com.hoc081098.solivagant.navigation.ProvideCompositionLocals
import com.hoc081098.solivagant.navigation.SavedStateSupport
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
      savedStateSupport.ProvideCompositionLocals(
        LocalLifecycleOwner provides rememberWindowLifecycleOwner()!!
      ) { MyApp() }
    }
  }
}

val Navigator by lazy(NONE) { NavEventNavigator() }
val EventBus by lazy(NONE) { ChannelEventBus() }

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

