package iuh.fit.se.shared.cache;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Utility để tạo các cache key an toàn với Clock-drift và Versioning.
 */
public class CacheKeyUtility {

    private static final String APP_VERSION = "v1";

    /**
     * Tạo key có salt dựa trên "Giờ hiện tại" để tránh cache tồn tại quá lâu nếu logic thay đổi.
     * @param region Vùng cache (ví dụ: books)
     * @param key Key logic (ví dụ: id hoặc search query)
     */
    public static String createSaltedKey(String region, Object key) {
        // Truncate to hours để đảm bảo các instance trong cluster dùng chung 1 "thời điểm" salt
        String salt = Instant.now().truncatedTo(ChronoUnit.HOURS).toString();
        return String.format("%s:%s:%s:%s", APP_VERSION, region, salt, key);
    }

    /**
     * Dùng cho KeyGenerator của Spring Cache.
     */
    public static String generate(String region, Object... params) {
        StringBuilder sb = new StringBuilder();
        for (Object p : params) {
            sb.append(p).append("_");
        }
        return createSaltedKey(region, sb.toString());
    }
}
