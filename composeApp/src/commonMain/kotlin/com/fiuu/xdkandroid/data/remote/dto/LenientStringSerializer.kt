package com.fiuu.xdkandroid.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

// Reads a JSON string OR number as a String? — /inquire returns transaction_id as a bare number
// (e.g. 4062656819), which the non-lenient Json config would otherwise reject for a String field.
// Blank strings decode to null so an empty app_code/error_description doesn't override a real
// value when merged with the stream result (inquireResult.x ?: streamResult.x).
object LenientStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        String.serializer().nullable.descriptor

    override fun deserialize(decoder: Decoder): String? {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()
            ?: return decoder.decodeString().ifBlank { null }
        if (element !is JsonPrimitive || element is JsonNull) return null
        return element.content.ifBlank { null }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }
}
