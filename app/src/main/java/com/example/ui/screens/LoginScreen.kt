package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppSettings
import com.example.ui.theme.PrimaryNavy
import com.example.util.BiometricAuthManager

/**
 * Login Screen composable providing minimal, clean UI with icon-only biometric authentication.
 * Guarantees a single instance of the system BiometricPrompt without redundant subtitle/description text,
 * preventing duplicate text overlays, double dialog invocations, and superfluous labels.
 */
@Composable
fun LoginScreen(
    settings: AppSettings,
    isCloudConnected: Boolean,
    isInitialLoading: Boolean = false,
    isPermissionDenied: Boolean = false,
    isDarkTheme: Boolean = false,
    isBiometricAvailable: Boolean = false,
    isBiometricEnabled: Boolean = false,
    onTriggerBiometric: (targetProfile: String) -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onSelectProfile: (userId: String) -> Unit, // "ADMIN", "STAFF_1", "STAFF_2"
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adminName = settings.getAdminName(false)
    val staff1Name = settings.getStaff1Name(false)
    val staff2Name = settings.getStaff2Name(false)

    // Tracks currently selected profile to guarantee explicit session mapping and prevent cross-account login
    var selectedProfile by rememberSaveable {
        mutableStateOf(BiometricAuthManager.getLastProfile(context))
    }

    val bgGradient = if (isDarkTheme) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
    } else {
        listOf(Color(0xFFF8FAFC), Color(0xFFEFF6FF), Color(0xFFE2E8F0))
    }

    val cardBgGradient = if (isDarkTheme) {
        listOf(Color(0xFF1E293B), Color(0xFF0F172A))
    } else {
        listOf(Color.White, Color(0xFFF8FAFC))
    }

    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = bgGradient))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION: Theme Toggle Bar & Brand Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Action Bar with Manual Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { onToggleTheme() }
                            .testTag("landing_theme_toggle_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Brand Hero Logo Container with Glow Border
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(1.5.dp, if (isDarkTheme) Color(0xFF38BDF8).copy(alpha = 0.6f) else PrimaryNavy.copy(alpha = 0.4f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Zoha Trading Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Shop Title
                Text(
                    text = settings.shopName.ifBlank { "ZOHA TRADING" }.uppercase(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // MIDDLE SECTION: 3 Profile Login Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT PROFILE TO LOGIN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = subtitleColor
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 1. JASIM
                ProfileLoginCard(
                    name = adminName.ifBlank { "Jasim" },
                    isSelected = selectedProfile == "ADMIN",
                    avatarIcon = Icons.Default.VerifiedUser,
                    accentColor = Color(0xFFF59E0B),
                    cardBgGradient = cardBgGradient,
                    borderColor = if (selectedProfile == "ADMIN") Color(0xFFF59E0B) else Color(0xFFF59E0B).copy(alpha = 0.4f),
                    textColor = textColor,
                    testTag = "landing_profile_admin_btn",
                    onClick = {
                        selectedProfile = "ADMIN"
                        BiometricAuthManager.setLastProfile(context, "ADMIN")
                        onSelectProfile("ADMIN")
                    }
                )

                // 2. SUMON
                ProfileLoginCard(
                    name = staff1Name.ifBlank { "Sumon" },
                    isSelected = selectedProfile == "STAFF_1",
                    avatarIcon = Icons.Default.Person,
                    accentColor = Color(0xFF38BDF8),
                    cardBgGradient = cardBgGradient,
                    borderColor = if (selectedProfile == "STAFF_1") Color(0xFF38BDF8) else Color(0xFF38BDF8).copy(alpha = 0.4f),
                    textColor = textColor,
                    testTag = "landing_profile_sumon_btn",
                    onClick = {
                        selectedProfile = "STAFF_1"
                        BiometricAuthManager.setLastProfile(context, "STAFF_1")
                        onSelectProfile("STAFF_1")
                    }
                )

                // 3. DEEDAT
                ProfileLoginCard(
                    name = staff2Name.ifBlank { "Deedat" },
                    isSelected = selectedProfile == "STAFF_2",
                    avatarIcon = Icons.Default.Person,
                    accentColor = Color(0xFFA78BFA),
                    cardBgGradient = cardBgGradient,
                    borderColor = if (selectedProfile == "STAFF_2") Color(0xFFA78BFA) else Color(0xFFA78BFA).copy(alpha = 0.4f),
                    textColor = textColor,
                    testTag = "landing_profile_deedat_btn",
                    onClick = {
                        selectedProfile = "STAFF_2"
                        BiometricAuthManager.setLastProfile(context, "STAFF_2")
                        onSelectProfile("STAFF_2")
                    }
                )

                // Minimal, clean, ICON-ONLY Fingerprint Button directly BELOW user buttons
                // No text label underneath or inside the button component itself
                if (isBiometricAvailable && isBiometricEnabled) {
                    Spacer(modifier = Modifier.height(2.dp))

                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = if (isDarkTheme) 0.85f else 0.65f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { onTriggerBiometric(selectedProfile) }
                            .testTag("landing_biometric_login_btn")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Login",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // BOTTOM SECTION: Cloud Sync Status Footer & Developer Credit
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isCloudConnected -> Color(0xFF22C55E)
                            isInitialLoading -> Color(0xFF38BDF8)
                            else -> Color(0xFF10B981)
                        },
                        modifier = Modifier.size(7.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isCloudConnected -> "Cloud Sync Active (Online)"
                            isInitialLoading -> "Connecting to Cloud Database..."
                            else -> "Cloud Sync Active (Online)"
                        },
                        fontSize = 11.sp,
                        color = if (isCloudConnected) Color(0xFF22C55E) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = buildAnnotatedString {
                        append("Design & Develop By ")
                        withStyle(
                            style = SpanStyle(
                                color = if (isDarkTheme) Color(0xFFFB923C) else Color(0xFFEA580C),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Jasim Uddin")
                        }
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("developer_credit_text")
                )
            }
        }
    }
}

@Composable
private fun ProfileLoginCard(
    name: String,
    isSelected: Boolean = false,
    avatarIcon: ImageVector,
    accentColor: Color,
    cardBgGradient: List<Color>,
    borderColor: Color,
    textColor: Color = Color.White,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBgGradient.first(),
        border = BorderStroke(if (isSelected) 2.dp else 1.2.dp, borderColor),
        shadowElevation = if (isSelected) 5.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(cardBgGradient))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Profile Avatar Icon
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = avatarIcon,
                            contentDescription = name,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Clean User Name Vertically Centered
                Text(
                    text = name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Arrow
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Login",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
