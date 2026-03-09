package com.pavel.pavelrssreader.data.network

import okhttp3.Dns
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException

/**
 * DNS resolver that falls back to Cloudflare's DNS-over-HTTPS (1.1.1.1) when the
 * system DNS fails. Querying 1.1.1.1 via its IP address requires no DNS lookup,
 * so it works even when the device's DNS resolver is broken for specific domains.
 */
class FallbackDns : Dns {

    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            Dns.SYSTEM.lookup(hostname)
        } catch (e: UnknownHostException) {
            queryDoH(hostname).ifEmpty { throw e }
        }
    }

    private fun queryDoH(hostname: String): List<InetAddress> {
        return try {
            val conn = URL("https://1.1.1.1/dns-query?name=$hostname&type=A")
                .openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/dns-json")
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            // Extract IPv4 addresses from "data":"x.x.x.x" fields in the Answer section
            Regex(""""data"\s*:\s*"([0-9]{1,3}(?:\.[0-9]{1,3}){3})"""")
                .findAll(body)
                .mapNotNull { runCatching { InetAddress.getByName(it.groupValues[1]) }.getOrNull() }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
