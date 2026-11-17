package com.cong.fishisland.thirdparty.music;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Only the server reads this configuration. Never expose token in a response. */
@Data
@Component
@ConfigurationProperties(prefix = "fishisland.third-party.music")
public class MusicProperties {
    private String apiUrl = "";
    private String token = "";
}
