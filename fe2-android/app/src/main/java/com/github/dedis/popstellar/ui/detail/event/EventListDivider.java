package com.github.dedis.popstellar.ui.detail.event;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * The purpose of this class is to provide a custom separator between elements of the event list In
 * particular we want only separators between sections and not between events or at the end
 */
public class EventListDivider extends RecyclerView.ItemDecoration {
  public static final String TAG = EventListDivider.class.getSimpleName();
  private final Paint mPaint;
  private final int mHeightDp;

  public EventListDivider(Context context) {
    this(context, Color.argb((int) (255 * 0.2), 0, 0, 0), 1f);
  }

  public EventListDivider(Context context, int color, float heightDp) {
    mPaint = new Paint();
    mPaint.setStyle(Paint.Style.FILL);
    mPaint.setColor(color);
    mHeightDp =
        (int)
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, heightDp, context.getResources().getDisplayMetrics());
  }

  private boolean hasDividerOnBottom(View view, RecyclerView parent, RecyclerView.State state) {
    int position = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewAdapterPosition();
    return position < state.getItemCount()
        && parent.getAdapter().getItemViewType(position + 1) == EventListAdapter.TYPE_HEADER;
  }

  @Override
  public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
    for (int i = 0; i < parent.getChildCount(); i++) {
      View view = parent.getChildAt(i);
      if (hasDividerOnBottom(view, parent, state)) {
        c.drawRect(
            view.getLeft(),
            view.getBottom(),
            view.getRight(),
            view.getBottom() + (float) mHeightDp,
            mPaint);
      }
    }
  }
}
