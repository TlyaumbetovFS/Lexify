package com.lexify.repository;

import com.lexify.model.RefreshSession;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, String> {

    @Modifying
    @Query("delete from RefreshSession session where session.expiresAt <= :now")
    void deleteExpired(Instant now);

    @Modifying
    @Query("delete from RefreshSession session where session.tokenHash = :hash")
    void revoke(String hash);
}
