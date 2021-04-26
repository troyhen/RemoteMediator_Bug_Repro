package com.troy.remotemediatorbugrepro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.DifferCallback
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingDataAdapter
import androidx.paging.cachedIn
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var cachedAdapter: ContentAdapter
    private lateinit var directAdapter: ContentAdapter
    private lateinit var direct: ToggleButton
    private lateinit var cached: ToggleButton
    private lateinit var instructions: TextView
    private lateinit var recycler: RecyclerView

    private var job: Job? = null

    private var mode: Mode = Mode.HOME
        set(value) {
            field = value
            instructions.isVisible = value == Mode.HOME
            recycler.isVisible = value != Mode.HOME
            direct.isChecked = value == Mode.DIRECT
            cached.isChecked = value == Mode.CACHED
            update()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        direct = findViewById(R.id.direct)
        cached = findViewById(R.id.cached)
        recycler = findViewById(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        cachedAdapter = ContentAdapter()
        directAdapter = ContentAdapter()
        instructions = findViewById(R.id.instructions)
        direct.setOnClickListener { mode = Mode.DIRECT }
        cached.setOnClickListener { mode = Mode.CACHED }
    }

    private fun update() {
        job?.cancel()
        job = lifecycleScope.launchWhenStarted {
            when (mode) {
                Mode.CACHED -> {
                    val flow = viewModel.cachedPagingDataFlow
                    val adapter = cachedAdapter
                    recycler.adapter = adapter
                    adapter.refresh()
                    flow.collect {
                        adapter.submitData(it)
                    }
                }
                Mode.DIRECT -> {
                    val flow = viewModel.directPagingDataFlow
                    val adapter = directAdapter
                    recycler.adapter = adapter
                    adapter.refresh()
                    flow.collect {
                        adapter.submitData(it)
                    }
                }
            }
        }
    }

    enum class Mode {
        HOME, DIRECT, CACHED,
    }
}

class ContentAdapter : PagingDataAdapter<Content, ContentAdapter.Holder>(Diff) {

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position) ?: return
        val layout = holder.itemView.findViewById<LinearLayout>(R.id.layout)
        val title = holder.itemView.findViewById<TextView>(R.id.title)
        val text = holder.itemView.findViewById<TextView>(R.id.text)
        layout.setBackgroundColor(item.color)
        title.text = item.title
        text.text = item.text
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_content, parent, false)
        return Holder(view)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)

    object Diff : DiffUtil.ItemCallback<Content>() {
        override fun areItemsTheSame(oldItem: Content, newItem: Content) = oldItem.cid == newItem.cid
        override fun areContentsTheSame(oldItem: Content, newItem: Content) = oldItem == newItem
    }
}

class MainViewModel : ViewModel() {

    private val db = App.app.db

    val directPagingDataFlow = Pager(PagingConfig(pageSize = 5, initialLoadSize = 5, enablePlaceholders = false)) {
        ContentSource(App.app.backend)
    }.flow.cachedIn(viewModelScope)

    @OptIn(ExperimentalPagingApi::class)
    val cachedPagingDataFlow = Pager(PagingConfig(pageSize = 5, initialLoadSize = 5, enablePlaceholders = false), remoteMediator = ContentMediator(App.app.db, App.app.backend)) {
        db.contentDao.contentSource()
    }.flow
}
