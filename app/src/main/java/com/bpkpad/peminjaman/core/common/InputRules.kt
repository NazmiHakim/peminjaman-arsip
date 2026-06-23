package com.bpkpad.peminjaman.core.common

/**
 * Shared input constraints for the loan module.
 *
 * UI filtering improves usability, while domain/database validation remains
 * the source of truth for data integrity.
 */
object InputRules {
    const val APPLICANT_NAME_MAX = 50
    const val WORK_UNIT_MAX = 150
    const val PHONE_MIN = 10
    const val PHONE_MAX = 15
    const val LETTER_NUMBER_MAX = 100
    const val DOCUMENT_SEARCH_MAX = 120
    const val BYPASS_NOTE_MAX = 500
    const val EXTENSION_REASON_MAX = 500
    const val AGENCY_NAME_MAX = 150
    const val AGENCY_CODE_MAX = 20
    const val AGENCY_ADDRESS_MAX = 255

    val applicantNameRegex = Regex("""^[\p{L}\p{M} .,'’()-]{1,$APPLICANT_NAME_MAX}$""")
    val workUnitRegex = Regex("""^[\p{L}\p{M}\p{N} .,'’()/&+\-]{1,$WORK_UNIT_MAX}$""")
    val phoneRegex = Regex("""^[0-9]{$PHONE_MIN,$PHONE_MAX}$""")
    val letterNumberRegex =
        Regex("""^[\p{L}\p{M}\p{N} ./,_:;()'’&+\-]{1,$LETTER_NUMBER_MAX}$""")
    val documentSearchRegex =
        Regex("""^[\p{L}\p{M}\p{N} ./,_:;()'’&+\-]{0,$DOCUMENT_SEARCH_MAX}$""")
    val bypassNoteRegex =
        Regex("""^[\p{L}\p{M}\p{N}\p{Zs}\r\n.,;:!?()/'"’@#&%+_\-]{1,$BYPASS_NOTE_MAX}$""")
    val agencyNameRegex =
        Regex("""^[\p{L}\p{M}\p{N} .,'’()/&+\-]{1,$AGENCY_NAME_MAX}$""")
    val agencyCodeRegex = Regex("""^[A-Z0-9][A-Z0-9_-]{1,${AGENCY_CODE_MAX - 1}}$""")
    val agencyAddressRegex =
        Regex("""^[\p{L}\p{M}\p{N}\p{Zs}.,/'’()#&:+\-]{1,$AGENCY_ADDRESS_MAX}$""")

    fun filterApplicantName(value: String): String =
        value.filter { it.isLetter() || it.isWhitespace() || it in ".,'’()-" }
            .take(APPLICANT_NAME_MAX)

    fun filterWorkUnit(value: String): String =
        value.filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,'’()/&+-" }
            .take(WORK_UNIT_MAX)

    fun filterPhone(value: String): String =
        value.filter(Char::isDigit).take(PHONE_MAX)

    fun filterLetterNumber(value: String): String =
        value.filter { it.isLetterOrDigit() || it.isWhitespace() || it in "./,_:;()'’&+-" }
            .take(LETTER_NUMBER_MAX)

    fun filterDocumentSearch(value: String): String =
        value.filter { it.isLetterOrDigit() || it.isWhitespace() || it in "./,_:;()'’&+-" }
            .take(DOCUMENT_SEARCH_MAX)

    fun filterBypassNote(value: String): String =
        value.filter {
            it.isLetterOrDigit() || it.isWhitespace() ||
                it in ".,;:!?()/'\"’@#&%+_-"
        }.take(BYPASS_NOTE_MAX)

    fun filterExtensionReason(value: String): String =
        filterBypassNote(value).take(EXTENSION_REASON_MAX)

    fun filterAgencyName(value: String): String =
        value.filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,'’()/&+-" }
            .take(AGENCY_NAME_MAX)

    fun filterAgencyCode(value: String): String =
        value.uppercase()
            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .take(AGENCY_CODE_MAX)

    fun filterAgencyAddress(value: String): String =
        value.filter {
            it.isLetterOrDigit() || it.isWhitespace() || it in ".,/'’()#&:+-"
        }.take(AGENCY_ADDRESS_MAX)

    fun validateApplicantName(value: String): String? = when {
        value.isBlank() -> "Nama pemohon wajib diisi"
        !applicantNameRegex.matches(value.trim()) ->
            "Nama maksimal $APPLICANT_NAME_MAX karakter dan hanya boleh berisi huruf serta tanda baca nama"
        else -> null
    }

    fun validateWorkUnit(value: String): String? = when {
        value.isBlank() -> "Unit kerja wajib diisi"
        !workUnitRegex.matches(value.trim()) ->
            "Unit kerja maksimal $WORK_UNIT_MAX karakter dan mengandung karakter tidak valid"
        else -> null
    }

    fun validatePhone(value: String): String? = when {
        value.isBlank() -> "Nomor telepon wajib diisi"
        !phoneRegex.matches(value) ->
            "Nomor telepon harus terdiri dari $PHONE_MIN–$PHONE_MAX angka"
        else -> null
    }

    fun validateLetterNumber(value: String): String? = when {
        value.isBlank() -> "Nomor surat wajib diisi"
        !letterNumberRegex.matches(value.trim()) ->
            "Nomor surat maksimal $LETTER_NUMBER_MAX karakter dan mengandung simbol yang tidak didukung"
        else -> null
    }

    fun validateBypassNote(value: String): String? = when {
        value.isBlank() -> "Catatan bypass wajib diisi"
        !bypassNoteRegex.matches(value.trim()) ->
            "Catatan bypass maksimal $BYPASS_NOTE_MAX karakter dan mengandung karakter tidak valid"
        else -> null
    }

    fun validateExtensionReason(value: String): String? = when {
        value.isBlank() -> "Alasan perpanjangan wajib diisi"
        value.trim().length > EXTENSION_REASON_MAX ->
            "Alasan perpanjangan maksimal $EXTENSION_REASON_MAX karakter"
        else -> null
    }

    fun validateAgencyName(value: String): String? = when {
        value.isBlank() -> "Nama instansi wajib diisi"
        !agencyNameRegex.matches(value.trim()) ->
            "Nama instansi maksimal $AGENCY_NAME_MAX karakter dan mengandung karakter tidak valid"
        else -> null
    }

    fun validateAgencyCode(value: String): String? = when {
        value.isBlank() -> null
        !agencyCodeRegex.matches(value.trim().uppercase()) ->
            "Kode harus 2–$AGENCY_CODE_MAX karakter: huruf besar, angka, - atau _"
        else -> null
    }

    fun validateAgencyAddress(value: String): String? = when {
        value.isBlank() -> null
        !agencyAddressRegex.matches(value.trim()) ->
            "Alamat maksimal $AGENCY_ADDRESS_MAX karakter dan mengandung karakter tidak valid"
        else -> null
    }
}
