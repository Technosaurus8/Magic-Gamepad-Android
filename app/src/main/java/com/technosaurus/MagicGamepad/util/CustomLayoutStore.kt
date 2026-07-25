package com.technosaurus.MagicGamepad.util

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object CustomLayoutStore {

    const val EXTRA_LAYOUT_ID = "layout_id"
    const val ELEMENT_COUNT = 21
    const val MAX_NAME_LENGTH = 32

    private const val KEY_PROFILE_IDS = "custom_layout_profile_ids"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(RemoteLayoutPrefs.PREFERENCES_FILE, Context.MODE_PRIVATE)

    private fun profileKey(id: String, suffix: String): String = "custom_layout_${id}_$suffix"

    private fun ensureBootstrap(context: Context) {
        val p = prefs(context)
        if (!p.getString(KEY_PROFILE_IDS, "").isNullOrEmpty()) return
        val id = UUID.randomUUID().toString()
        val empty = blankProfileData()
        p.edit()
            .putString(KEY_PROFILE_IDS, id)
            .putString(profileKey(id, "name"), "Layout 1")
            .putString(profileKey(id, "positions"), serializeIntPairs(empty.positions))
            .putString(profileKey(id, "sizes"), serializeIntPairs(empty.sizes))
            .putString(profileKey(id, "isHidden"), serializeBooleanArray(empty.hiddenStates))
            .apply()
    }

    private fun blankProfileData(): CustomLayoutProfile {
        val positions = Array(ELEMENT_COUNT) { intArrayOf(0, 0) }
        val sizes = Array(ELEMENT_COUNT) { intArrayOf(0, 0) }
        val hiddenStates = BooleanArray(ELEMENT_COUNT) { true }
        return CustomLayoutProfile("", "", positions, sizes, hiddenStates)
    }

    private fun readProfileIds(p: SharedPreferences): MutableList<String> {
        val raw = p.getString(KEY_PROFILE_IDS, "") ?: ""
        if (raw.isEmpty()) return mutableListOf()
        return raw.split(",").filter { it.isNotEmpty() }.toMutableList()
    }

    private fun writeProfileIds(editor: SharedPreferences.Editor, ids: List<String>) {
        editor.putString(KEY_PROFILE_IDS, ids.joinToString(","))
    }

    @JvmStatic
    fun getProfiles(context: Context): List<CustomLayoutProfile> {
        ensureBootstrap(context)
        val p = prefs(context)
        return readProfileIds(p).mapNotNull { loadProfile(context, it) }
    }

    @JvmStatic
    fun loadProfile(context: Context, id: String): CustomLayoutProfile? {
        ensureBootstrap(context)
        val p = prefs(context)
        if (!profileExists(p, id)) return null
        val name = p.getString(profileKey(id, "name"), "") ?: ""
        return CustomLayoutProfile(
            id = id,
            name = name,
            positions = loadPositions(context, id),
            sizes = loadSizes(context, id),
            hiddenStates = loadIsHidden(context, id),
        )
    }

    @JvmStatic
    fun createProfile(context: Context, name: String): CustomLayoutProfile? {
        ensureBootstrap(context)
        val trimmed = sanitizeName(name) ?: return null
        val p = prefs(context)
        val blank = blankProfileData()
        val id = UUID.randomUUID().toString()
        val ids = readProfileIds(p)
        ids.add(id)
        p.edit()
            .putString(profileKey(id, "name"), trimmed)
            .putString(profileKey(id, "positions"), serializeIntPairs(blank.positions))
            .putString(profileKey(id, "sizes"), serializeIntPairs(blank.sizes))
            .putString(profileKey(id, "isHidden"), serializeBooleanArray(blank.hiddenStates))
            .also { writeProfileIds(it, ids) }
            .apply()
        return loadProfile(context, id)
    }

    @JvmStatic
    fun renameProfile(context: Context, id: String, name: String): Boolean {
        ensureBootstrap(context)
        val trimmed = sanitizeName(name) ?: return false
        val p = prefs(context)
        if (!profileExists(p, id)) return false
        p.edit().putString(profileKey(id, "name"), trimmed).apply()
        return true
    }

    @JvmStatic
    fun deleteProfile(context: Context, id: String): Boolean {
        ensureBootstrap(context)
        val p = prefs(context)
        val ids = readProfileIds(p)
        if (ids.size <= 1 || !ids.contains(id)) return false
        ids.remove(id)
        val editor = p.edit()
        writeProfileIds(editor, ids)
        editor.remove(profileKey(id, "name"))
        editor.remove(profileKey(id, "positions"))
        editor.remove(profileKey(id, "sizes"))
        editor.remove(profileKey(id, "isHidden"))
        editor.apply()
        return true
    }

    @JvmStatic
    fun loadPositions(context: Context, profileId: String): Array<IntArray> {
        ensureBootstrap(context)
        return deserializeIntPairs(
            prefs(context).getString(profileKey(profileId, "positions"), ""),
        )
    }

    @JvmStatic
    fun loadSizes(context: Context, profileId: String): Array<IntArray> {
        ensureBootstrap(context)
        return deserializeIntPairs(
            prefs(context).getString(profileKey(profileId, "sizes"), ""),
        )
    }

    @JvmStatic
    fun loadIsHidden(context: Context, profileId: String): BooleanArray {
        ensureBootstrap(context)
        return deserializeBooleanArray(
            prefs(context).getString(profileKey(profileId, "isHidden"), null),
        )
    }

    @JvmStatic
    fun savePositions(context: Context, profileId: String, positions: Array<IntArray>) {
        prefs(context).edit()
            .putString(profileKey(profileId, "positions"), serializeIntPairs(positions))
            .apply()
    }

    @JvmStatic
    fun saveSizes(context: Context, profileId: String, sizes: Array<IntArray>) {
        prefs(context).edit()
            .putString(profileKey(profileId, "sizes"), serializeIntPairs(sizes))
            .apply()
    }

    @JvmStatic
    fun saveIsHidden(context: Context, profileId: String, isHidden: BooleanArray) {
        prefs(context).edit()
            .putString(profileKey(profileId, "isHidden"), serializeBooleanArray(isHidden))
            .apply()
    }

    @JvmStatic
    fun saveProfile(context: Context, profile: CustomLayoutProfile) {
        val p = prefs(context)
        if (!profileExists(p, profile.id)) return
        p.edit()
            .putString(profileKey(profile.id, "name"), profile.name)
            .putString(profileKey(profile.id, "positions"), serializeIntPairs(profile.positions))
            .putString(profileKey(profile.id, "sizes"), serializeIntPairs(profile.sizes))
            .putString(profileKey(profile.id, "isHidden"), serializeBooleanArray(profile.hiddenStates))
            .apply()
    }

    private fun profileExists(p: SharedPreferences, id: String): Boolean =
        readProfileIds(p).contains(id)

    private fun sanitizeName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_NAME_LENGTH) return null
        return trimmed
    }

    private fun serializeIntPairs(data: Array<IntArray>): String {
        return buildString {
            data.forEachIndexed { i, pair ->
                append(pair[0]).append(',').append(pair[1])
                if (i < data.lastIndex) append(';')
            }
        }
    }

    private fun deserializeIntPairs(raw: String?): Array<IntArray> {
        val data = Array(ELEMENT_COUNT) { intArrayOf(0, 0) }
        if (raw.isNullOrEmpty()) return data
        val entries = raw.split(";")
        for (i in entries.indices) {
            if (i >= ELEMENT_COUNT) break
            val parts = entries[i].split(",")
            if (parts.size >= 2) {
                data[i][0] = parts[0].toInt()
                data[i][1] = parts[1].toInt()
            }
        }
        return data
    }

    private fun serializeBooleanArray(array: BooleanArray): String {
        return array.joinToString(",") { if (it) "1" else "0" }
    }

    private fun deserializeBooleanArray(raw: String?): BooleanArray {
        val def = BooleanArray(ELEMENT_COUNT) { true }
        if (raw == null) return def
        val array = BooleanArray(ELEMENT_COUNT) { true }
        val parts = raw.split(",")
        for (i in parts.indices) {
            if (i >= ELEMENT_COUNT) break
            array[i] = parts[i] == "1"
        }
        return array
    }
}
