package com.rinx.artRINXapp.core.tour

import com.rinx.artRINXapp.core.navigation.NavRoutes

/** Elements the tour can spotlight. Bounds are reported by the screen that owns each element. */
enum class TourTarget { HOME_NAV, CREATE_NAV, PROFILE_NAV, PROFILE_INVITE }

/**
 * The four tour steps, in order. [route] is the destination the step lives on — the host navigates
 * there before showing the step, so each step is shown ON its own tab: Welcome on Home, Upload on
 * the Create tab, and Profile + Invite on the Profile tab. The spotlight uses the matching nav-bar
 * bounds (reported by Home's bottom nav, identical across tabs) and the Invite button's own bounds.
 */
enum class TourStep(
    val target: TourTarget,
    val route: String,
    val body: String,
) {
    WELCOME(TourTarget.HOME_NAV, NavRoutes.HOME,
        "Welcome! Here's a quick tour of artRINX. Your homepage has three feeds to explore global art, shop, and find collected works for you!"),
    UPLOAD(TourTarget.CREATE_NAV, NavRoutes.CREATE,
        "Upload art and create collections."),
    PROFILE(TourTarget.PROFILE_NAV, NavRoutes.PROFILE,
        "View your uploaded art and collections. Your likes are private."),
    INVITE(TourTarget.PROFILE_INVITE, NavRoutes.PROFILE,
        "Invite people who believe in supporting the budding art community.");

    companion object {
        val ordered = entries
    }
}
