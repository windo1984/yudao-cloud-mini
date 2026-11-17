package cn.iocoder.yudao.module.system.controller.admin.sso.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 管理后台 - DaxPay 身份换取 yudao 令牌 Request VO
 *
 * <p>由 ft-gateway 在校验 DaxPay 登录态后构造，属于内部接口的报文，
 * 业务方不直接调用。</p>
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - DaxPay 身份换取 yudao 令牌 Request VO")
@Data
public class SsoDaxPayExchangeReqVO {

    @Schema(description = "DaxPay 用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long daxpayUserId;

    @Schema(description = "DaxPay 账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ft_admin")
    @NotEmpty(message = "DaxPay 账号不能为空")
    private String account;

    @Schema(description = "DaxPay 用户名称", example = "张三")
    private String name;

    @Schema(description = "DaxPay 身份域，运营端为 admin", example = "admin")
    private String clientCode;

}
