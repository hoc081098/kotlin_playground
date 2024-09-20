package com.hoc081098.kotlin_playground.solivagant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.hoc081098.channeleventbus.ChannelEventBusException
import com.hoc081098.channeleventbus.ValidationBeforeClosing
import com.hoc081098.flowext.FlowExtPreview
import com.hoc081098.flowext.catchAndResume
import com.hoc081098.kmp.viewmodel.Closeable
import com.hoc081098.solivagant.lifecycle.compose.collectAsStateWithLifecycle
import com.hoc081098.solivagant.navigation.NavRoot
import com.hoc081098.solivagant.navigation.ScreenDestination
import com.hoc081098.solivagant.navigation.rememberCloseableOnRoute
import kotlin.random.Random
import kotlinx.coroutines.flow.emptyFlow

@Immutable
data object FirstRoute : NavRoot {
  @OptIn(FlowExtPreview::class)
  @JvmStatic
  @Stable
  val Destination = ScreenDestination<FirstRoute> { route, modifier ->
    val result by remember {
      EventBus
        .receiveAsFlow(SecondResultToFirst)
        .catchAndResume {
          if (it is ChannelEventBusException.FlowAlreadyCollected) emptyFlow()
          else throw it
        }
    }.collectAsStateWithLifecycle(null)

    rememberCloseableOnRoute(route) {
      Closeable {
        EventBus.closeKey(
          key = SecondResultToFirst,
          validations = ValidationBeforeClosing.NONE,
        )
      }
    }

    Box(
      modifier = modifier.background(Color.Red.copy(alpha = 0.2f)),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "result: $result",
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.titleLarge,
        )

        Button(
          onClick = {
            Navigator.navigateTo(
              SecondRoute(
                id = Random.nextInt().toString(),
                otherIds = List(2) { Random.nextInt().toString() }
              )
            )
          }
        ) { Text("Go to second route") }
      }
    }
  }
}
