/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators.effect;

import com.github.steveice10.mc.protocol.data.game.world.particle.ParticleType;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class EffectMappingCoverage {
    private static final Set<ParticleType> particleTypesWithAdditionalData = new HashSet<>();
    private static final Set<ParticleType> knownMissingParticles = new HashSet<>();

    static {
        particleTypesWithAdditionalData.add(ParticleType.BLOCK);
        particleTypesWithAdditionalData.add(ParticleType.DUST);
        particleTypesWithAdditionalData.add(ParticleType.FALLING_DUST);
        particleTypesWithAdditionalData.add(ParticleType.ITEM);

        knownMissingParticles.add(ParticleType.LANDING_HONEY);
        knownMissingParticles.add(ParticleType.DRIPPING_OBBSIDIAN_TEAR);
        knownMissingParticles.add(ParticleType.ELDER_GUARDIAN);
        knownMissingParticles.add(ParticleType.WARPED_SPORE);
        knownMissingParticles.add(ParticleType.UNDERWATER);
        knownMissingParticles.add(ParticleType.CRIMSON_SPORE);
        knownMissingParticles.add(ParticleType.ASH);
        knownMissingParticles.add(ParticleType.SWEEP_ATTACK);
        knownMissingParticles.add(ParticleType.LANDING_LAVA);
        knownMissingParticles.add(ParticleType.LANDING_OBSIDIAN_TEAR);
        knownMissingParticles.add(ParticleType.WHITE_ASH);
        knownMissingParticles.add(ParticleType.DAMAGE_INDICATOR);
        knownMissingParticles.add(ParticleType.ENCHANTED_HIT);
        knownMissingParticles.add(ParticleType.REVERSE_PORTAL);
        knownMissingParticles.add(ParticleType.BARRIER);
    }

    @Test
    public void particleType_allMappedWithoutAdditionalData() {
        Set<ParticleType> missingParticle = new HashSet<>();
        ParticleType[] particleTypes = ParticleType.values();

        for (ParticleType particleType : particleTypes) {
            int particleId = EffectRegistry.getParticleId(particleType);
            if (particleId == -1) {
                LevelEventType eventType = EffectRegistry.getParticleLevelEventType(particleType);
                if (eventType == null) {
                    String eventName = EffectRegistry.getParticleString(particleType);
                    if (eventName == null) {
                        missingParticle.add(particleType);
                    }
                }
            }
        }

        missingParticle.removeAll(particleTypesWithAdditionalData);
        List<ParticleType> missingParticlesCopy = new ArrayList<>(missingParticle);
        missingParticlesCopy.removeAll(knownMissingParticles);

        List<ParticleType> knownMissingParticlesCopy = new ArrayList<>(knownMissingParticles);
        knownMissingParticlesCopy.removeAll(missingParticle);

        List<ParticleType> nolongerMissing = new ArrayList<>(knownMissingParticlesCopy);
        nolongerMissing.removeAll(knownMissingParticlesCopy);

        assertEquals("Missing effects mappings: ", missingParticlesCopy, Collections.emptyList());
        assertEquals("No longer missing effects mappings: ", nolongerMissing, Collections.emptyList());
    }

}
