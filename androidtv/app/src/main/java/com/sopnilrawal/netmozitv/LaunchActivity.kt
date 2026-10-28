package com.sopnilrawal.netmozitv

import android.app.Activity
import android.os.Bundle

/** Target of home screen tiles and global search results: opens the title in Kodi. */
class LaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.data?.getQueryParameter("path")?.let { Kodi.open(this, it) }
        finish()
    }
}
