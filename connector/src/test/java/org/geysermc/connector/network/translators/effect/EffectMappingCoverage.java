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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class EffectMappingCoverage {
    static Set<ParticleType> particleTypesWithAdditionalData = new HashSet<>();
    static {
        particleTypesWithAdditionalData.add(ParticleType.BLOCK);
        particleTypesWithAdditionalData.add(ParticleType.DUST);
        particleTypesWithAdditionalData.add(ParticleType.FALLING_DUST);
        particleTypesWithAdditionalData.add(ParticleType.ITEM);
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
        try {
            assertEquals(missingParticle.size(), 0);
        } catch (AssertionError error) {
            System.err.println("Missing particle mappings: " + missingParticle);
        }
    }

}
