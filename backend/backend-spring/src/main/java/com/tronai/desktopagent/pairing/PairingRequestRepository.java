package com.tronai.desktopagent.pairing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PairingRequestRepository extends JpaRepository<PairingRequestEntity, String> {

    Optional<PairingRequestEntity> findByCodeHash(String codeHash);

    Optional<PairingRequestEntity> findByIdAndPollSecretHash(String id, String pollSecretHash);

    Optional<PairingRequestEntity> findByPublicRequestId(String publicRequestId);
}
