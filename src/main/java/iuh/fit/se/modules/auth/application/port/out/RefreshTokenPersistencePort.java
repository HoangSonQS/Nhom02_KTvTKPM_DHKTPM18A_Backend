package iuh.fit.se.modules.auth.application.port.out;

/**
 * Port giao tiếp để quản lý Refresh Token trong cache (Redis).
 * Hỗ trợ Multi-device và Reuse Detection (rv counter).
 */
public interface RefreshTokenPersistencePort {
    
    /**
     * Lưu trữ Refresh Token cho một thiết bị cụ thể.
     * Gia hạn TTL cho cả Token và Version counter.
     */
    void saveRefreshToken(String userId, String deviceId, String token);

    /**
     * Lấy Refresh Token hiện tại của thiết bị.
     */
    String getRefreshToken(String userId, String deviceId);

    /**
     * Lấy và tăng Refresh Version (rv) của thiết bị một cách nguyên tử.
     * Gia hạn TTL sau mỗi lần tăng.
     */
    Integer incrementAndGetVersion(String userId, String deviceId);

    /**
     * Lấy Refresh Version hiện tại.
     */
    Integer getCurrentVersion(String userId, String deviceId);

    /**
     * Xóa sạch phiên (vô hiệu hóa) của một thiết bị cụ thể.
     */
    void revokeDeviceSession(String userId, String deviceId);

    /**
     * Xóa sạch TẤT CẢ các phiên của người dùng (dùng khi phát hiện Reuse Token).
     */
    void revokeAllUserSessions(String userId);
}
