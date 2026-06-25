package com.example.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Dp

// Glassmorphism constants
val GlassBgColor = Color(0x11FFFFFF) // Highly translucent frosted glass base
val GlassBgColorHeavy = Color(0x22FFFFFF) // High contrast translucent frosted glass base
val GlassBorderColor = Color(0x26FFFFFF) // Subtle light border for highlight edge
val GlassBorderColorHighlight = Color(0x4DFFFFFF)

/**
 * A highly realistic, physical-looking glassmorphism modifier.
 * Simulates density, bevels, light reflection, and diagonal specular glare.
 */
fun Modifier.glassic(
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.dp,
    hasSpecularGlare: Boolean = true,
    borderBrush: Brush? = null
): Modifier = this.then(
    Modifier
        .background(
            brush = if (hasSpecularGlare) {
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x2BFFFFFF), // Lighter top-left bevel sheen
                        0.32f to Color(0x0FFFFFFF), // Frosted transparent plate core
                        0.46f to Color(0x20FFFFFF), // Diagonal specular light ray entry
                        0.50f to Color(0x36FFFFFF), // High-intensity gloss reflection peak
                        0.54f to Color(0x20FFFFFF), // Specular light ray exit
                        0.68f to Color(0x0FFFFFFF), // Frosted transparent plate core continuation
                        1.0f to Color(0x1FFFFFFF)  // Bottom-right ambient counter-reflection bounce
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x24FFFFFF),
                        Color(0x0AFFFFFF),
                        Color(0x1CFFFFFF)
                    )
                )
            },
            shape = shape
        )
        .border(
            width = borderWidth,
            brush = borderBrush ?: Brush.linearGradient(
                colors = listOf(
                    Color(0xB3FFFFFF), // Sharp bevel edge catching main light source (top-left)
                    Color(0x26FFFFFF), // Smooth fade on the sides
                    Color(0x08FFFFFF), // Deep refract shadow side (bottom-right)
                    Color(0x4DFFFFFF)  // Faint secondary edge reflection highlight
                )
            ),
            shape = shape
        )
)

/**
 * A ready-made premium physical Glass Card that uses our advanced glassic modifier.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.dp,
    hasSpecularGlare: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassic(shape = shape, borderWidth = borderWidth, hasSpecularGlare = hasSpecularGlare)
        ) {
            content()
        }
    }
}

@Composable
fun GlassScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // Pure premium obsidian black backdrop
    ) {
        content()
    }
}

// In-app solid black high-contrast notification component
@Composable
fun BlackSolidNotification(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.5.dp, Color(0xFF2E2E2E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Notification",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Global object to trigger in-app solid black notifications
object InAppNotifier {
    private val _notificationState = MutableTransitionState(false)
    val notificationState: State<Boolean> = derivedStateOf { _notificationState.currentState || _notificationState.targetState }
    
    var currentMessage by mutableStateOf("")
        private set

    fun showNotification(message: String) {
        currentMessage = message
        _notificationState.targetState = true
    }

    fun dismiss() {
        _notificationState.targetState = false
    }
}
