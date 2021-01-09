package org.geysermc.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WrappedPacket {
    private Class<?> clazz;
    private String packet;
    private long delay;
}
