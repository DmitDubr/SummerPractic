package com.vertical.app.di

import com.vertical.app.core.config.AppConfig
import com.vertical.app.core.config.defaultApiBaseUrl
import com.vertical.app.core.network.VerticalApiClient
import com.vertical.app.core.storage.PlatformSessionStorage
import com.vertical.app.core.storage.SessionStorage
import com.vertical.app.data.*
import com.vertical.app.presentation.booking.BookingFormStore
import com.vertical.app.presentation.bookings.BookingDetailStore
import com.vertical.app.presentation.bookings.BookingListStore
import com.vertical.app.presentation.schedule.ScheduleStore
import com.vertical.app.presentation.slot.SlotDetailStore
import com.vertical.app.presentation.waitlist.WaitlistStore
import com.vertical.app.session.DefaultSessionRepository
import com.vertical.app.session.SessionRepository
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

fun initKoin() {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return
    startKoin { modules(verticalAppModule) }
}

val verticalAppModule = module {
    single { AppConfig() }
    single<SessionStorage> { PlatformSessionStorage }
    single<SessionRepository> { DefaultSessionRepository(get()) }
    single { VerticalApiClient(get(), defaultApiBaseUrl()) }

    single<SlotRepository> { KtorSlotRepository(get()) }
    single<InstructorRepository> { KtorInstructorRepository(get()) }
    single<ProfileRepository> { KtorProfileRepository(get(), get()) }
    single<BookingRepository> { KtorBookingRepository(get(), get()) }
    single<WaitlistRepository> { KtorWaitlistRepository(get(), get()) }
    single<RatingRepository> { KtorRatingRepository(get()) }
    single<PushRepository> { KtorPushRepository(get()) }

    viewModel { ScheduleStore(get(), get()) }
    viewModel { SlotDetailStore(get()) }
    viewModel { BookingFormStore(get(), get(), get()) }
    viewModel { BookingListStore(get(), get()) }
    viewModel { BookingDetailStore(get()) }
    viewModel { WaitlistStore(get(), get(), get()) }
}
