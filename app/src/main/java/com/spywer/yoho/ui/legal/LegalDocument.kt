package com.spywer.yoho.ui.legal

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.spywer.yoho.R

enum class LegalDocument(
    val extraValue: String,
    @StringRes val titleRes: Int,
    @RawRes val bodyRes: Int,
) {
    Terms("terms", R.string.legal_terms_title, R.raw.legal_terms),
    Privacy("privacy", R.string.legal_privacy_title, R.raw.legal_privacy),
    Dmca("dmca", R.string.legal_dmca_title, R.raw.legal_dmca),
    ;

    companion object {
        fun fromExtra(value: String?): LegalDocument =
            entries.firstOrNull { it.extraValue.equals(value, ignoreCase = true) } ?: Terms
    }
}
