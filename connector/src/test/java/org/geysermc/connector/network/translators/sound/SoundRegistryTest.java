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

package org.geysermc.connector.network.translators.sound;

import com.github.steveice10.mc.protocol.data.game.world.sound.BuiltinSound;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SoundRegistryTest {

    @Test
    public void fromJava_BuiltinSounds_translatedCorrectly() {
        BuiltinSound[] builtinSounds = BuiltinSound.values();

        for (BuiltinSound builtinSound : builtinSounds) {
            SoundRegistry.SoundMapping soundMapping = SoundRegistry.fromJava(builtinSound.getName());

            assertNotNull("Expected sound mapping for: " + builtinSound, soundMapping);
        }
    }

    //Test if there are all BuiltinSounds mapped to corresponding bedrock packets
    @Test
    public void toSoundEvent_BuiltinSound_allMapped() {
        List<String> missingMappings = new ArrayList<>();
        BuiltinSound[] builtinSounds = BuiltinSound.values();

        for (BuiltinSound builtinSound : builtinSounds) {
            SoundRegistry.SoundMapping soundMapping = SoundRegistry.fromJava(builtinSound.getName());

            if (soundMapping.isLevelEvent()) {
                LevelEventType.valueOf(soundMapping.getBedrock());
            } else if (soundMapping.getPlaysound() == null) {
                SoundEvent sound = SoundRegistry.toSoundEvent(soundMapping.getBedrock());

                if (sound == null) {
                    sound = SoundRegistry.toSoundEvent(builtinSound.getName());
                }

                if (sound == null) {
                    missingMappings.add(builtinSound.getName());
                }
            }
        }

        try {
            assertEquals(missingMappings.size(), 0);
        } catch (AssertionError error) {
            System.err.println("Missing mappings: " + missingMappings);
        }
    }

}
