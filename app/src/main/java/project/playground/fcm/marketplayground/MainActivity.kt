package project.playground.fcm.marketplayground

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.CheckBox
import kotlinx.android.synthetic.main.activity_main.market_tree
import kotlinx.android.synthetic.main.activity_main.resize_bot_right
import kotlinx.android.synthetic.main.activity_main.resize_center
import kotlinx.android.synthetic.main.activity_main.resize_left
import kotlinx.android.synthetic.main.activity_main.scale_transform
import kotlinx.android.synthetic.main.activity_main.seekBar
import kotlinx.android.synthetic.main.activity_main.show_debug
import project.playground.fcm.marketplayground.library.data.MarketProvider

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resize_center.setOnClickListener {
            market_tree.resizeVirtualField(
                (market_tree.width / 2).toFloat() to (market_tree.height / 2).toFloat(),
                (if (seekBar.progress < 400) seekBar.progress * -1 else seekBar.progress / 400).toFloat()
            )
        }

        resize_left.setOnClickListener {
            market_tree.resizeVirtualField(
                (market_tree.width / 6).toFloat() to (market_tree.height / 2).toFloat(),
                800f
            )
        }

        resize_bot_right.setOnClickListener {
            market_tree.resizeVirtualField(
                (market_tree.width / 6).toFloat() to (market_tree.height / 2).toFloat(),
                -600f
            )
        }

        market_tree.shouldApplyScaleAndTransform = false
        scale_transform.setOnClickListener {
            market_tree.shouldApplyScaleAndTransform = (it as CheckBox).isChecked
        }
        show_debug.setOnClickListener {
            market_tree.inDebugMode = (it as CheckBox).isChecked
        }
    }

    override fun onResume() {
        super.onResume()
        market_tree.marketData = MarketProvider.getSmallMarket()
    }
}
