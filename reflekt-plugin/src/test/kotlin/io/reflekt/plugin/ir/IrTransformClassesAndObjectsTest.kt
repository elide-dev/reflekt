package io.reflekt.plugin.ir

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("ir")
class IrTransformClassesAndObjectsTest {
    @Test
    fun testClasses() {
        assertEquals(
            setOf("io.reflekt.test.ir.C1", "io.reflekt.test.ir.C2", "io.reflekt.test.ir.C3", "io.reflekt.test.ir.C3.C5"),
            IrTransformTestHelper.classFqNames("Reflekt.classes().withSupertype<CInterface>()")
        )
    }

    @Test
    fun testObjects() {
        assertEquals(
            setOf("io.reflekt.test.ir.O1", "io.reflekt.test.ir.O1.O2"),
            IrTransformTestHelper.objectFqNames("Reflekt.objects().withSupertype<OInterface>()")
        )
    }
}
