package project.playground.fcm.marketplayground.library.data

import com.threesixty.coin.math.Mappable
import com.threesixty.coin.math.Rect

data class MapGroup(val groupSize: Double) : Mappable {
    override var bounds: Rect = Rect()
    override var depth: Int = 0
    override var order: Int = 0
    override var size: Double = groupSize

    override fun setBounds(x: Double, y: Double, w: Double, h: Double) {
        bounds.setRect(x, y, w, h)
    }
}

data class MapItem(val value: Double, val name: String) : Mappable {

    override var bounds: Rect = Rect()
    override var depth: Int = 0
    override var order: Int = 0
    override var size: Double = value

    override fun setBounds(x: Double, y: Double, w: Double, h: Double) {
        bounds.setRect(x, y, w, h)
    }
}