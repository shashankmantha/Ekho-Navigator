package com.ekhonavigator.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.ekhonavigator.R
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.feature.schedule.navigation.ScheduleNavKey
import com.ekhonavigator.feature.social.navigation.SocialNavKey
import com.ekhonavigator.feature.map.navigation.MapNavKey
import com.ekhonavigator.feature.home.navigation.HomeNavKey

/**
 * Type for the top level navigation items in the application. Contains UI information about the
 * current route that is used in the top app bar and common navigation UI.
 *
 * @param selectedIcon The icon to be displayed in the navigation UI when this destination is
 * selected.
 * @param unselectedIcon The icon to be displayed in the navigation UI when this destination is
 * not selected.
 * @param label Text that to be displayed in the navigation UI.
 * @param titleRes The string resource for the screen title.
 */
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

val MAP = TopLevelNavItem(
    selectedIcon = EkhoIcons.MapFilled,
    unselectedIcon = EkhoIcons.MapOutlined,
    label = "Map",
)

val SCHEDULE = TopLevelNavItem(
    selectedIcon = EkhoIcons.CalendarFilled,
    unselectedIcon = EkhoIcons.CalendarOutlined,
    label = "Schedule",
)

val SOCIAL = TopLevelNavItem(
    selectedIcon = EkhoIcons.SocialFilled,
    unselectedIcon = EkhoIcons.SocialOutlined,
    label = "Social",
)

val TOP_LEVEL_NAV_ITEMS = mapOf(
    HomeNavKey to HOME,
    ScheduleNavKey to SCHEDULE,
    SocialNavKey to SOCIAL,
    MapNavKey to MAP,
)
