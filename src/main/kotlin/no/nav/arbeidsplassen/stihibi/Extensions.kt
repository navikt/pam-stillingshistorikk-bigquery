package no.nav.arbeidsplassen.stihibi

import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


val HEX_CHARS = "0123456789ABCDEF".toCharArray()
val bqDatetimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")

fun String.toMD5Hex(): String {
    val digest = MessageDigest.getInstance("MD5")!!
    val hex = digest.digest(this.toByteArray()).hexBinary()
    digest.reset()
    return hex
}

fun ByteArray.hexBinary(): String {
    val r = StringBuilder(size * 2)
    forEach {
        val i = it.toInt()
        r.append(HEX_CHARS[i shr 4 and 0xF])
        r.append(HEX_CHARS[i and 0xF])
    }
    return r.toString()
}

fun LocalDateTime.toBqDateTime(): String = format(bqDatetimeFormatter)

