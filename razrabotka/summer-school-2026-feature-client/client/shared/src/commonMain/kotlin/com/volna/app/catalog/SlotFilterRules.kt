package com.volna.app.catalog

fun SlotFilters.hasActiveFilters(): Boolean =
    dateFrom != null ||
        dateTo != null ||
        routeTypes.isNotEmpty() ||
        instructorIds.isNotEmpty() ||
        onlyAvailable
