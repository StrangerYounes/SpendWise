package com.corner.myshoppinglist.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptParserTest {

    @Test
    fun testParseReceiptText() {
        val text = """
            mushroom extra
            0.29 kg @ 680,000.00 197,200.00 0
            Tomato Extra
            2.325 kg @ 105,000.00 244,125.00 0
            Green Sweet Pepper
            0.51 kg @ 75,000.00 38,250.00 0
            Sweet Pepper Colored
            0.66 kg @ 350,000.00 231,000.00 0
            Liban Lait laban ful pc 358,701.00 0
            Rindo tamarind 1.25m
            2 pc @ 86,303.10 172,606.20 *
            cidfoods garlic dip pc 96,193.00 *
            Al Wadi fava beans 8
            2 pc @ 132,153.00 264,306.00 *
            Rim sparkling water
            1 pck6 @ 588,845.00 588,845.00 *
        """.trimIndent()

        val items = ReceiptParser.parseRows(text.lines())

        assertEquals(9, items.size)
        
        assertEquals("mushroom extra", items[0].name)
        assertEquals(0.29, items[0].quantity, 0.001)
        assertEquals(197200.0, items[0].totalPrice!!, 0.001)
        
        assertEquals("Liban Lait laban ful", items[4].name)
        assertEquals(358701.0, items[4].totalPrice!!, 0.001)
        
        assertEquals("Rim sparkling water", items[8].name)
        assertEquals(1.0, items[8].quantity, 0.001)
        assertEquals(588845.0, items[8].totalPrice!!, 0.001)
    }

    @Test
    fun testParseNoisyReceiptText() {
        val text = """
            Onion Red
            0,85 kg a 120 000,00 102 000,00 0
            Cucumber Local
            1.42 kg © 95.000,00 134.900,00 *
            Milk 1L Full Fat 150.000,00 0
            Bread Large Bundle 85 000,00
            Cheese Halloumi 250g 210,500.00 *
            TOTAL AMOUNT 682.400,00
        """.trimIndent()

        val items = ReceiptParser.parseRows(text.lines())

        assertEquals(5, items.size)

        assertEquals("Onion Red", items[0].name)
        assertEquals(0.85, items[0].quantity, 0.001)
        assertEquals(102000.0, items[0].totalPrice!!, 0.001)

        assertEquals("Cucumber Local", items[1].name)
        assertEquals(134900.0, items[1].totalPrice!!, 0.001)

        assertEquals("Milk 1L Full Fat", items[2].name)
        assertEquals(150000.0, items[2].totalPrice!!, 0.001)

        assertEquals("Bread Large Bundle", items[3].name)
        assertEquals(85000.0, items[3].totalPrice!!, 0.001)

        assertEquals("Cheese Halloumi 250g", items[4].name)
        assertEquals(210500.0, items[4].totalPrice!!, 0.001)
    }

    @Test
    fun testParseSecondReceiptText() {
        val text = """
            Favia Group s.a.r.l
            Description Amount
            Amarin arabic brown
            3 pc @ 102,486.00 307,458.00 0
            Domo Corn Flour Glut pc 69,223.00 0
            X-tra Ketchup Hot 34 pc 75,516.00 *
            Tomato Extra
            1.78 kg @ 90,000.00 160,200.00 0
            Bonux Standard 1.5K off2 308,357.00 *
            Total L.L. 1,436,890.00
        """.trimIndent()

        val items = ReceiptParser.parseRows(text.lines())

        assertEquals(5, items.size)
        assertEquals("Amarin arabic brown", items[0].name)
        assertEquals(307458.0, items[0].totalPrice!!, 0.01)
        
        assertEquals("Domo Corn Flour Glut", items[1].name)
        assertEquals(69223.0, items[1].totalPrice!!, 0.01)
        
        assertEquals("Tomato Extra", items[3].name)
        assertEquals(1.78, items[3].quantity, 0.01)
    }
}
