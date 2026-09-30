package com.bettercontent.betterrpgprogression

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap

object TestMinecraftBootstrap {
    private var bootstrapped = false

    fun bootstrap() {
        if (bootstrapped) return

        SharedConstants.tryDetectVersion()
        try {
            Bootstrap.bootStrap()
        } catch (error: ExceptionInInitializerError) {
            // Forge initializes vanilla registries before its networking layer. Plain JVM
            // tests do not apply Forge's event-class transformer, so the final networking
            // hook cannot construct listener lists. Accept only that known post-registry
            // failure; production and GameTest launches apply the transformer normally.
            val expectedHarnessFailure = generateSequence<Throwable>(error) { it.cause }
                .filterIsInstance<NoSuchMethodException>()
                .any { it.message?.startsWith("net.minecraftforge.network.NetworkEvent") == true }
            if (!expectedHarnessFailure) throw error
        }
        bootstrapped = true
    }
}
