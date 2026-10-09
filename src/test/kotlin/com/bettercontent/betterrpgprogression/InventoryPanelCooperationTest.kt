package com.bettercontent.betterrpgprogression

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Files
import java.nio.file.Path

class InventoryPanelCooperationTest {
    @Test fun optionalRecordHostReplacesRetiredScreenNameWithoutChangingNativeGameplay() {
        val source = Files.readString(Path.of("src/main/kotlin/com/bettercontent/betterrpgprogression/client/ui/InventoryStatsOverlay.kt"))
        assertTrue("setJournalPanelHost" in source)
        assertTrue("{ false }" in source)
        assertTrue("if (journalPanelHost.test(screen)) return" in source)
        assertFalse("JournalInventoryScreen" in source)
        assertFalse("containerMenu" in source)
        assertFalse("handleInventoryMouseClick" in source)
    }
}
