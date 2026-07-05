package com.vertical.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vertical.app.presentation.booking.BookingFormIntent
import com.vertical.app.presentation.booking.BookingFormScreen
import com.vertical.app.presentation.booking.BookingFormStore
import com.vertical.app.presentation.bookings.*
import com.vertical.app.presentation.schedule.ScheduleIntent
import com.vertical.app.presentation.schedule.ScheduleScreen
import com.vertical.app.presentation.schedule.ScheduleStore
import com.vertical.app.presentation.slot.SlotDetailIntent
import com.vertical.app.presentation.slot.SlotDetailScreen
import com.vertical.app.presentation.slot.SlotDetailStore
import com.vertical.app.presentation.waitlist.WaitlistIntent
import com.vertical.app.presentation.waitlist.WaitlistScreen
import com.vertical.app.presentation.waitlist.WaitlistStore
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VerticalApp() {
    MaterialTheme {
        val navController = rememberNavController()
        val scheduleStore = koinViewModel<ScheduleStore>()
        val slotDetailStore = koinViewModel<SlotDetailStore>()
        val bookingFormStore = koinViewModel<BookingFormStore>()
        val bookingListStore = koinViewModel<BookingListStore>()
        val bookingDetailStore = koinViewModel<BookingDetailStore>()
        val waitlistStore = koinViewModel<WaitlistStore>()

        val scheduleState by scheduleStore.state.collectAsState()
        val slotDetailState by slotDetailStore.state.collectAsState()
        val bookingFormState by bookingFormStore.state.collectAsState()
        val bookingListState by bookingListStore.state.collectAsState()
        val bookingDetailState by bookingDetailStore.state.collectAsState()
        val waitlistState by waitlistStore.state.collectAsState()

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val selectedTab = when {
            currentDestination?.hasRoute<BookingsDestination>() == true -> MainTab.Bookings
            currentDestination?.hasRoute<BookingDetailDestination>() == true -> MainTab.Bookings
            else -> MainTab.Schedule
        }

        val showNavBar = currentDestination?.hasRoute<SlotDetailDestination>() != true &&
            currentDestination?.hasRoute<BookingFormDestination>() != true &&
            currentDestination?.hasRoute<BookingDetailDestination>() != true &&
            currentDestination?.hasRoute<WaitlistDestination>() != true

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            NavHost(
                navController,
                startDestination = ScheduleDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showNavBar) 72.dp else 0.dp),
            ) {
                composable<ScheduleDestination> {
                    ScheduleScreen(
                        state = scheduleState,
                        onIntent = scheduleStore::accept,
                        onSlotClick = { id ->
                            slotDetailStore.accept(SlotDetailIntent.Open(id))
                            navController.navigate(SlotDetailDestination(id.value))
                        },
                    )
                }
                composable<SlotDetailDestination> { entry ->
                    val route = entry.toRoute<SlotDetailDestination>()
                    LaunchedEffect(route.slotId) {
                        slotDetailStore.accept(SlotDetailIntent.Open(route.id()))
                    }
                    SlotDetailScreen(
                        state = slotDetailState,
                        onIntent = slotDetailStore::accept,
                        onBack = { navController.popBackStack() },
                        onBook = { id ->
                            bookingFormStore.accept(BookingFormIntent.Open(id))
                            navController.navigate(BookingFormDestination(id.value))
                        },
                        onWaitlist = { id ->
                            waitlistStore.accept(WaitlistIntent.Open(id))
                            navController.navigate(WaitlistDestination(id.value))
                        },
                    )
                }
                composable<BookingFormDestination> { entry ->
                    val route = entry.toRoute<BookingFormDestination>()
                    LaunchedEffect(route.slotId) {
                        bookingFormStore.accept(BookingFormIntent.Open(route.id()))
                    }
                    BookingFormScreen(
                        state = bookingFormState,
                        onIntent = bookingFormStore::accept,
                        onBack = { navController.popBackStack() },
                        onSuccessNavigate = {
                            bookingListStore.accept(BookingListIntent.Refresh)
                            scheduleStore.accept(ScheduleIntent.Refresh)
                            navController.navigate(BookingsDestination) {
                                popUpTo<ScheduleDestination> { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                    )
                    LaunchedEffect(bookingFormStore) {
                        while (true) {
                            val slotId = bookingFormStore.effects()
                            waitlistStore.accept(WaitlistIntent.Open(slotId))
                            navController.navigate(WaitlistDestination(slotId.value)) {
                                popUpTo<BookingFormDestination> { inclusive = true }
                            }
                        }
                    }
                }
                composable<WaitlistDestination> { entry ->
                    val route = entry.toRoute<WaitlistDestination>()
                    LaunchedEffect(route.slotId) {
                        waitlistStore.accept(WaitlistIntent.Open(route.id()))
                    }
                    WaitlistScreen(
                        state = waitlistState,
                        onIntent = waitlistStore::accept,
                        onBack = { navController.popBackStack() },
                        onSuccessNavigate = {
                            bookingListStore.accept(BookingListIntent.Refresh)
                            navController.navigate(BookingsDestination) {
                                popUpTo<ScheduleDestination> { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable<BookingsDestination> {
                    LaunchedEffect(Unit) {
                        bookingListStore.accept(BookingListIntent.Refresh)
                    }
                    BookingListScreen(
                        state = bookingListState,
                        onIntent = bookingListStore::accept,
                        onBookingClick = { id ->
                            bookingDetailStore.accept(BookingDetailIntent.Open(id))
                            navController.navigate(BookingDetailDestination(id.value))
                        },
                        onGoSchedule = {
                            navController.popBackStack(ScheduleDestination, inclusive = false)
                        },
                    )
                }
                composable<BookingDetailDestination> { entry ->
                    val route = entry.toRoute<BookingDetailDestination>()
                    LaunchedEffect(route.bookingId) {
                        bookingDetailStore.accept(BookingDetailIntent.Open(route.id()))
                    }
                    BookingDetailScreen(
                        state = bookingDetailState,
                        onIntent = bookingDetailStore::accept,
                        onBack = { navController.popBackStack() },
                        onGoSchedule = {
                            navController.popBackStack(ScheduleDestination, inclusive = false)
                        },
                    )
                    LaunchedEffect(bookingDetailStore) {
                        while (true) {
                            when (bookingDetailStore.effects()) {
                                BookingDetailEffect.Cancelled -> {
                                    bookingListStore.accept(BookingListIntent.Refresh)
                                    scheduleStore.accept(ScheduleIntent.Refresh)
                                    navController.popBackStack()
                                }
                                BookingDetailEffect.LeftWaitlist -> {
                                    bookingListStore.accept(BookingListIntent.Refresh)
                                    scheduleStore.accept(ScheduleIntent.Refresh)
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                }
            }

            if (showNavBar) {
                NavigationBar(Modifier.align(Alignment.BottomCenter)) {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                when (tab) {
                                    MainTab.Schedule -> {
                                        navController.popBackStack(ScheduleDestination, inclusive = false)
                                    }
                                    MainTab.Bookings -> {
                                        navController.navigate(BookingsDestination) {
                                            popUpTo(ScheduleDestination) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            },
                            icon = {
                                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        when (tab) {
                                            MainTab.Schedule -> "Р"
                                            MainTab.Bookings -> "З"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            },
                            label = { Text(tab.title) },
                        )
                    }
                }
            }
        }
    }
}
