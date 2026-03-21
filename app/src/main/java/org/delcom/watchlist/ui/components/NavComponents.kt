package org.delcom.watchlist.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

// ═════════════════════════════════════════════════════════════════════════════
// Bottom Navigation
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Definisi item Bottom Navigation Bar.
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val iconActive: ImageVector,
) {
    object Home    : BottomNavItem("home",    "Home",      Icons.Outlined.Home,   Icons.Filled.Home)
    object Movies  : BottomNavItem("movies",  "Watchlist", Icons.Outlined.Movie,  Icons.Filled.Movie)
    object Profile : BottomNavItem("profile", "Profil",    Icons.Outlined.Person, Icons.Filled.Person)
}

@Composable
fun BottomNavComponent(navController: NavHostController) {
    val items        = listOf(BottomNavItem.Home, BottomNavItem.Movies, BottomNavItem.Profile)
    val currentRoute = navController.currentDestination?.route

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.height(80.dp),
            tonalElevation = 0.dp,
        ) {
            items.forEach { screen ->
                val selected = currentRoute?.contains(screen.route) == true
                val animH by animateDpAsState(if (selected) 56.dp else 48.dp, label = "navH")

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = {
                        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                                )
                            }
                            Icon(
                                imageVector = if (selected) screen.iconActive else screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp),
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    modifier = Modifier.height(animH),
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = MaterialTheme.colorScheme.primary,
                        selectedTextColor   = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor      = Color.Transparent,
                    ),
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Top App Bar
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Item untuk dropdown menu di TopBar.
 */
data class TopBarMenuItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchListTopBar(
    title: String,
    navController: NavHostController,
    showBackButton: Boolean = true,
    showMenu: Boolean = false,
    menuItems: List<TopBarMenuItem> = emptyList(),
    onBackClick: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showBackButton) {
                        Card(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            onClick = { onBackClick?.invoke() ?: navController.popBackStack() },
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            actions = {
                if (showMenu && menuItems.isNotEmpty()) {
                    Box(Modifier.padding(end = 8.dp)) {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            menuItems.forEachIndexed { i, item ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Icon(
                                                item.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (item.isDestructive) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                item.text,
                                                color = if (item.isDestructive) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (item.isDestructive) FontWeight.Bold
                                                else FontWeight.Normal,
                                            )
                                        }
                                    },
                                    onClick = { expanded = false; item.onClick() },
                                )
                                // Divider sebelum item destructive terakhir
                                if (i == menuItems.size - 2 && menuItems.last().isDestructive) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Snackbar
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Snackbar dengan ikon warna berdasarkan tipe pesan.
 *
 * Format pesan: `"<type>|<text>"` — misal `"success|Film berhasil ditambahkan"`.
 * Tipe yang didukung: `success`, `error`, `warning`, `info` (default).
 */
@Composable
fun WatchListSnackbar(snackbarData: SnackbarData, onDismiss: () -> Unit) {
    val parts   = snackbarData.visuals.message.split("|", limit = 2)
    val type    = parts.getOrNull(0) ?: "info"
    val message = parts.getOrNull(1) ?: snackbarData.visuals.message

    val (icon, iconColor, bgColor) = when (type) {
        "error"   -> Triple(Icons.Default.Error,        Color(0xFFFA896B), Color(0xFFFFF1ED))
        "success" -> Triple(Icons.Default.CheckCircle,  Color(0xFF13DEB9), Color(0xFFE2FBF7))
        "warning" -> Triple(Icons.Default.Warning,      Color(0xFFFFAE1F), Color(0xFFFFF5E3))
        else      -> Triple(Icons.Default.Info,         Color(0xFF539BFF), Color(0xFFEAF3FF))
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(Modifier.width(12.dp))
            Text(message, color = Color(0xFF383A42), modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF383A42))
            }
        }
    }
}