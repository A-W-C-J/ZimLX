/*
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.zimmob.zimlx.smartspace

import android.os.Handler
import android.text.TextUtils
import android.view.View
import androidx.annotation.Keep
import com.android.launcher3.LauncherModel
import com.android.launcher3.R
import org.zimmob.zimlx.loadSmallIcon
import org.zimmob.zimlx.smartspace.ZimSmartspaceController.CardData
import org.zimmob.zimlx.smartspace.ZimSmartspaceController.Line
import org.zimmob.zimlx.toBitmap

@Keep
class NowPlayingProvider(controller: ZimSmartspaceController) :
        ZimSmartspaceController.NotificationBasedDataProvider(controller) {

    private val media = MediaListener(context, this::reload, Handler(LauncherModel.getUiWorkerLooper()))
    private val defaultIcon = context.getDrawable(R.drawable.ic_music_note)!!.toBitmap()!!

    override fun waitForSetup() {
        super.waitForSetup()

        media.onResume()
    }

    private fun getEventCard(): CardData? {
        val tracking = media.tracking ?: return null
        val title = tracking.info.title ?: return null

        val sbn = tracking.sbn
        val icon = sbn?.loadSmallIcon(context)?.toBitmap() ?: defaultIcon

        val mediaInfo = tracking.info
        val lines = mutableListOf<Line>()
        lines.add(Line(title))
        if (!TextUtils.isEmpty(mediaInfo.artist)) {
            lines.add(Line(mediaInfo.artist.toString()))
        } else if (sbn != null) {
            lines.add(Line(getApp(sbn).toString()))
        } else {
            lines.add(Line(getApp(tracking.packageName)))
        }
        val intent = sbn?.notification?.contentIntent
        return if (intent != null) {
            CardData(icon, lines, intent, true)
        } else {
            CardData(icon, lines, View.OnClickListener {
                media.toggle(true)
            }, true)
        }
    }

    private fun reload() {
        updateData(null, getEventCard())
    }

    override fun onDestroy() {
        super.onDestroy()
        media.onPause()
    }
}
