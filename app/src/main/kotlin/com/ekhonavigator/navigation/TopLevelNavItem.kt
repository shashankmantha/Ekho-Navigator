package com.ekhonavigator.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.R
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.feature.calendar.navigation.CalendarNavKey
import com.ekhonavigator.feature.discover.navigation.DiscoverNavKey
import com.ekhonavigator.feature.home.navigation.HomeNavKey
import com.ekhonavigator.feature.map.navigation.MapNavKey
import com.ekhonavigator.feature.social.navigation.SocialNavKey

data class TopLevelNavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    @StringRes val titleRes: Int = R.string.app_name,
)

val HOME = TopLevelNavItem(
    selectedIcon = EkhoIcons.Home,
    unselectedIcon = EkhoIcons.HomeOutlined,
    label = "Home",
)

val CALENDAR = TopLevelNavItem(
    selectedIcon = EkhoIcons.CalendarFilled,
    unselectedIcon = EkhoIcons.CalendarOutlined,
    label = "Calendar",
)

// "Discover" was renamed to "Campus" — NavKey stays DiscoverNavKey so saved
// back-stacks from older installs still resolve.
val DISCOVER = TopLevelNavItem(
    selectedIcon = EkhoIcons.EventsFilled,
    unselectedIcon = EkhoIcons.EventsOutlined,
    label = "Campus",
)

val SOCIAL = TopLevelNavItem(
    selectedIcon = EkhoIcons.SocialFilled,
    unselectedIcon = EkhoIcons.SocialOutlined,
    label = "Social",
)

val MAP = TopLevelNavItem(
    selectedIcon = EkhoIcons.MapFilled,
    unselectedIcon = EkhoIcons.MapOutlined,
    label = "Map",
)

val TOP_LEVEL_NAV_ITEMS: Map<NavKey, TopLevelNavItem> = mapOf(
    HomeNavKey to HOME,
    CalendarNavKey to CALENDAR,
    DiscoverNavKey() to DISCOVER,
    SocialNavKey to SOCIAL,
    MapNavKey() to MAP,
)
