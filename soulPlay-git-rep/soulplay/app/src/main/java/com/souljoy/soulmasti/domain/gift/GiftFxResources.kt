package com.souljoy.soulmasti.domain.gift

import android.content.Context
import java.util.Locale

/**
 * Per-gift full-screen send effects. Lottie files live in `res/raw/` as:
 * **`gift_fx_<slug>.json`**
 *
 * `<slug>` is derived from [GiftCatalog] ids: lowercased, non-alphanumeric → `_`.
 * Legacy id `"dream palace"` maps to the same animation as [GiftCatalog.DRAGON] (`gift_fx_dragon`).
 */
object GiftFxResources {

    fun slugForRaw(giftId: String): String =
        giftId.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "unknown" }

    /** `R.raw` id or `0` if missing. */
    fun lottieRawRes(context: Context, giftId: String): Int {
        val slug = slugForRaw(giftId)
        var id = context.resources.getIdentifier("gift_fx_$slug", "raw", context.packageName)
        if (id == 0 && slug == "dream_palace") {
            id = context.resources.getIdentifier("gift_fx_dragon", "raw", context.packageName)
        }
        return id
    }

    fun hasAnyFx(context: Context, giftId: String): Boolean =
        lottieRawRes(context, giftId) != 0
}
