package cn.iocoder.yudao.module.system.controller.admin.sso.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 管理后台 - DaxPay 身份换取 yudao 令牌 Response VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - DaxPay 身份换取 yudao 令牌 Response VO")
@Data
@Accessors(chain = true)
public class SsoDaxPayExchangeRespVO {

    @Schema(description = "yudao 用户编号", example = "1024")
    private Long userId;

    @Schema(description = "yudao 用户账号", example = "ft_admin")
    private String username;

    @Schema(description = "yudao 访问令牌")
    private String accessToken;

    @Schema(description = "yudao 刷新令牌")
    private String refreshToken;

    @Schema(description = "yudao 访问令牌过期时间")
    private LocalDateTime expiresTime;

    @Schema(description = "本次是否为新建账号（true 表示首次由 DaxPay 身份自动开通）", example = "false")
    private Boolean created;

}
