package com.troy.remotemediatorbugrepro

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import kotlin.math.max
import kotlin.math.min

/**
 * Example of a direct paging source with no cache
 */
class ContentSource(private val backend: ContentApi) : PagingSource<Int, Content>() {

    override fun getRefreshKey(state: PagingState<Int, Content>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Content> {
        Log.v("Source", "load(key ${params.key}, size ${params.loadSize})")
        return try {
            val offset = params.key ?: 1
            val response = backend.getContent(offset, params.loadSize)
            LoadResult.Page(data = response.items, prevKey = response.prevKey, nextKey = response.nextKey)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

/**
 * Example of a remote mediator which caches items to the database
 */
@OptIn(ExperimentalPagingApi::class)
class ContentMediator(private val db: AppDatabase, private val backend: ContentApi) : RemoteMediator<Int, Content>() {

    private var firstIndex = 1
    private var lastIndex = 1

    override suspend fun load(loadType: LoadType, state: PagingState<Int, Content>): MediatorResult {
        Log.v("Mediator", "load(type $loadType, state $state)")
        return try {
            val size = if (loadType == LoadType.REFRESH) state.config.initialLoadSize else state.config.pageSize
            val itemNumber = when (loadType) {
                LoadType.REFRESH -> state.lastItemOrNull()?.cid ?: 1
                LoadType.PREPEND -> (firstIndex - size).takeIf { it > 0 } ?: return MediatorResult.Success(endOfPaginationReached = true).also {
                    Log.v("Mediator", "returned $it endOfPaginationReached true")
                }
                LoadType.APPEND -> lastIndex
            }
            val response = backend.getContent(itemNumber, size)
            Log.v("Mediator", "loaded $size items")
            val nextNumber = itemNumber + response.items.size
            if (loadType == LoadType.REFRESH) {
                firstIndex = itemNumber
                lastIndex = nextNumber
            } else {
                firstIndex = min(firstIndex, itemNumber)
                lastIndex = max(lastIndex, nextNumber)
            }

            db.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    db.contentDao.deleteAll()
                }
                db.contentDao.insertAll(*response.items.toTypedArray())
            }

            val endOfPaginationReached = response.items.isEmpty()
            MediatorResult.Success(endOfPaginationReached).also {
                Log.v("Mediator", "returned $it, endOfPaginationReached $endOfPaginationReached")
            }
        } catch (e: Exception) {
            MediatorResult.Error(e).also {
                Log.v("Mediator", "returned $it")
            }
        }
    }
}
