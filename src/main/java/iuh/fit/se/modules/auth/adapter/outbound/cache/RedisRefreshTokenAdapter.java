package iuh.fit.se.modules.auth.adapter.outbound.cache;

import iuh.fit.se.modules.auth.application.port.out.RefreshTokenPersistencePort;
import iuh.fit.se.shared.security.AccessTokenSessionValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisRefreshTokenAdapter implements RefreshTokenPersistencePort, AccessTokenSessionValidator {

    private final RedisTemplate<String, String> stringRedisTemplate;

    public RedisRefreshTokenAdapter(@Qualifier("authStringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String TOKEN_KEY_PREFIX = "refresh:";
    private static final String VERSION_KEY_PREFIX = "refresh_version:";
    private static final long TTL_DAYS = 7;

    @Override
    public void saveRefreshToken(String userId, String deviceId, String token) {
        String tokenKey = getTokenKey(userId, deviceId);
        String versionKey = getVersionKey(userId, deviceId);

        // Sử dụng Pipeline để atomic hoá việc lưu và set TTL
        stringRedisTemplate.executePipelined((RedisCallback<?>) connection -> {
            byte[] tkK = tokenKey.getBytes();
            byte[] vK = versionKey.getBytes();
            byte[] tkV = token.getBytes();

            connection.stringCommands().set(tkK, tkV);
            connection.keyCommands().expire(tkK, TimeUnit.DAYS.toSeconds(TTL_DAYS));
            connection.keyCommands().expire(vK, TimeUnit.DAYS.toSeconds(TTL_DAYS));
            return null;
        });
    }

    @Override
    public String getRefreshToken(String userId, String deviceId) {
        return stringRedisTemplate.opsForValue().get(getTokenKey(userId, deviceId));
    }

    @Override
    public Integer incrementAndGetVersion(String userId, String deviceId) {
        String versionKey = getVersionKey(userId, deviceId);
        String tokenKey = getTokenKey(userId, deviceId);

        // Atomic Increment + Expire Pipeline
        Long newVersion = stringRedisTemplate.opsForValue().increment(versionKey);
        
        // Cập nhật TTL cho cả 2 keys
        stringRedisTemplate.expire(versionKey, TTL_DAYS, TimeUnit.DAYS);
        stringRedisTemplate.expire(tokenKey, TTL_DAYS, TimeUnit.DAYS);

        return newVersion != null ? newVersion.intValue() : 0;
    }

    @Override
    public Integer getCurrentVersion(String userId, String deviceId) {
        String version = stringRedisTemplate.opsForValue().get(getVersionKey(userId, deviceId));
        return version != null ? Integer.parseInt(version) : 0;
    }

    @Override
    public boolean isActive(Long userId, String deviceId, Integer refreshVersion) {
        if (userId == null || deviceId == null || deviceId.isBlank() || refreshVersion == null) {
            return false;
        }
        Integer currentVersion = getCurrentVersion(userId.toString(), deviceId);
        return refreshVersion.equals(currentVersion);
    }

    @Override
    public void revokeDeviceSession(String userId, String deviceId) {
        stringRedisTemplate.delete(getTokenKey(userId, deviceId));
        stringRedisTemplate.delete(getVersionKey(userId, deviceId));
    }

    @Override
    public void revokeAllUserSessions(String userId) {
        Set<String> tokenKeys = stringRedisTemplate.keys(TOKEN_KEY_PREFIX + userId + ":*");
        Set<String> versionKeys = stringRedisTemplate.keys(VERSION_KEY_PREFIX + userId + ":*");
        
        if (tokenKeys != null && !tokenKeys.isEmpty()) stringRedisTemplate.delete(tokenKeys);
        if (versionKeys != null && !versionKeys.isEmpty()) stringRedisTemplate.delete(versionKeys);
    }

    private String getTokenKey(String userId, String deviceId) {
        return TOKEN_KEY_PREFIX + userId + ":" + deviceId;
    }

    private String getVersionKey(String userId, String deviceId) {
        return VERSION_KEY_PREFIX + userId + ":" + deviceId;
    }
}
