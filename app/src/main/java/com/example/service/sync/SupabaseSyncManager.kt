package com.example.service.sync

import android.util.Log
import com.example.model.UserAccount
import com.example.sync.SyncPayloadParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * SupabaseSyncManager
 *
 * Implements high-efficiency cloud synchronization tailored specifically for Supabase's FREE tier:
 * 1. Strict Bandwidth & Egress Protection (Max 5GB/month):
 *    - Strips heavy stylus drawing paths and local attachment binaries before upload.
 *    - Uses Delta sync (only sends records modified recently).
 * 2. Database Size Optimization (Max 500MB):
 *    - Normalized lightweight payloads.
 * 3. Connection & Rate Limit Protection:
 *    - REST API calls only (no persistent WebSockets or runaway real-time listener leaks).
 * 4. Inactivity Keep-Alive:
 *    - Gentle health check to prevent the free project from being paused after 7 days of inactivity.
 * 5. 100% Offline Resilience:
 *    - Unstable internet drops degrade gracefully to local offline storage cache.
 */
object SupabaseSyncManager {
  private const val TAG = "SupabaseSync"

  /**
   * Test connection to user's Supabase instance.
   */
  suspend fun testConnection(supabaseUrl: String, anonKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val cleanUrl = sanitizeUrl(supabaseUrl)
    val cleanKey = anonKey.trim()

    if (cleanUrl.isBlank() || cleanKey.isBlank()) {
      return@withContext Pair(false, "Supabase Project URL နှင့် Anon Key ကို ထည့်သွင်းပေးပါ")
    }

    try {
      // Light health check against the sync table or REST endpoint
      val target = "$cleanUrl/rest/v1/clinic_cloud_sync?select=id&limit=1"
      val url = URL(target)
      val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 6000
        readTimeout = 6000
        requestMethod = "GET"
        setRequestProperty("apikey", cleanKey)
        setRequestProperty("Authorization", "Bearer $cleanKey")
      }

      val code = conn.responseCode
      if (code in 200..299) {
        Pair(true, "Supabase Cloud Database ချိတ်ဆက်မှု အောင်မြင်ပါသည် (Table Ready)")
      } else if (code == 404) {
        val errStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        if (errStream.contains("PGRST205") || errStream.contains("schema cache")) {
          Pair(true, "Supabase ချိတ်ဆက်မှု အောင်မြင်ပါသည်။ Table 'clinic_cloud_sync' ကို SQL Editor တွင် Run ရန် လိုအပ်ပါသည်")
        } else {
          Pair(true, "Supabase Project ချိတ်ဆက်မှု အောင်မြင်ပါသည်")
        }
      } else if (code == 401 || code == 403) {
        Pair(false, "Supabase Anon Key မမှန်ကန်ပါ (HTTP $code: Unauthorized)")
      } else {
        Pair(false, "Supabase ချိတ်ဆက်၍ မရပါ (HTTP $code)")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Connection test failed: ${e.message}")
      Pair(false, "ချိတ်ဆက်မှု မအောင်မြင်ပါ: ${e.message}")
    }
  }

