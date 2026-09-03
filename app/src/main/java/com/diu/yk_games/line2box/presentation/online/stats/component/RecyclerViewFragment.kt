package com.diu.yk_games.line2box.presentation.online.stats.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diu.yk_games.line2box.presentation.adapter.ScoreListAdapter
import com.diu.yk_games.line2box.util.dpToPx

class RecyclerViewFragment : Fragment() {
    var adapter: ScoreListAdapter? = null
    var onCreateViewCallback: ((RecyclerView) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        return RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                val marginInPx = 30.dpToPx
                updateMargins(left = marginInPx, right = marginInPx)
            }
            updatePadding(top = 8.dpToPx)
            clipToPadding = false
            adapter = this@RecyclerViewFragment.adapter
            onCreateViewCallback?.invoke(this)
        }
    }

    companion object {
        fun newInstance(
            listAdapter: ScoreListAdapter,
            onCreateView: (RecyclerView) -> Unit = {}
        ): RecyclerViewFragment {
            return RecyclerViewFragment().apply {
                this.adapter = listAdapter
                this.onCreateViewCallback = onCreateView
            }
        }
    }
}