package ru.sviridov.newsfeed.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_feed.*
import ru.sviridov.component.feeditem.model.NewsItem
import ru.sviridov.newsfeed.FeedType
import ru.sviridov.newsfeed.R
import ru.sviridov.newsfeed.presentation.adapter.FeedAdapter
import ru.sviridov.newsfeed.presentation.adapter.swipe.FeedItemCustomTouchHelperCallback
import ru.sviridov.newsfeed.presentation.viewmodel.FeedViewModel
import ru.sviridov.newsfeed.presentation.viewmodel.FeedViewModelFactory

class FeedFragment : Fragment(), AdapterActionHandler {

    private val feedAdapter: FeedAdapter by lazy { FeedAdapter(this) }
    private val feedType: FeedType by lazy { requireArguments().get(FEED_TYPE) as FeedType }

    private val viewModel by viewModels<FeedViewModel> {
        FeedViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshLayout.isEnabled = false

        viewModel.viewState.observe(viewLifecycleOwner, { viewState ->
            render(viewState)
        })

        if (feedType == FeedType.REGULAR_FEED) {
            setUpRefreshLayout()
            viewModel.handleAction(FeedViewActions.GetFreshNews)
        } else {
            viewModel.handleAction(FeedViewActions.GetLikedNews)
        }
        initRecycler()
    }

    private fun initRecycler() {
        val context = requireContext()

        val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        dividerItemDecoration.setDrawable(getDrawable(context, R.drawable.divider)!!)

        val linearLayoutManager = LinearLayoutManager(this.context)

        feedRecycler.apply {
            layoutManager = linearLayoutManager
            adapter = feedAdapter
            addItemDecoration(dividerItemDecoration)
            itemAnimator?.changeDuration = ITEM_ANIMATOR_DURATION
        }

        val itemTouchHelperCallback = FeedItemCustomTouchHelperCallback(feedAdapter, context)
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(feedRecycler)

        feedRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!refreshLayout.isRefreshing && feedType == FeedType.REGULAR_FEED) {
                    if (linearLayoutManager
                            .findLastVisibleItemPosition() == feedAdapter.itemCount - 1
                    ) {
                        viewModel.handleAction(FeedViewActions.GetPreviousNews)
                    }
                }
            }
        })
    }

    private fun render(state: FeedViewState) {
        when (state) {
            FeedViewState.Loading -> renderLoadingState()
            is FeedViewState.ShowApiError -> renderError(state.apiError)
            is FeedViewState.ShowNewsFeed -> renderUpdatedFeed(
                state.newsList,
                state.scrollRecyclerUp
            )
        }
    }

    private fun setUpRefreshLayout() {
        refreshLayout.isEnabled = true
        refreshLayout.setOnRefreshListener {
            viewModel.handleAction(FeedViewActions.GetFreshNews)
        }
    }

    override fun onImageViewClicked(url: String) {
        (requireActivity() as FeedFragmentHost).openDetails(url)
    }

    override fun onItemHided(item: NewsItem) {
        viewModel.handleAction(FeedViewActions.HideCurrentItem(item))
    }

    override fun onItemLiked(item: NewsItem, shouldBeLiked: Boolean) {
        if (shouldBeLiked) {
            viewModel.handleAction(FeedViewActions.SetCurrentItemAsLiked(item))
        } else {
            viewModel.handleAction(FeedViewActions.SetCurrentItemAsDisliked(item))
        }
    }

    private fun renderLoadingState() {
        refreshLayout.isRefreshing = true
    }

    private fun renderError(apiError: Throwable) {
        (requireActivity() as FeedFragmentHost).showErrorDialog(apiError.message)
        refreshLayout.isRefreshing = false
    }

    private fun renderUpdatedFeed(newList: List<NewsItem>, scrollRecyclerUp: Boolean) {
        feedAdapter.newsList = newList
        refreshLayout.isRefreshing = false
        if (scrollRecyclerUp) {
            feedRecycler.handler.postDelayed({
                try {
                    feedRecycler.scrollToPosition(0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 500)
        }
    }

    companion object {
        const val ITEM_ANIMATOR_DURATION = 50L

        private const val FEED_TYPE = "feed_type"

        fun newInstance(feedType: FeedType) =
            FeedFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(FEED_TYPE, feedType)
                }
            }
    }
}
