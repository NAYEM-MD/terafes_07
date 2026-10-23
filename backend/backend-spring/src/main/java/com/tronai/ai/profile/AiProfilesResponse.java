package com.tronai.ai.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Top-level response for available AI profiles.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProfilesResponse {

    private List<AiProfileDefinition> profiles;
    private String defaultProfile;
}
