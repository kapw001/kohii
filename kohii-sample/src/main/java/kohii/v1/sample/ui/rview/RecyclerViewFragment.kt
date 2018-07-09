/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kohii.v1.sample.ui.rview

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.doOnNextLayout
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kohii.v1.sample.R
import okio.Okio


/**
 * @author eneim (2018/07/06).
 */
class RecyclerViewFragment : Fragment() {

  companion object {
    fun newInstance() = RecyclerViewFragment()
  }

  data class PlayerInfo(val adapterPos: Int, val viewTop: Int)

  interface PlayerInfoHolder {

    fun recordPlayerInfo(info: PlayerInfo?)

    fun fetchPlayerInfo(): PlayerInfo?
  }

  private val items: List<Item>? by lazy {
    val asset = requireActivity().assets
    val type = Types.newParameterizedType(List::class.java, Item::class.java)
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val adapter: JsonAdapter<List<Item>> = moshi.adapter(type)
    adapter.fromJson(Okio.buffer(Okio.source(asset.open("theme.json"))))
  }

  override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_recycler_view, parent, false)
  }

  private var container: RecyclerView? = null
  private var playerInfoHolder: PlayerInfoHolder? = null

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    playerInfoHolder = context as? PlayerInfoHolder?
  }

  override fun onDetach() {
    super.onDetach()
    playerInfoHolder = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (items == null) return

    prepareTransitions()
    postponeEnterTransition()

    container = (view.findViewById(R.id.recyclerView) as RecyclerView).also {
      it.setHasFixedSize(false)
      it.layoutManager = LinearLayoutManager(requireContext())
      it.adapter = ItemsAdapter(this, ArrayList(items!!).apply { this.addAll(items!!) }) { dp ->
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            resources.displayMetrics).toInt()
      }
    }

    val playerInfo = this.playerInfoHolder?.fetchPlayerInfo()
    if (playerInfo != null) {
      container!!.doOnNextLayout {
        val layoutManager = container!!.layoutManager as LinearLayoutManager
        val viewAtPosition = layoutManager.findViewByPosition(playerInfo.adapterPos)
        // Scroll to position if the view for the current position is null (not currently part of
        // layout manager children), or it's not completely visible.
        if (viewAtPosition == null || //
            layoutManager.isViewPartiallyVisible(viewAtPosition, false, true)) {
          it.postDelayed(200) {
            layoutManager.scrollToPositionWithOffset(playerInfo.adapterPos, playerInfo.viewTop)
          }
        }
      }
    }
  }

  private fun prepareTransitions() {
    // Hmm Google https://stackoverflow.com/questions/49461738/transitionset-arraylist-size-on-a-null-object-reference
    val transition = TransitionInflater.from(requireContext())
        .inflateTransition(R.transition.player_exit_transition)
    transition.duration = 375
    exitTransition = transition

    val playerInfo = this.fetchPlayerInfo() ?: return
    setExitSharedElementCallback(object : SharedElementCallback() {
      override fun onMapSharedElements(names: List<String>?, elements: MutableMap<String, View>?) {
        // Locate the ViewHolder for the clicked position.
        val holder = container!!.findViewHolderForAdapterPosition(playerInfo.adapterPos)
        if (holder is VideoViewHolder) {
          // Map the first shared element name to the child ImageView.
          elements?.put(names?.get(0)!!, holder.transView)
        } else {
          return
        }
      }
    })
  }

  // Called by Adapter
  fun recordPlayerInfo(playerInfo: PlayerInfo?) {
    this.playerInfoHolder?.recordPlayerInfo(playerInfo)
  }

  // Called by Adapter
  fun fetchPlayerInfo() = this.playerInfoHolder?.fetchPlayerInfo()
}