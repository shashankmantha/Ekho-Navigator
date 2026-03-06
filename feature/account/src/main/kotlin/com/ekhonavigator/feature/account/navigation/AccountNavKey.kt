package com.ekhonavigator.feature.account.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.feature.account.AccountScreen
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
@Serializable
object AccountNavKey : NavKey

fun Navigator.navigateToAccount() {
    navigate(AccountNavKey)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.eventEntry(navigator: Navigator) {
    entry<AccountNavKey>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) { key ->
        AccountScreen()
    }
}