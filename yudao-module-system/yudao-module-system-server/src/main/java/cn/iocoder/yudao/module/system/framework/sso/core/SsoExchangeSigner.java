package cn.iocoder.yudao.module.system.framework.sso.core;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 网关与 yudao 之间的内部请求签名。
 *
 * <p>签名内容：{@code timestamp + "\n" + nonce + "\n" + rawBody}，算法 HMAC-SHA256，十六进制小写。
 * 网关侧实现必须与本类完全一致（ft-gateway 的 {@code SsoExchangeSigner}）。</p>
 *
 * @author 芋道源码
 */
public final class SsoExchangeSigner {

    /** 签名请求头：时间戳（毫秒） */
    public static final String HEADER_TIMESTAMP = "X-Sso-Timestamp";

    /** 签名请求头：一次性随机串 */
    public static final String HEADER_NONCE = "X-Sso-Nonce";

    /** 签名请求头：签名值 */
    public static final String HEADER_SIGN = "X-Sso-Sign";

    private static final String ALGORITHM = "HmacSHA256";

    private SsoExchangeSigner() {
    }

    public static String sign(String secret, String timestamp, String nonce, String body) {
        String content = timestamp + "\n" + nonce + "\n" + (body == null ? "" : body);
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return toHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("计算统一登录签名失败", ex);
        }
    }

    /**
     * 校验签名，使用常量时间比较避免时序侧信道。
     */
    public static boolean verify(String secret, String timestamp, String nonce, String body, String signature) {
        if (secret == null || signature == null) {
            return false;
        }
        String expected = sign(secret, timestamp, nonce, body);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

}
