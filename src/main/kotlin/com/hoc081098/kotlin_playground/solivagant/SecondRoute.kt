package com.hoc081098.kotlin_playground.solivagant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hoc081098.channeleventbus.OptionWhenSendingToBusDoesNotExist
import com.hoc081098.solivagant.navigation.NavRoute
import com.hoc081098.solivagant.navigation.ScreenDestination
import kotlin.random.Random

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
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = route.toString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
          )
          Spacer(Modifier.height(16.dp))
          Button(
            onClick = {
              Navigator.navigateBack()
              EventBus.send(
                event = SecondResultToFirst(number = Random.nextInt()),
                option = OptionWhenSendingToBusDoesNotExist.DO_NOTHING
              )
            }
          ) {
            Text("Back to first route")
          }
        }
      }
    }
  }
}
