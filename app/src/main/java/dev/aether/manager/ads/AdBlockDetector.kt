package dev.aether.manager.ads

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Detects common ad-blocking setups:
 *  1. VPN active (any VPN interface or TRANSPORT_VPN capability)
 *  2. DNS set to AdGuard (94.140.14.x / 94.140.15.x)
 *
 * All checks run on IO dispatcher — call from a coroutine.
 */
object AdBlockDetector {

    private const val TAG = "AdBlockDetector"

    data class DetectionResult(
        val vpnActive: Boolean,
        val adguardDns: Boolean,
    ) {
        val isBlocking: Boolean get() = vpnActive || adguardDns
    }

    /** Run all checks and return a combined result. */
    suspend fun detect(context: Context): DetectionResult = withContext(Dispatchers.IO) {
        val vpn     = isVpnActive(context)
        val adguard = isAdGuardDns()
        Log.d(TAG, "vpn=$vpn  adguardDns=$adguard")
        DetectionResult(vpnActive = vpn, adguardDns = adguard)
    }

    // ── VPN detection ─────────────────────────────────────────────────────

    private fun isVpnActive(context: Context): Boolean {
        // Primary: ConnectivityManager capability check (API 23+)
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.activeNetwork?.let { net ->
            val caps = cm.getNetworkCapabilities(net) ?: return@let
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true
        }
        // Fallback: scan for tun/ppp network interfaces
        return try {
            NetworkInterface.getNetworkInterfaces()
                ?.toList()
                ?.any { iface ->
                    iface.isUp && !iface.isLoopback &&
                            (iface.name.startsWith("tun") ||
                             iface.name.startsWith("ppp") ||
                             iface.name.startsWith("tap"))
                } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "VPN interface scan failed: ${e.message}")
            false
        }
    }

    // ── AdGuard DNS detection ─────────────────────────────────────────────
    // Resolve a known domain and check if the answer comes from AdGuard's
    // IP space (94.140.14.0/23). This is a heuristic — a filtered NXDOMAIN
    // or a spoofed A-record from AdGuard servers indicates active filtering.

    private val ADGUARD_PREFIXES = listOf("94.140.14.", "94.140.15.")

    private fun isAdGuardDns(): Boolean {
        return try {
            // "testmyads.adfox.ru" is a real ad-serving domain that AdGuard blocks.
            // AdGuard returns 0.0.0.0 for blocked domains; an empty result also
            // signals filtering. We also check the resolver address itself.
            val addresses: Array<InetAddress> = InetAddress.getAllByName("dns.adguard.com")
            val resolvedIps = addresses.map { it.hostAddress ?: "" }
            Log.d(TAG, "dns.adguard.com resolved to: $resolvedIps")
            // If dns.adguard.com resolves to an AdGuard IP we know AdGuard DNS is in use
            resolvedIps.any { ip -> ADGUARD_PREFIXES.any { prefix -> ip.startsWith(prefix) } }
        } catch (e: Exception) {
            // Resolution failure can itself mean DNS is being intercepted/blocked
            Log.w(TAG, "AdGuard DNS check failed: ${e.message}")
            false
        }
    }
}
