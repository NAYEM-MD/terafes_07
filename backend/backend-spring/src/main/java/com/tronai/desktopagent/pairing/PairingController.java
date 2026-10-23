package com.tronai.desktopagent.pairing;

import com.tronai.auth.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/desktop-agents")
public class PairingController {

    private final PairingService pairing;

    public PairingController(PairingService pairing) {
        this.pairing = pairing;
    }

    @PostMapping("/pairing-requests")
    public Map<String, Object> create(@Valid @RequestBody CreatePairingRequest request) {
        return pairing.createRequest(request.deviceUuid(), request.publicKey(), request.os(),
                request.hostname(), request.agentVersion(), request.displayName());
    }

    @GetMapping("/pairing-requests/{id}")
    public Map<String, Object> poll(@PathVariable String id,
                                    @RequestHeader(value = "X-Tron-Poll-Secret", required = false) String headerSecret,
                                    @RequestParam(value = "pollSecret", required = false) String pollSecret) {
        String secret = headerSecret != null && !headerSecret.isBlank() ? headerSecret : pollSecret;
        return pairing.poll(id, secret);
    }

    @PostMapping("/pair")
    public Map<String, Object> pair(@Valid @RequestBody PairRequest request) {
        return pairing.pair(SecurityUtils.getRequiredUserId(), request.code(), request.allowedWorkspace(), request.displayName());
    }

    @PostMapping("/link-requests")
    public Map<String, Object> createLinkRequest(@Valid @RequestBody CreateLinkRequest request) {
        return pairing.createLinkRequest(request.deviceUuid(), request.publicKey(), request.os(),
                request.hostname(), request.agentVersion(), request.displayName(), request.requestedWorkspace());
    }

    @GetMapping("/link-requests/{id}")
    public Map<String, Object> getLinkRequest(@PathVariable String id) {
        return pairing.getLinkRequest(id);
    }

    @PostMapping("/link-requests/{id}/authorize")
    public Map<String, Object> authorizeLinkRequest(@PathVariable String id,
                                                    @RequestBody(required = false) AuthorizeLinkRequest request) {
        String allowedWorkspace = request != null ? request.allowedWorkspace() : null;
        String displayName = request != null ? request.displayName() : null;
        return pairing.authorizeLinkRequest(SecurityUtils.getRequiredUserId(), id, allowedWorkspace, displayName);
    }

    @GetMapping("/link-requests/{id}/poll")
    public Map<String, Object> pollLinkRequest(@PathVariable String id,
                                              @RequestHeader(value = "X-Tron-Poll-Secret", required = false) String headerSecret,
                                              @RequestParam(value = "pollSecret", required = false) String pollSecret) {
        String secret = headerSecret != null && !headerSecret.isBlank() ? headerSecret : pollSecret;
        return pairing.poll(id, secret);
    }

    @PostMapping("/link-requests/{id}/poll")
    public Map<String, Object> pollLinkRequestPost(@PathVariable String id,
                                                   @RequestHeader(value = "X-Tron-Poll-Secret", required = false) String headerSecret,
                                                   @RequestBody(required = false) PollLinkRequest body) {
        String secret = headerSecret;
        if ((secret == null || secret.isBlank()) && body != null) {
            secret = body.pollSecret();
        }
        return pairing.poll(id, secret);
    }

    @GetMapping("/download-info")
    public Map<String, Object> getDownloadInfo() {
        return pairing.getDownloadInfo();
    }

    public record CreateLinkRequest(
            @NotBlank String deviceUuid,
            @NotBlank String publicKey,
            String os,
            String hostname,
            String agentVersion,
            String displayName,
            String requestedWorkspace
    ) {
    }

    public record AuthorizeLinkRequest(
            String allowedWorkspace,
            String displayName
    ) {
    }

    public record PollLinkRequest(String pollSecret) {
    }

    public record CreatePairingRequest(
            @NotBlank String deviceUuid,
            @NotBlank String publicKey,
            String os,
            String hostname,
            String agentVersion,
            String displayName
    ) {
    }

    public record PairRequest(
            @NotBlank String code,
            @NotBlank String allowedWorkspace,
            String displayName
    ) {
    }
}
