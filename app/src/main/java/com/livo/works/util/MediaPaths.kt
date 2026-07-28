package com.livo.works.util

/**
 * Structural marker present in every Supabase public-object URL, sitting
 * immediately before the bucket segment:
 *
 *     https://<project>.supabase.co/storage/v1/object/public/<bucket>/<path>
 *                                             ^^^^^^^^^^^^^^^^
 */
private const val PUBLIC_OBJECT_MARKER = "/object/public/"

/**
 * Converts a photo reference into the relative storage path the backend stores
 * in its database.
 *
 * GET endpoints return fully resolved CDN URLs (needed as-is to render the
 * image), but POST/PUT reject them with 403 and require the relative path back.
 * The split is done on the URL's structure rather than a hardcoded host or
 * bucket name, so it keeps working across environments and bucket renames.
 *
 * Strings that are already relative - newly uploaded "temp/..." paths, or paths
 * that never carried a host - are returned unchanged.
 */
fun String.toRelativeMediaPath(): String {
    val markerStart = indexOf(PUBLIC_OBJECT_MARKER)
    if (markerStart == -1) return this

    return substring(markerStart + PUBLIC_OBJECT_MARKER.length)
        .substringAfter('/') // drop the bucket segment
}
