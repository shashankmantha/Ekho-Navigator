package com.ekhonavigator.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
This is temporary data only
We need to implement our core data repositories correctly
So this can be an actual dynamic and integrated display of relevant data
We can grab ideas from this section such as the basic structure of the data objects
But we shouldn't be defining any major shared data class in this feature class
All data definitions should be in core
*/
@Composable
internal fun DummyHomeContent(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        WeatherWidget(Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

        SectionLabel("NEXT UP", Modifier.padding(horizontal = 16.dp))
        NextClassCard(
            item = DummyData.nextUp,
            minutesUntil = 46,
            onClick = { onEventClick("comp350") },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Spacer(Modifier.height(8.dp))

        SectionLabel("TODAY'S SCHEDULE", Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column {
                DummyData.todaySchedule.forEachIndexed { index, item ->
                    ScheduleRow(item = item, onClick = { onEventClick(item.title) })
                    if (index < DummyData.todaySchedule.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionLabel("FRIENDS", Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        FriendsRow(Modifier.padding(horizontal = 16.dp))

        Spacer(Modifier.height(48.dp))
    }
}


// ── Dummy Data ──────────────────────────────────────────────

private enum class EventCategory { CLASS, EVENT, SOCIAL, DEADLINE }

private data class ScheduleItem(
    val title: String,
    val subtitle: String = "",
    val time: String,
    val location: String,
    val category: EventCategory,
    val tag: String? = null,
)

private data class FriendStub(val initials: String, val online: Boolean)

private object DummyData {
    val nextUp = ScheduleItem(
        title = "COMP 350 — Software Engineering",
        time = "9:00 AM – 10:15 AM",
        location = "Sierra Hall | Room 1422",
        category = EventCategory.CLASS,
    )

    val todaySchedule = listOf(
        ScheduleItem(
            title = "COMP 429 — Computer Architecture",
            time = "11:00 AM – 12:15 PM",
            location = "Madera 2510",
            category = EventCategory.CLASS,
        ),
        ScheduleItem(
            title = "COMP 350L — SE Lab",
            time = "2:00 PM – 3:15 PM",
            location = "Sierra Hall 1422",
            category = EventCategory.CLASS,
        ),
        ScheduleItem(
            title = "Career Fair — Grand Salon",
            time = "12:00 PM – 3:00 PM",
            location = "Student Union",
            category = EventCategory.EVENT,
            tag = "Event",
        ),
        ScheduleItem(
            title = "Study Group — COMP 429",
            subtitle = "with Marcos, Cristian",
            time = "3:30 PM – 4:30 PM",
            location = "Library 2nd Floor",
            category = EventCategory.SOCIAL,
            tag = "Social",
        ),
        ScheduleItem(
            title = "COMP 350 Sprint Review Due",
            subtitle = "Submit via Canvas",
            time = "11:59 PM",
            location = "",
            category = EventCategory.DEADLINE,
            tag = "Deadline",
        ),
        ScheduleItem(
            title = "MATH 352 — Probability & Stats",
            time = "4:00 PM – 5:15 PM",
            location = "Sierra Hall 1201",
            category = EventCategory.CLASS,
        ),
        ScheduleItem(
            title = "Intramural Soccer — Co-ed",
            subtitle = "vs Team Dolphins",
            time = "6:00 PM – 7:00 PM",
            location = "South Quad Field",
            category = EventCategory.SOCIAL,
            tag = "Social",
        ),
        ScheduleItem(
            title = "Campus Movie Night",
            subtitle = "Outdoor screening",
            time = "8:00 PM – 10:00 PM",
            location = "Central Mall Lawn",
            category = EventCategory.EVENT,
            tag = "Event",
        ),
        ScheduleItem(
            title = "COMP 429 Homework #4 Due",
            subtitle = "Pipeline hazard analysis",
            time = "11:59 PM",
            location = "",
            category = EventCategory.DEADLINE,
            tag = "Deadline",
        ),
    )

    val friends = listOf(
        FriendStub("MR", true),
        FriendStub("CM", true),
        FriendStub("SM", false),
        FriendStub("RS", false),
        FriendStub("JK", true),
    )
}


// ── Color Helper ────────────────────────────────────────────

private fun categoryColor(category: EventCategory): Color = when (category) {
    EventCategory.CLASS -> Color(0xFFC8102E)
    EventCategory.EVENT -> Color(0xFF1565C0)
    EventCategory.SOCIAL -> Color(0xFF6A1B9A)
    EventCategory.DEADLINE -> Color(0xFFE65100)
}


// ── Sub-composables ─────────────────────────────────────────

@Composable
private fun WeatherWidget(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1565C0), Color(0xFF1E88E5), Color(0xFF42A5F5)),
                ),
            )
            .padding(20.dp),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        "72°F",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            EkhoIcons.Cloud,
                            null,
                            Modifier.size(18.dp),
                            tint = Color.White.copy(0.85f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Partly Cloudy", color = Color.White.copy(0.9f), fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("📍 School Campus", color = Color.White.copy(0.7f), fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    WeatherPill(EkhoIcons.Air, "8 mph")
                    Spacer(Modifier.height(6.dp))
                    WeatherPill(EkhoIcons.WaterDrop, "65%")
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .background(Color.White.copy(0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    "4 classes today",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun WeatherPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        Modifier
            .background(Color.White.copy(0.18f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = Color.White.copy(0.9f))
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            color = Color.White.copy(0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SectionLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun NextClassCard(
    item: ScheduleItem,
    minutesUntil: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .background(
                        categoryColor(item.category),
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    ),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .background(
                            categoryColor(item.category).copy(0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        "in ${minutesUntil}m",
                        color = categoryColor(item.category),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(item: ScheduleItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(44.dp)
                .background(categoryColor(item.category), RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    append(item.time)
                    if (item.location.isNotEmpty()) {
                        append("  •  "); append(item.location)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.subtitle.isNotEmpty()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                )
            }
        }
        if (item.tag != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                item.tag,
                Modifier
                    .background(
                        categoryColor(item.category).copy(0.08f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = categoryColor(item.category),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun FriendsRow(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DummyData.friends.forEach { friend ->
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        friend.initials,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (friend.online) {
                    Box(Modifier
                        .size(10.dp)
                        .background(Color(0xFF2E7D32), CircleShape))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme { HomeScreen(onEventClick = {}) }
}