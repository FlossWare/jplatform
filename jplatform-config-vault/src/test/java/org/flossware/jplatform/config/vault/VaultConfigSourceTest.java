package org.flossware.jplatform.config.vault;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VaultConfigSourceTest {
    @Test
    void testBasicFunctionality() {
        VaultConfigSource source = new VaultConfigSource();
        assertNotNull(source.loadConfig("/test"));
    }
}
