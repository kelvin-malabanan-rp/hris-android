package io.rocketpartners.hris.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.ContentCard
import io.rocketpartners.hris.designsystem.DSCard
import io.rocketpartners.hris.designsystem.EmptyState
import io.rocketpartners.hris.designsystem.FilterChip
import io.rocketpartners.hris.designsystem.InlineError
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.designsystem.hexColor
import io.rocketpartners.hris.model.CalendarEvent
import io.rocketpartners.hris.model.UserOnLeave
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.launch

private val MONTH_TITLE = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val DAY_TITLE = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

/** Calendar tab: filter chips, month grid, and the selected day's detail. Mirrors iOS `CalendarView`. */
@Composable
fun CalendarScreen(repository: CalendarRepository, modifier: Modifier = Modifier) {
    val store = remember { CalendarStore(repository) }
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }

    LaunchedEffect(state.visibleMonth) { store.reloadVisibleMonth() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Theme.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
    ) {
        io.rocketpartners.hris.designsystem.ScreenHeader("Calendar")
        FilterChipsRow(state.presentTypes, state.activeTypes, store::toggleType, store::clearTypes)

        MonthHeader(
            title = state.visibleMonth.atDay(1).format(MONTH_TITLE),
            isCurrentMonth = state.visibleMonth == java.time.YearMonth.now(),
            onPrev = { scope.launch { store.showPreviousMonth() } },
            onNext = { scope.launch { store.showNextMonth() } },
            onToday = { scope.launch { store.goToToday(); store.reloadVisibleMonth() } },
        )

        ContentCard {
            MonthGridView(
                weeks = MonthGrid.weeks(state.visibleMonth, firstDayOfWeek),
                monthValue = state.visibleMonth.monthValue,
                selected = state.selectedDate,
                firstDayOfWeek = firstDayOfWeek,
                dotColorsOn = { day -> dotColors(state.eventsOn(day)) },
                onSelect = { day -> scope.launch { store.selectDate(day) } },
            )
        }

        SelectedDayDetail(
            title = state.selectedDate.format(DAY_TITLE),
            events = state.eventsOn(state.selectedDate),
            usersOnLeave = state.usersOnLeave,
            phase = state.phase,
            onRetry = { store.load() },
        )
    }
}

@Composable
private fun MonthHeader(
    title: String,
    isCurrentMonth: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = Theme.brand)
        }
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (!isCurrentMonth) {
            Text(
                "Today",
                color = Theme.brand,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = Theme.Spacing.sm).clickable(onClick = onToday),
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = Theme.brand)
        }
    }
}

@Composable
private fun MonthGridView(
    weeks: List<List<LocalDate>>,
    monthValue: Int,
    selected: LocalDate,
    firstDayOfWeek: DayOfWeek,
    dotColorsOn: (LocalDate) -> List<Color>,
    onSelect: (LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        Row(Modifier.fillMaxWidth()) {
            MonthGrid.weekdaySymbols(firstDayOfWeek).forEach { symbol ->
                Text(
                    symbol,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        inMonth = day.monthValue == monthValue,
                        isToday = day == today,
                        isSelected = day == selected,
                        dotColors = dotColorsOn(day),
                        modifier = Modifier.weight(1f),
                        onClick = { if (day.monthValue == monthValue) onSelect(day) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    dotColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs),
    ) {
        Box(
            // Clip to a circle BEFORE clickable so the press ripple is a circle, not a square box.
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    when {
                        isSelected -> Modifier.background(Theme.brand)
                        isToday -> Modifier.border(Theme.Stroke.focus, Theme.brand, CircleShape)
                        else -> Modifier
                    },
                )
                .clickable(enabled = inMonth, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${day.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
        Row(
            modifier = Modifier.size(width = 40.dp, height = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dotColors.take(3).forEach { color ->
                Box(Modifier.size(5.dp).background(color, CircleShape))
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    types: List<EventTypeChip>,
    activeTypes: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm),
    ) {
        FilterChip("All", activeTypes.isEmpty(), onClear)
        types.forEach { type ->
            FilterChip(type.label, type.slug in activeTypes, { onToggle(type.slug) }, color = hexColor(type.color))
        }
    }
}

@Composable
private fun SelectedDayDetail(
    title: String,
    events: List<CalendarEvent>,
    usersOnLeave: List<UserOnLeave>,
    phase: Phase,
    onRetry: suspend () -> Unit,
) {
    DSCard(title = title) {
        when (phase) {
            is Phase.Idle, is Phase.Loading ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is Phase.Failed -> InlineError(message = phase.message, retry = onRetry)
            is Phase.Loaded ->
                if (events.isEmpty()) {
                    EmptyState(icon = Icons.Filled.EventBusy, title = "No events", compact = true)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                        events.forEach { EventRow(it) }
                    }
                }
        }
        HorizontalDivider(Modifier.padding(vertical = Theme.Spacing.sm))
        Text(
            if (usersOnLeave.isEmpty()) "On leave" else "On leave (${usersOnLeave.size})",
            style = MaterialTheme.typography.titleSmall,
        )
        if (usersOnLeave.isEmpty()) {
            EmptyState(icon = Icons.Filled.Groups, title = "Nobody on leave", accent = Theme.Accent.LEAVE, compact = true)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                usersOnLeave.forEach { LeaveRow(it) }
            }
        }
    }
}

private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

@Composable
private fun EventRow(event: CalendarEvent) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
        Box(Modifier.size(width = 4.dp, height = 36.dp).background(hexColor(event.color) ?: Theme.brand, RoundedCornerShape(2.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs)) {
            Text(eventDisplayTitle(event.type, event.title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            event.type?.let {
                Text(it.replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            timeLabel(event),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Prefix holiday events with the PH flag (the company observes Philippine holidays); other
 *  event types render their title unchanged. */
internal fun eventDisplayTitle(type: String?, title: String): String =
    if (type?.equals("holiday", ignoreCase = true) == true) "🇵🇭 $title" else title

private fun timeLabel(event: CalendarEvent): String {
    if (event.allDay) return "All day"
    val start = event.startDate ?: return ""
    return TIME_FMT.format(start.atZone(ZoneOffset.UTC))
}

@Composable
private fun LeaveRow(entry: UserOnLeave) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
        io.rocketpartners.hris.designsystem.Avatar(name = entry.user.name, size = 36.dp, accent = Theme.Accent.LEAVE)
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs)) {
            Text(entry.user.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(entry.leaveType.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Distinct event colors in first-appearance order, resolved to Compose colors. */
private fun dotColors(events: List<CalendarEvent>): List<Color> {
    val seen = LinkedHashSet<String>()
    val colors = mutableListOf<Color>()
    for (event in events) {
        val key = event.color ?: "accent"
        if (seen.add(key)) colors.add(hexColor(event.color) ?: Color(0xFF007AFF))
    }
    return colors
}
