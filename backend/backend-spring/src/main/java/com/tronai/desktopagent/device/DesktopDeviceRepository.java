package com.tronai.desktopagent.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DesktopDeviceRepository extends JpaRepository<DesktopDeviceEntity, Long> {

    List<DesktopDeviceEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<DesktopDeviceEntity> findByIdAndUserId(Long id, Long userId);

    Optional<DesktopDeviceEntity> findByDeviceUuid(String deviceUuid);

    Optional<DesktopDeviceEntity> findByCredentialHash(String credentialHash);

    long countByStatus(DeviceStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DesktopDeviceEntity d
            set d.status = com.tronai.desktopagent.device.DeviceStatus.OFFLINE
            where d.id = :id
              and d.status in (
                  com.tronai.desktopagent.device.DeviceStatus.ONLINE,
                  com.tronai.desktopagent.device.DeviceStatus.BUSY)
              and (d.lastSeen is null or d.lastSeen < :cutoff)
            """)
    int markOfflineIfStale(@Param("id") Long id, @Param("cutoff") Instant cutoff);
}
