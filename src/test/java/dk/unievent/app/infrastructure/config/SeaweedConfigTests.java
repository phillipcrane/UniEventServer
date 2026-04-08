package dk.unievent.app.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeaweedConfigTests {

    @Test
    void gettersAndSettersShouldStoreSeaweedProperties() {
        SeaweedConfig config = new SeaweedConfig();

        config.setMasterUrl("seaweed-master:9333");
        config.setVolumeUrl("http://seaweed-volume:8080");
        config.setReplicationFactor(2);

        assertEquals("seaweed-master:9333", config.getMasterUrl());
        assertEquals("http://seaweed-volume:8080", config.getVolumeUrl());
        assertEquals(2, config.getReplicationFactor());
    }
}
