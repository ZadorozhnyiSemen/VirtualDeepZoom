package project.playground.fcm.marketplayground.library.data

import com.threesixty.coin.math.TreeModel
import java.security.SecureRandom
import kotlin.random.Random

class MarketProvider {
    companion object {
        fun getSmallMarket() = TreeModel(MapGroup(1.0)).apply {
            val randomNameGenerator = RandomStringGenerator(4)
            val nOfEach = 2
            for (i in 1..nOfEach) {
                addChild(TreeModel(MapItem(2.0, randomNameGenerator.nextString())))
            }
            for (i in 1..nOfEach) {
                addChild(TreeModel(MapItem(3.0, randomNameGenerator.nextString())))
            }
            for (i in 1..nOfEach) {
                addChild(TreeModel(MapItem(5.0, randomNameGenerator.nextString())))
            }

            for (i in 1..nOfEach) {
                addChild(TreeModel(MapItem(.02, randomNameGenerator.nextString())))
            }
            for (i in 1..nOfEach) {
                addChild(TreeModel(MapItem(.2, randomNameGenerator.nextString())))
            }
            for (i in 1..nOfEach) {
                addChild(TreeModel(MapItem(2.0, randomNameGenerator.nextString())))
            }
//            for (i in 1..nOfEach) {
//                addChild(TreeModel(MapItem(3.0, randomNameGenerator.nextString())))
//            }
//            for (i in 1..nOfEach) {
//                addChild(TreeModel(MapItem(5.0, randomNameGenerator.nextString())))
//            }
//            for (i in 1..nOfEach) {
//                addChild(TreeModel(MapItem(10.0, randomNameGenerator.nextString())))
//            }
//            for (i in 1..nOfEach) {
//                addChild(TreeModel(MapItem(20.0, randomNameGenerator.nextString())))
//            }
        }
    }
}

class RandomStringGenerator(length: Int) {
    private val randomGenerator = SecureRandom()
    private val charArray = chars.toCharArray()
    private val buffer = CharArray(length)

    fun nextString(): String {
        for (i in 0 until buffer.size) {
            buffer[i] = charArray[randomGenerator.nextInt(charArray.size)]
        }
        return String(buffer)
    }

    companion object {
        final val chars = "abcdefghijklmnopqrstuvwxyz".toUpperCase()
    }
}