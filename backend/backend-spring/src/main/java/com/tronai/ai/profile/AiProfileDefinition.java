package com.tronai.ai.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Public descriptor for an AI performance profile.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProfileDefinition {

    private String id;
    private String displayName;
    private String description;
    private boolean available;
    private String modelDisplayName;
    private String reasonUnavailable;
}
