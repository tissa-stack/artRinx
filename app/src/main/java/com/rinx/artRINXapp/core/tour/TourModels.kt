package com.rinx.artRINXapp.core.tour

import com.rinx.artRINXapp.core.navigation.NavRoutes

/** Elements the tour can spotlight. Bounds are reported by the screen that owns each element. */
enum class TourTarget { HOME_NAV, DISCOVER_TAB, SHOP_TAB, FORYOU_TAB, SEARCH_BAR, CREATE_NAV }

/**
 * The six tour steps, in order. [route] is the bottom-tab destination the step lives on — the host
 * navigates there before showing the step (Discover→Shop→For You happen on Home; the Search step
 * runs on the Search screen and spotlights its search bar; the Create step returns to Home).
 */
enum class TourStep(
    val target: TourTarget,
    val route: String,
    val body: String,
) {
    WELCOME(TourTarget.HOME_NAV, NavRoutes.HOME,
        "Welcome to RINX! Learn about what's new, recommended, and trending."),
    DISCOVER(TourTarget.DISCOVER_TAB, NavRoutes.HOME,
        "Get inspired by art and curations (collections of art) shared by the entire RINX community."),
    SHOP(TourTarget.SHOP_TAB, NavRoutes.HOME,
        "Browse artists' latest creations available to purchase."),
    FOR_YOU(TourTarget.FORYOU_TAB, NavRoutes.HOME,
        "See what artists and collectors you follow are sharing."),
    SEARCH(TourTarget.SEARCH_BAR, NavRoutes.SEARCH,
        "Find art, curations, artists, collectors, or galleries by entering key words or names."),
    CREATE(TourTarget.CREATE_NAV, NavRoutes.HOME,
        "Upload art from your collection and create curations.");

    companion object {
        val ordered = entries
    }
}
