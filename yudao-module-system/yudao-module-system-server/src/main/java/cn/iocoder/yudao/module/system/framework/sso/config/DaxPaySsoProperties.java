package cn.iocoder.yudao.module.system.framework.sso.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * DaxPay 单点登录配置（以 DaxPay 为登录入口、yudao 为账号匹配方）。
 *
 * <p>本配置只服务于 {@code POST /admin-api/system/sso/daxpay/exchange} 一个内部接口：
 * 网关（ft-gateway）校验 DaxPay 登录态后，带着签名来换取 yudao 访问令牌。</p>
 *
 * @author 芋道源码
 */
@ConfigurationProperties(prefix = "yudao.sso.daxpay")
@Data
public class DaxPaySsoProperties {

    /**
     * 总开关。关闭后 exchange 接口直接报错，不影响 yudao 原有账号密码登录。
     */
    private boolean enable = true;

    /**
     * 与网关共享的签名密钥（网关侧 {@code ft.gateway.sso.shared-secret} 必须一致）。
     * 建议 32 位以上随机串，只放配置中心，不落库。
     */
    private String exchangeSecret;

    /**
     * 签名时间戳容忍范围（秒），超出即判定为重放。
     */
    private long timestampToleranceSeconds = 300;

    /**
     * 签发 yudao 令牌使用的 OAuth2 客户端编号，需存在于 system_oauth2_client。
     */
    private String clientId = "default";

    /**
     * 本地不存在同名账号时是否自动创建。
     */
    private boolean autoCreate = true;

    /**
     * 自动创建账号时分配的默认角色编号集合。
     * <p>为空时新建账号没有任何菜单权限，登录后后台是空白的，务必配置一个基础角色。</p>
     */
    private List<Long> defaultRoleIds = new ArrayList<>();

    /**
     * 自动创建账号时使用的随机密码长度（4~16，用户不会用到这个密码）。
     */
    private int randomPasswordLength = 12;

    /**
     * DaxPay 账号含非字母数字字符时，映射为 yudao 账号的前缀。
     * 最终账号 = 前缀 + SHA-256(account) 前 10 位。
     */
    private String usernameFallbackPrefix = "dx";

}
