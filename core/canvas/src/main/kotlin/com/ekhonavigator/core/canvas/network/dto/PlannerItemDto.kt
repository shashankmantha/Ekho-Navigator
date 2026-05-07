package com.ekhonavigator.core.canvas.network.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

@Serializable
data class PlannerItemDto(
    @SerialName("plannable_id") val plannableId: String,
    @SerialName("plannable_type") val plannableType: String,
    @SerialName("course_id") val courseId: String? = null,
    @SerialName("plannable_date") val plannableDate: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("new_activity") val newActivity: Boolean = false,
    @SerialName("context_name") val contextName: String? = null,
    @SerialName("context_image") val contextImage: String? = null,
    val plannable: PlannableDto = PlannableDto(),
    @Serializable(with = SubmissionsOrFalseSerializer::class)
    val submissions: SubmissionsDto? = null,
)

@Serializable
data class PlannableDto(
    val title: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("points_possible") val pointsPossible: Double? = null,
)

@Serializable
data class SubmissionsDto(
    val submitted: Boolean = false,
    val late: Boolean = false,
    val missing: Boolean = false,
    val graded: Boolean = false,
    @SerialName("needs_grading") val needsGrading: Boolean = false,
    @SerialName("has_feedback") val hasFeedback: Boolean = false,
    val excused: Boolean = false,
)

/**
 * Canvas sends `"submissions": false` for plannable types without submission semantics
 * (announcements, calendar_events, planner_notes) instead of an object or null. Without
 * this serializer the whole response fails to parse on the first non-submittable item.
 * Treat any non-object value (false, true, null, primitive) as "no submission status."
 */
internal object SubmissionsOrFalseSerializer : KSerializer<SubmissionsDto?> {
    private val backing = SubmissionsDto.serializer().nullable
    override val descriptor: SerialDescriptor = backing.descriptor

    override fun serialize(encoder: Encoder, value: SubmissionsDto?) {
        backing.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): SubmissionsDto? {
        if (decoder !is JsonDecoder) return backing.deserialize(decoder)
        val element = decoder.decodeJsonElement()
        return (element as? JsonObject)
            ?.let { decoder.json.decodeFromJsonElement(SubmissionsDto.serializer(), it) }
    }
}
