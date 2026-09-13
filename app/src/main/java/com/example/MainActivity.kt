package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NotificationPermissionHandler {
                        SpeakerEnablerScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationPermissionHandler(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            Log.d("NotificationPermission", "Permission granted: $isGranted")
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            hasPermission = (status == PackageManager.PERMISSION_GRANTED)
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            hasPermission = true
        }
    }

    content()
}

@Composable
fun SpeakerEnablerScreen(
    modifier: Modifier = Modifier,
    viewModel: AudioViewModel = viewModel()
) {
    val isSpeakerForced by viewModel.isSpeakerForceEnabled.collectAsStateWithLifecycle()
    val isHeadphoneConnected by viewModel.isHeadphoneConnected.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val isPlayingTestSound by viewModel.isTestSoundPlaying.collectAsStateWithLifecycle()
    val useCallStream by viewModel.useCallStream.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(Color(0xFF0F0F13)) // Deep visual obsidian background
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Title Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Speaker Icon",
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SPEAKER ENABLER",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color.White
                )
            }
            Text(
                text = "Headphone Jack Stuck Bypass Utility",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        // Hardware Status Indicator Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191922)),
            shape = RoundedCornerShape(16.dp),
            border = borderStrokeForStatus(isHeadphoneConnected)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hardware Status Check",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Glowing connection indicator
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isHeadphoneConnected) Color(0xFFFFB300) else Color(0xFF00FF66))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isHeadphoneConnected) "Headphones Stuck/Connected" else "No Headphones Detected",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isHeadphoneConnected) Color(0xFFFFCC00) else Color(0xFF90FF90)
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.ic_headset),
                        contentDescription = "Headset State",
                        tint = if (isHeadphoneConnected) Color(0xFFFFB300) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info Icon",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHeadphoneConnected) {
                            "If your earphones are unplugged, your hardware sensor is stuck. Enable Force Speaker below."
                        } else {
                            "Device hardware is reporting normal routing."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        // Animated Toggle Button Controller
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isSpeakerForced) 1.08f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            val buttonColor by animateColorAsState(
                targetValue = if (isSpeakerForced) Color(0xFF00FFCC) else Color(0xFF2E2E3A),
                label = "buttonColor"
            )

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(buttonColor.copy(alpha = 0.15f))
                    .border(
                        width = 3.dp,
                        color = buttonColor.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable(onClickLabel = "Toggle Speaker Override") {
                        viewModel.toggleSpeakerForce()
                    }
                    .testTag("toggle_speaker_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isSpeakerForced) R.drawable.ic_speaker else R.drawable.ic_headset
                        ),
                        contentDescription = "Toggle Speaker Override",
                        tint = buttonColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isSpeakerForced) "FORCE SPEAKER" else "SYSTEM DEFAULT",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSpeakerForced) {
                    if (useCallStream) "LOUDSPEAKER OVERRIDE ACTIVE (CALL STREAM)" else "LOUDSPEAKER OVERRIDE ACTIVE (MEDIA STREAM)"
                } else {
                    "AUTO-ROUTING ENABLED"
                },
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = if (isSpeakerForced) Color(0xFF00FFCC) else Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        // Routing Stream Configuration Card (SOLVES SILENCING ISSUES)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191922)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (useCallStream) Color(0xFF90CAF9) else Color(0xFF00FFCC).copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (useCallStream) Icons.Default.PhonelinkRing else Icons.Default.Radio,
                                contentDescription = null,
                                tint = if (useCallStream) Color(0xFF90CAF9) else Color(0xFF00FFCC),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (useCallStream) "Active Profile: Call Stream" else "Active Profile: Media Stream (Recommended)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (useCallStream) {
                                "Uses system communication channel. Guaranteed routing lock, but standard media sounds might be quiet/muted depending on your Android version."
                            } else {
                                "Forces standard media routing. Perfect for YouTube, music, movies, games, and system tones. Recommended default."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = useCallStream,
                        onCheckedChange = { viewModel.toggleRoutingStream() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF90CAF9),
                            checkedTrackColor = Color(0xFF0D47A1),
                            uncheckedThumbColor = Color(0xFF00FFCC),
                            uncheckedTrackColor = Color(0xFF003D32)
                        ),
                        modifier = Modifier
                            .testTag("toggle_stream_profile_button")
                    )
                }
            }
        }

        // Persistence & Service Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191922)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Protection",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Keep speaker routing active when screen is off or app is closed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { viewModel.toggleForegroundService() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00FFCC),
                            checkedTrackColor = Color(0xFF005244),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF2E2E3A)
                        ),
                        modifier = Modifier
                            .testTag("toggle_service_button")
                    )
                }

                if (isServiceRunning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF003D32), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Running",
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Background service is running smoothly.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFFCCFFFA)
                        )
                    }
                }
            }
        }

        // Diagnostics Card (Test Sound & Reset)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191922)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Audio Diagnostics",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Text(
                    text = "Test if sound correctly plays from the loudspeaker. This will play a system tone at your selected stream profile level.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Play / Stop Test Sound Button
                Button(
                    onClick = { viewModel.playTestSound() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("play_sound_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayingTestSound) Color(0xFFFF3366) else Color(0xFF00FFCC),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isPlayingTestSound) Icons.Default.MusicOff else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPlayingTestSound) "STOP DIAGNOSTIC SOUND" else "PLAY DIAGNOSTIC SOUND",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }

                // Reset System Defaults Button
                OutlinedButton(
                    onClick = { viewModel.resetToDefault() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_audio_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5555)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5555)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset routing"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RESET TO SYSTEM DEFAULT",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun borderStrokeForStatus(isHeadphoneConnected: Boolean): androidx.compose.foundation.BorderStroke {
    val strokeColor = if (isHeadphoneConnected) Color(0xFFFFB300) else Color(0xFF2E2E3A)
    return androidx.compose.foundation.BorderStroke(1.5.dp, strokeColor)
}
