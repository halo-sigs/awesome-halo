package com.kyotom.halo.immich;

import lombok.Data;
import org.springframework.util.StringUtils;
import java.util.Set;

/**
 * The properties of storage policy that uses this plugin as backend.
 * <p>
 * This data class is bound to {@code policy-template-immich.yaml}.
 */
@Data
public class ImmichProperties {
    private String displayName;

    /**
     * Including protocol, without trailing {@code /} or api path.
     */
    private String immichUrl;

    /**
     * Without leading {@code Bearer}.
     */
    private String immichApiKey;

    private String immichAlbumName;


    private Set<String> allowedFileTypes;

    @SuppressWarnings("unused")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @SuppressWarnings("unused")
    public void setImmichUrl(String immichUrl) {
        if (immichUrl.endsWith("/")) {
            this.immichUrl = immichUrl.substring(0, immichUrl.length() - 1);
        }
        else {
            this.immichUrl = immichUrl;
        }
    }

    @SuppressWarnings("unused")
    public void setImmichApiKey(String immichApiKey) {
        this.immichApiKey = StringUtils.hasText(immichApiKey) ? immichApiKey : null;
    }

    @SuppressWarnings("unused")
    public void setImmichAlbumName(String immichAlbumName) {
        if (StringUtils.hasText(immichAlbumName)) {
            this.immichAlbumName = immichAlbumName;
        } else {
            this.immichAlbumName = null;
        }
    }

    @SuppressWarnings("unused")
    public void setAllowedFileTypes(Set<String> allowedFileTypes) {
        this.allowedFileTypes = allowedFileTypes;
    }
}
