package com.ai.operator.feature.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.operator.core.agent.AgentState
import kotlin.math.roundToInt

@Composable
fun FloatingBubbleComponent(
    agentState: AgentState,
    onClick: () -> Unit,
    onDrag: (Int, Int) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val statusColor = when (agentState) {
        AgentState.IDLE -> Color(0xFF9E9E9E) // Gray
        AgentState.THINKING -> Color(0xFF2196F3) // Blue
        AgentState.PLANNING -> Color(0xFF03A9F4) // Light Blue
        AgentState.WAITING_CONFIRMATION -> Color(0xFFFFC107) // Yellow
        AgentState.EXECUTING -> Color(0xFF9C27B0) // Purple
        AgentState.OBSERVING -> Color(0xFFFF9800) // Orange
        AgentState.VERIFYING -> Color(0xFF00BCD4) // Cyan
        AgentState.COMPLETED -> Color(0xFF4CAF50) // Green
        AgentState.FAILED -> Color(0xFFF44336) // Red
        AgentState.STOPPED -> Color(0xFFE91E63) // Pink
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(64.dp)
            .clip(CircleShape)
            .background(statusColor)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    onDrag(offsetX.roundToInt(), offsetY.roundToInt())
                }
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🤖",
            fontSize = 28.sp
        )
    }
}
