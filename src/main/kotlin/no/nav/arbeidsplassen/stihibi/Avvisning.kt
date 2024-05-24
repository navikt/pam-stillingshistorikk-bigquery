package no.nav.arbeidsplassen.stihibi

import java.time.LocalDateTime

data class Avvisning(
    val adUuid: String,
    val remarks: List<RemarkType>?,
    val reportee: String?,
    val avvist_tidspunkt: LocalDateTime
)

enum class RemarkType {
    NOT_APPROVED_BY_LABOUR_INSPECTION,
    NO_EMPLOYMENT,
    DUPLICATE,
    DISCRIMINATING,
    REJECT_BECAUSE_CAPACITY,
    FOREIGN_JOB,
    COLLECTION_JOB,
    UNKNOWN
}
