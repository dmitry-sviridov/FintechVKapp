package ru.sviridov.newsfeed.presentation.adapter.swipe

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ru.sviridov.newsfeed.R
import ru.sviridov.newsfeed.presentation.adapter.FeedAdapter


// TODO: Fix flashing bug on swipe to like/dislike
class FeedItemCustomTouchHelperCallback(
    private val adapter: FeedAdapter,
    private val context: Context
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteIcon = AppCompatResources.getDrawable(context, R.drawable.ic_delete)!!
    private val likeIcon = AppCompatResources.getDrawable(context, R.drawable.ic_like)!!

    private val colorHideDrawableBackground =
        ColorDrawable(context.resources.getColor(R.color.swipe_to_delete))
    private val colorLikeDrawableBackground =
        ColorDrawable(context.resources.getColor(R.color.swipe_to_like))

    lateinit var colorDrawableBackground: ColorDrawable
    lateinit var icon: Drawable

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        viewHolder2: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDirection: Int) {
        if (swipeDirection == ItemTouchHelper.LEFT) {
            adapter.onItemDismiss(viewHolder.adapterPosition)
        } else {
            adapter.onItemApprove(viewHolder.adapterPosition)
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView

        // left swipe case
        if (dX < 0) {
            colorDrawableBackground = colorHideDrawableBackground
            icon = deleteIcon
            val iconMarginVertical =
                (viewHolder.itemView.height - icon.intrinsicHeight) / 2
            val iconMarginHorizontal = icon.intrinsicWidth
            colorDrawableBackground.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
            icon.setBounds(
                itemView.right - iconMarginHorizontal - icon.intrinsicWidth,
                itemView.top + iconMarginVertical,
                itemView.right - iconMarginHorizontal,
                itemView.bottom - iconMarginVertical
            )
        } else {
            // right swipe case
            colorDrawableBackground = colorLikeDrawableBackground
            icon = likeIcon

            val iconMarginVertical =
                (viewHolder.itemView.height - icon.intrinsicHeight) / 2
            val iconMarginHorizontal = icon.intrinsicWidth
            colorDrawableBackground.setBounds(
                itemView.left, itemView.top, dX.toInt(), itemView.bottom
            )
            icon.setBounds(
                itemView.left + iconMarginHorizontal,
                itemView.top + iconMarginVertical,
                itemView.left + iconMarginHorizontal + icon.intrinsicWidth,
                itemView.bottom - iconMarginVertical
            )
        }

        icon.level = 0
        colorDrawableBackground.draw(c)

        c.save()

        if (dX > 0)
            c.clipRect(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
        else
            c.clipRect(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )

        icon.draw(c)

        c.restore()

        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            dX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }
}