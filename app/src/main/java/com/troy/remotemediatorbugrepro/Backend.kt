package com.troy.remotemediatorbugrepro

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * This is a simulated backend API
 */
object ContentApi {

    private const val FULL_ALPHA = 0xff000000.toInt()
    private const val MAX_ITEMS = 500

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun getContent(itemNumber: Int, size: Int): Response {
        require(itemNumber > 0) { "itemNumber of 0 or less is illegal" }
        delay((100..1000).random().toLong())    // network communications overhead
        Log.v("Api", "getContent(itemNumber $itemNumber, size $size)")
        var index = itemNumber
        val end = itemNumber + size
        val items = mutableListOf<Content>()
        while (index < end && index <= MAX_ITEMS) {
            items.add(Content(index, "Item $index", "This is some text content for item $index", Random.nextInt() or FULL_ALPHA))
            index++
        }
        val prev = when  {
            itemNumber <= 1 -> null
            itemNumber <= size -> 1
            else -> itemNumber - size
        }
        val next = when  {
            end < MAX_ITEMS -> end
            else -> null
        }
        return Response(prev, next, items)
    }

    class Response(
        val prevKey: Int?,
        val nextKey: Int?,
        val items: List<Content>
    )
}
