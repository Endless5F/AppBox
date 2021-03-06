package com.android.core.cache

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Exception

/**
 *
 * @author jiaochengyun
 * @version
 * @since 2021/10/17
 */
object HiCache {

    fun <T> saveCache(key: String, body: T) {
        val cache = Cache()
        cache.key = key
        cache.data = toByteArray(body)
        CacheDatabase.get().cacheDao.saveCache(cache)
    }

    fun <T> getCache(key: String): T? {
        val cache = CacheDatabase.get().cacheDao.getCache(key)
        return (if (cache?.data != null) toObject(cache.data) else null) as? T
    }

    fun deleteCache(key: String) {
        val cache = Cache()
        cache.key = key
        CacheDatabase.get().cacheDao.deleteCache(cache)
    }

    private fun <T> toByteArray(body: T): ByteArray? {
        var baos: ByteArrayOutputStream? = null
        var oos: ObjectOutputStream? = null
        try {
            baos = ByteArrayOutputStream()
            oos = ObjectOutputStream(baos)
            oos.writeObject(body)
            oos.flush()
            return baos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            baos?.close()
            oos?.close()
        }

        return ByteArray(0)
    }

    private fun toObject(data: ByteArray?): Any? {
        var bais: ByteArrayInputStream? = null
        var ois: ObjectInputStream? = null
        try {
            bais = ByteArrayInputStream(data)
            ois = ObjectInputStream(bais)
            return ois.readObject()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bais?.close()
            ois?.close()
        }

        return null
    }

}