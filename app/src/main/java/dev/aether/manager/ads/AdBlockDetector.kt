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
 * Detects ad-blocking setups:
 *  1. VPN active  — TRANSPORT_VPN capability atau tun/ppp interface
 *  2. AdGuard Private DNS — baca LinkProperties.privateDnsServerName
 *  3. AdGuard DNS probe — resolve domain iklan & cek apakah diblock (0.0.0.0 / NXDOMAIN)
 */
object AdBlockDetector {

    private const val TAG = "AdBlockDetector"

    // Domain iklan sungguhan yang AdGuard DNS pasti blokir
    // Normal DNS → resolve ke IP valid; AdGuard DNS → 0.0.0.0 atau NXDOMAIN
    private val AD_PROBE_DOMAINS = listOf(
        "pagead2.googlesyndication.com",
        "ads.google.com",
        "googleadservices.com"
    )

    // Keyword hostname Private DNS yang menunjukkan AdGuard
    private val ADGUARD_DNS_KEYWORDS = listOf(
        "adguard", "dns.adguard", "unfiltered.adguard"
    )

    data class DetectionResult(
        val vpnActive: Boolean,
        val adguardDns: Boolean,
    ) {
        val isBlocking: Boolean get() = vpnActive || adguardDns
    }

    suspend fun detect(context: Context): DetectionResult = withContext(Dispatchers.IO) {
        val vpn     = isVpnActive(context)
        val adguard = isAdGuardDns(context)
        Log.d(TAG, "vpn=$vpn  adguardDns=$adguard")
        DetectionResult(vpnActive = vpn, adguardDns = adguard)
    }

    // ── 1. VPN detection ─────────────────────────────────────────────────

    private fun isVpnActive(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Cek SEMUA network aktif, bukan hanya activeNetwork
        // AdGuard app kadang bukan activeNetwork tapi tetap intercept traffic
        cm.allNetworks.forEach { net ->
            val caps = cm.getNetworkCapabilities(net) ?: return@forEach
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                Log.d(TAG, "VPN detected via TRANSPORT_VPN")
                return true
            }
        }

        // Fallback: scan network interface tun/ppp/tap
        return try {
            NetworkInterface.getNetworkInterfaces()
                ?.toList()
                ?.any { iface ->
                    iface.isUp && !iface.isLoopback &&
                        (iface.name.startsWith("tun") ||
                         iface.name.startsWith("ppp") ||
                         iface.name.startsWith("tap") ||
                         iface.name.startsWith("vpn"))
                } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Interface scan failed: ${e.message}")
            false
        }
    }

    // ── 2. AdGuard DNS detection ─────────────────────────────────────────
    //
    // KENAPA logika lama salah:
    //   InetAddress.getAllByName("dns.adguard.com") SELALU return 94.140.14.14
    //   karena itu memang A-record domain tersebut di DNS manapun (Google, ISP, dll).
    //   Tidak ada hubungannya dengan DNS yang sedang dipakai device.
    //
    // Cara benar:
    //   A) Baca LinkProperties.privateDnsServerName → nama server Private DNS yg dikonfigurasi
    //   B) Probe domain iklan → jika resolve ke 0.0.0.0 berarti diblokir AdGuard

    private fun isAdGuardDns(context: Context): Boolean {
        if (checkPrivateDnsHostname(context)) return true
        return probeAdDomain()
    }

    private fun checkPrivateDnsHostname(context: Context): Boolean {
        return try {
            val cm      = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val props   = cm.getLinkProperties(network) ?: return false

            // privateDnsServerName = hostname di setting "Private DNS" Android 9+
            // null berarti off atau auto (pakai ISP DNS)
            val hostname = props.privateDnsServerName
            Log.d(TAG, "Private DNS hostname: $hostname")

            if (hostname.isNullOrBlank()) return false
            ADGUARD_DNS_KEYWORDS.any { kw -> hostname.contains(kw, ignoreCase = true) }
        } catch (e: Exception) {
            Log.w(TAG, "Private DNS check failed: ${e.message}")
            false
        }
    }

    private fun probeAdDomain(): Boolean {
        var nxCount = 0
        for (domain in AD_PROBE_DOMAINS) {
            try {
                val ips = InetAddress.getAllByName(domain).map { it.hostAddress ?: "" }
                Log.d(TAG, "Probe $domain → $ips")
                // AdGuard DNS mengembalikan 0.0.0.0 untuk domain yang diblokir
                if (ips.all { it == "0.0.0.0" || it == "::" || it.isBlank() }) {
                    Log.d(TAG, "AdGuard confirmed: $domain blocked (0.0.0.0)")
                    return true
                }
            } catch (e: Exception) {
                // UnknownHostException = NXDOMAIN → domain diblokir DNS
                Log.d(TAG, "Probe $domain → NXDOMAIN (${e.javaClass.simpleName})")
                nxCount++
            }
        }
        // Jika mayoritas probe NXDOMAIN, kemungkinan besar AdGuard aktif
        return nxCount >= 2
    }
}
