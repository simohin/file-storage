package com.github.simohin.file.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Data
@Component
@ConfigurationProperties(prefix = "app.file-storage")
public class DiskSpaceProperties {

    private String path = "./storage";
    private DataSize maxTotalSize = DataSize.ofMegabytes(200);
    private boolean diskSpaceCheckEnabled = true;
    private int diskSpaceThreshold = 90;
}