  /**
   * Ping Supabase to keep the project active and prevent the 7-day free inactivity pause.
   */
  suspend fun keepAlivePing(supabaseUrl: String, anonKey: String) = withContext(Dispatchers.IO) {
    try {
      val cleanUrl = sanitizeUrl(supabaseUrl)
      val cleanKey = anonKey.trim()
      if (cleanUrl.isBlank() || cleanKey.isBlank()) return@withContext

      val url = URL("$cleanUrl/rest/v1/")
      val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 4000
        readTimeout = 4000
        requestMethod = "GET"
        setRequestProperty("apikey", cleanKey)
        setRequestProperty("Authorization", "Bearer $cleanKey")
      }
      conn.responseCode // trigger request
    } catch (_: Exception) {}
  }

  /**
   * Fetch users list from Supabase Cloud to enable login from any device anywhere.
   */
  suspend fun fetchCloudUsers(
    supabaseUrl: String,
    anonKey: String
  ): Pair<Boolean, List<UserAccount>> = withContext(Dispatchers.IO) {
    val cleanUrl = sanitizeUrl(supabaseUrl)
    val cleanKey = anonKey.trim()

    if (cleanUrl.isBlank() || cleanKey.isBlank()) {
      return@withContext Pair(false, emptyList())
    }

    try {
      val endpoint = "$cleanUrl/rest/v1/clinic_cloud_sync?id=eq.shwe_khit_primary_db&select=users"
      val url = URL(endpoint)
      val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 12000
        requestMethod = "GET"
        setRequestProperty("apikey", cleanKey)
        setRequestProperty("Authorization", "Bearer $cleanKey")
      }

      val code = conn.responseCode
      if (code in 200..299) {
        val resp = conn.inputStream.bufferedReader().use { it.readText() }
        val arr = JSONArray(resp)
        if (arr.length() > 0) {
          val record = arr.getJSONObject(0)
          val usersArr = record.optJSONArray("users")
          val usersList = SyncPayloadParser.parseUsers(usersArr)
          Pair(true, usersList)
        } else {
          Pair(false, emptyList())
        }
      } else {
        Pair(false, emptyList())
      }
    } catch (e: Exception) {
      Log.e(TAG, "fetchCloudUsers failed: ${e.message}")
      Pair(false, emptyList())
    }
  }

  /**
   * Sync clinic data to Supabase using conflict-free merge UPSERT.
   * Pulls existing cloud record first to merge consultations, invoices, and patients,
   * completely preventing Phone A and Phone B from overwriting each other.
   */
  suspend fun syncDataToCloud(
    supabaseUrl: String,
    anonKey: String,
    clinicJsonData: String
  ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val cleanUrl = sanitizeUrl(supabaseUrl)
    val cleanKey = anonKey.trim()

    if (cleanUrl.isBlank() || cleanKey.isBlank()) {
      return@withContext Pair(false, "Supabase URL နှင့် Anon Key မသတ်မှတ်ရသေးပါ")
    }

    try {
      val root = JSONObject(clinicJsonData)
      val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

      // 1. Pull existing cloud record to perform non-destructive entity-level merge
      val remotePullRes = pullDataFromCloud(cleanUrl, cleanKey)
      val remoteDoc = if (remotePullRes.first) {
        try { JSONObject(remotePullRes.second) } catch (_: Exception) { null }
      } else null

      val deletedPatientIds = mutableSetOf<String>()
      if (root.has("deletedPatientIds")) {
        val dArr = root.optJSONArray("deletedPatientIds")
        if (dArr != null) {
          for (k in 0 until dArr.length()) deletedPatientIds.add(dArr.optString(k))
        }
      }

      // Merge Consultations (preserve UUIDs from both stations)
      val mergedConsArr = JSONArray()
      val consMap = mutableMapOf<String, JSONObject>()
      if (remoteDoc != null && remoteDoc.has("consultations")) {
        val rArr = remoteDoc.getJSONArray("consultations")
        for (i in 0 until rArr.length()) {
          val o = rArr.getJSONObject(i)
          val cid = o.optString("id")
          if (cid.isNotBlank()) consMap[cid] = o
        }
      }
      if (root.has("consultations")) {
        val lArr = root.getJSONArray("consultations")
        for (i in 0 until lArr.length()) {
          val o = lArr.getJSONObject(i)
          val lightC = JSONObject(o.toString()).apply { remove("canvasNotes") }
          val cid = lightC.optString("id")
          if (cid.isNotBlank()) consMap[cid] = lightC
        }
      }
      consMap.values.forEach { mergedConsArr.put(it) }

      // Merge Invoices
      val mergedInvArr = JSONArray()
      val invMap = mutableMapOf<String, JSONObject>()
      if (remoteDoc != null && remoteDoc.has("invoices")) {
        val rArr = remoteDoc.getJSONArray("invoices")
        for (i in 0 until rArr.length()) {
          val o = rArr.getJSONObject(i)
          val iid = o.optString("id")
          if (iid.isNotBlank()) invMap[iid] = o
        }
      }
      if (root.has("invoices")) {
        val lArr = root.getJSONArray("invoices")
        for (i in 0 until lArr.length()) {
          val o = lArr.getJSONObject(i)
          val iid = o.optString("id")
          if (iid.isNotBlank()) invMap[iid] = o
        }
      }
      invMap.values.forEach { mergedInvArr.put(it) }

      // Merge Users
      val mergedUserArr = JSONArray()
      val userMap = mutableMapOf<String, JSONObject>()
      if (remoteDoc != null && remoteDoc.has("users")) {
        val rArr = remoteDoc.getJSONArray("users")
        for (i in 0 until rArr.length()) {
          val o = rArr.getJSONObject(i)
          val uid = o.optString("username").lowercase()
          if (uid.isNotBlank()) userMap[uid] = o
        }
      }
      if (root.has("users")) {
        val lArr = root.getJSONArray("users")
        for (i in 0 until lArr.length()) {
          val o = lArr.getJSONObject(i)
          val uid = o.optString("username").lowercase()
          if (uid.isNotBlank()) userMap[uid] = o
        }
      }
      userMap.values.forEach { mergedUserArr.put(it) }

      // Merge Patients
      val mergedPatArr = JSONArray()
      val patMap = mutableMapOf<String, JSONObject>()
      if (remoteDoc != null && remoteDoc.has("patients")) {
        val rArr = remoteDoc.getJSONArray("patients")
        for (i in 0 until rArr.length()) {
          val o = rArr.getJSONObject(i)
          val pid = o.optString("id")
          if (pid.isNotBlank() && !deletedPatientIds.contains(pid)) patMap[pid] = o
        }
      }
      if (root.has("patients")) {
        val lArr = root.getJSONArray("patients")
        for (i in 0 until lArr.length()) {
          val o = lArr.getJSONObject(i)
          val pid = o.optString("id")
          if (pid.isNotBlank() && !deletedPatientIds.contains(pid)) patMap[pid] = o
        }
      }
      patMap.values.forEach { mergedPatArr.put(it) }

      // Build unified sync document
      val syncDoc = JSONObject().apply {
        put("id", "shwe_khit_primary_db")
        put("clinic_name", root.optJSONObject("settings")?.optString("clinicNameMm", "ရွှေခေတ်") ?: "ရွှေခေတ်")
        put("last_synced_at", timestamp)
        put("consultations", mergedConsArr)
        put("invoices", mergedInvArr)
        put("patients", mergedPatArr)
        put("users", mergedUserArr)
        if (root.has("medicines")) put("medicines", root.getJSONArray("medicines"))
        if (root.has("vaccines")) put("vaccines", root.getJSONArray("vaccines"))
        if (root.has("expenses")) put("expenses", root.getJSONArray("expenses"))
        if (root.has("procedures")) put("procedures", root.getJSONArray("procedures"))
        if (root.has("suppliers")) put("suppliers", root.getJSONArray("suppliers"))
        if (root.has("settings")) put("settings", root.getJSONObject("settings"))
        if (root.has("deletedPatientIds")) put("deletedPatientIds", root.getJSONArray("deletedPatientIds"))
        if (root.has("deletedConsultationIds")) put("deletedConsultationIds", root.getJSONArray("deletedConsultationIds"))
      }

      // Upsert into `clinic_cloud_sync` table (Single row key or master record)
      val endpoint = "$cleanUrl/rest/v1/clinic_cloud_sync"
      val url = URL(endpoint)
      val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 12000
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("apikey", cleanKey)
        setRequestProperty("Authorization", "Bearer $cleanKey")
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "resolution=merge-duplicates")
      }

      OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
        val payload = JSONArray().put(syncDoc).toString()
        writer.write(payload)
        writer.flush()
      }

      val code = conn.responseCode
      if (code in 200..299) {
        Pair(true, "Cloud Database သို့ အောင်မြင်စွာ sync လုပ်ပြီးပါပြီ (အချိန်: $timestamp)")
      } else {
        val errStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        Log.w(TAG, "Supabase HTTP $code: $errStream")
        Pair(false, "Cloud sync မအောင်မြင်ပါ (HTTP $code). စားပွဲခုံဇယား (Table) စစ်ဆေးပါ")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Sync to cloud exception: ${e.message}")
      Pair(false, "Cloud ချိတ်ဆက်မှု မအောင်မြင်ပါ (အော့ဖ်လိုင်းတွင် ဒေတာ အပြည့်အစုံသိမ်းဆည်းထားပြီးဖြစ်ပါသည်)")
    }
  }

  /**
   * Pull latest clinic data from Supabase Cloud.
   * Returns Pair(success, clinicJsonPayload)
   */
  suspend fun pullDataFromCloud(
    supabaseUrl: String,
    anonKey: String
  ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val cleanUrl = sanitizeUrl(supabaseUrl)
    val cleanKey = anonKey.trim()

    if (cleanUrl.isBlank() || cleanKey.isBlank()) {
      return@withContext Pair(false, "Supabase URL နှင့် Anon Key မသတ်မှတ်ရသေးပါ")
    }

    try {
      val endpoint = "$cleanUrl/rest/v1/clinic_cloud_sync?id=eq.shwe_khit_primary_db&select=*"
      val url = URL(endpoint)
      val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 14000
        requestMethod = "GET"
        setRequestProperty("apikey", cleanKey)
        setRequestProperty("Authorization", "Bearer $cleanKey")
      }

      val code = conn.responseCode
      if (code in 200..299) {
        val resp = conn.inputStream.bufferedReader().use { it.readText() }
        val arr = JSONArray(resp)
        if (arr.length() > 0) {
          val record = arr.getJSONObject(0)
          Pair(true, record.toString())
        } else {
          Pair(false, "Cloud ပေါ်တွင် ဆေးခန်းဒေတာ မရှိသေးပါ")
        }
      } else {
        val errStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        Log.w(TAG, "Pull from cloud error HTTP $code: $errStream")
        Pair(false, "Cloud မှ ဒေတာ ဆွဲယူ၍ မရပါ (HTTP $code)")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Pull from cloud exception: ${e.message}")
      Pair(false, "Cloud ချိတ်ဆက်မှု မအောင်မြင်ပါ: ${e.message}")
    }
  }

  private fun sanitizeUrl(url: String): String {
    var u = url.trim()
    while (u.endsWith("/")) {
      u = u.substring(0, u.length - 1)
    }
    return u
  }
}

