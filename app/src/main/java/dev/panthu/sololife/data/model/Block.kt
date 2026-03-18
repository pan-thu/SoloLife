package dev.panthu.sololife.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Block {
    abstract val id: String

    @Serializable @SerialName("text")
    data class Text(override val id: String, val html: String) : Block()

    @Serializable @SerialName("image")
    data class Image(override val id: String, val paths: List<String>) : Block()

    @Serializable @SerialName("divider")
    data class Divider(override val id: String) : Block()

    @Serializable @SerialName("checklist")
    data class Checklist(override val id: String, val items: List<CheckItem>) : Block()
}

@Serializable
data class CheckItem(val id: String, val text: String, val checked: Boolean)
