
package com.cristianmg.newplayerivoox.player.ads

import android.net.Uri

import com.cristianmg.newplayerivoox.player.ads.model.VastBannerManifest
import com.google.android.exoplayer2.upstream.ParsingLoadable

import java.io.IOException
import java.io.InputStream

class IvooxAdsManifestParser : ParsingLoadable.Parser<VastBannerManifest> {

    @Throws(IOException::class)
    override fun parse(uri: Uri, inputStream: InputStream): VastBannerManifest {
        return VastBannerManifest(Uri.EMPTY)
    }

}
