package com.glass.notifpanel

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView

class PanelRoot @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : FrameLayout(c, a) {
    var onBack: (() -> Unit)? = null

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BACK) {
            if (e.action == KeyEvent.ACTION_UP) onBack?.invoke()
            return true
        }
        return super.dispatchKeyEvent(e)
    }
}

class MaxHeightRecyclerView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : RecyclerView(c, a) {
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val max = (resources.displayMetrics.heightPixels * 0.55f).toInt()
        super.onMeasure(widthSpec, View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST))
    }
}
