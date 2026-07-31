package com.adaptiveoperator.ai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adaptiveoperator.ai.agent.AgentStatus
import com.adaptiveoperator.ai.presentation.theme.StatusActing
import com.adaptiveoperator.ai.presentation.theme.StatusError
import com.adaptiveoperator.ai.presentation.theme.StatusIdle
import com.adaptiveoperator.ai.presentation.theme.StatusObserving
import com.adaptiveoperator.ai.presentation.theme.StatusReady
import com.adaptiveoperator.ai.presentation.theme.StatusThinking

fun AgentStatus.color(): Color = when (this) {
    AgentStatus.READY, AgentStatus.SUCCESS -> StatusReady
    AgentStatus.LOADING -> StatusThinking
    AgentStatus.THINKING -> StatusThinking
    AgentStatus.ACTING -> StatusActing
    AgentStatus.OBSERVING -> StatusObserving
    AgentStatus.ERROR -> StatusError
    AgentStatus.IDLE -> StatusIdle
}

fun AgentStatus.label(): String = when (this) {
    AgentStatus.READY -> "Ready"
    AgentStatus.LOADING -> "Loading"
    AgentStatus.THINKING -> "Thinking"
    AgentStatus.ACTING -> "Acting"
    AgentStatus.OBSERVING -> "Observing"
    AgentStatus.SUCCESS -> "Success"
    AgentStatus.ERROR -> "Error"
    AgentStatus.IDLE -> "Idle"
}

@Composable
fun StatusIndicator(status: AgentStatus, modifier: Modifier = Modifier, showLabel: Boolean = true) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(10.dp)
                .background(status.color(), CircleShape)
        )
        if (showLabel) {
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Text(status.label(), style = MaterialTheme.typography.labelSmall)
        }
    }
}
