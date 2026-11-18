package com.sopnilrawal.netmozitv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Publishes the "NetMozi Discover" row of tiles on the Android TV home screen. */
object ChannelSync {
    private const val CHANNEL_KEY = "netmozi_discover"

    fun schedule(ctx: Context) {
        val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val wm = WorkManager.getInstance(ctx)
        wm.enqueueUniqueWork(
            "channel-now", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(network).build(),
        )
        wm.enqueueUniquePeriodicWork(
            "channel", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).setConstraints(network).build(),
        )
    }

    fun sync(ctx: Context) {
        Prefs(ctx).applyToTmdb()
        val helper = PreviewChannelHelper(ctx)
        val channelId = helper.allChannels.firstOrNull { it.internalProviderId == CHANNEL_KEY }?.id
            ?: createChannel(ctx, helper)

        val titles = if (Tmdb.hasKey) {
            Tmdb.trending(Tmdb.MOVIE).take(12) + runCatching { Tmdb.discover(Tmdb.MOVIE, language = "hi").take(8) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        ctx.contentResolver.delete(TvContractCompat.buildPreviewProgramsUriForChannel(channelId), null, null)
        helper.publishPreviewProgram(
            tile(channelId, ctx.getString(R.string.voice_search), resUri(ctx, R.drawable.tile_voice), Uri.parse("netmozitv://search?voice=1"))
        )
        helper.publishPreviewProgram(
            tile(channelId, ctx.getString(R.string.search), resUri(ctx, R.drawable.tile_search), Uri.parse("netmozitv://search"))
        )
        for (m in titles.distinctBy { it.key }) {
            val poster = m.posterUrl ?: continue
            helper.publishPreviewProgram(
                tile(channelId, m.title, Uri.parse(poster), Uri.parse("netmozitv://title?type=${m.type}&id=${m.id}"), m.year)
            )
        }
    }

    private fun createChannel(ctx: Context, helper: PreviewChannelHelper): Long {
        val channel = PreviewChannel.Builder()
            .setDisplayName(ctx.getString(R.string.channel_name))
            .setAppLinkIntentUri(Uri.parse("netmozitv://home"))
            .setInternalProviderId(CHANNEL_KEY)
            .setLogo(BitmapFactory.decodeResource(ctx.resources, R.drawable.logo))
            .build()
        val id = helper.publishChannel(channel)
        // Our first channel is shown on the home screen without asking the user.
        runCatching { TvContractCompat.requestChannelBrowsable(ctx, id) }
        return id
    }

    private fun tile(channelId: Long, title: String, poster: Uri, intent: Uri, description: String = ""): PreviewProgram =
        PreviewProgram.Builder()
            .setChannelId(channelId)
            .setType(TvContractCompat.PreviewProgramColumns.TYPE_MOVIE)
            .setTitle(title)
            .setDescription(description)
            .setPosterArtUri(poster)
            .setPosterArtAspectRatio(TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_MOVIE_POSTER)
            .setIntentUri(intent)
            .build()

    private fun resUri(ctx: Context, res: Int): Uri =
        Uri.parse("android.resource://${ctx.packageName}/${ctx.resources.getResourceTypeName(res)}/${ctx.resources.getResourceEntryName(res)}")
}

class SyncWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result = try {
        ChannelSync.sync(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}

/** The launcher asks new apps for their channels, and channels are refreshed after a reboot. */
class ChannelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ChannelSync.schedule(context)
    }
}
