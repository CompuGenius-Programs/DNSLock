package com.cgprograms.dnslock

import android.content.Context
import android.graphics.drawable.Icon
import android.net.wifi.WifiManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class WifiTileService : TileService() {

    private lateinit var wifiManager: WifiManager

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        toggleWifi()
        updateTile()
    }

    // Toggle Wi-Fi on/off
    private fun toggleWifi() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val status = !wifiManager.isWifiEnabled
        wifiManager.isWifiEnabled = status
        val statusText = if (status) "enabled" else "disabled"
        Toast.makeText(this, "Wi-Fi is now $statusText", Toast.LENGTH_SHORT).show()
    }

    // Update the tile based on the current Wi-Fi state
    private fun updateTile() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val tile = qsTile

        // Check Wi-Fi state and update tile icon and label
        if (wifiManager.isWifiEnabled) {
            tile.state = Tile.STATE_ACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.baseline_wifi_24)
            tile.label = "Wi-Fi ON"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.baseline_wifi_off_24)
            tile.label = "Wi-Fi OFF"
        }

        tile.updateTile() // Refresh the tile UI
    }
}
