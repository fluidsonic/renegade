package ccr.server.level

import ccr.server.defs.DefinitionClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefinitionRegistryTest {

    @Test
    fun `empty registry has size zero`() {
        val registry = DefinitionRegistry()
        assertEquals(0, registry.size)
        assertTrue(registry.isEmpty)
    }

    @Test
    fun `register and find by id`() {
        val registry = DefinitionRegistry()
        val def = DefinitionClass("TestDef", 42u, 100u)
        registry.register(def)
        assertEquals(1, registry.size)
        val found = registry.findById(42u)
        assertNotNull(found)
        assertEquals("TestDef", found.name)
        assertEquals(42u, found.id)
    }

    @Test
    fun `register and find by name`() {
        val registry = DefinitionRegistry()
        val def = DefinitionClass("MyDef", 1u, 2u)
        registry.register(def)
        val found = registry.findByName("MyDef")
        assertNotNull(found)
        assertEquals(1u, found.id)
    }

    @Test
    fun `find returns null for unknown id`() {
        val registry = DefinitionRegistry()
        assertNull(registry.findById(999u))
    }

    @Test
    fun `find returns null for unknown name`() {
        val registry = DefinitionRegistry()
        assertNull(registry.findByName("NonExistent"))
    }

    @Test
    fun `later registration with same id overwrites earlier`() {
        val registry = DefinitionRegistry()
        registry.register(DefinitionClass("First", 1u, 10u))
        registry.register(DefinitionClass("Second", 1u, 20u))
        assertEquals(1, registry.size)
        assertEquals("Second", registry.findById(1u)?.name)
    }

    @Test
    fun `all returns all registered definitions`() {
        val registry = DefinitionRegistry()
        registry.register(DefinitionClass("A", 1u, 0u))
        registry.register(DefinitionClass("B", 2u, 0u))
        registry.register(DefinitionClass("C", 3u, 0u))
        assertEquals(3, registry.all().size)
    }
}
