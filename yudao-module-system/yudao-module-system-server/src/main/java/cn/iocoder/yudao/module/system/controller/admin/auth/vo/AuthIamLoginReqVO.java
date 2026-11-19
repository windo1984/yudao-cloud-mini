package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 统一认证（ft-iam ticket）登录请求。
 *
 * <p>基座 ft-layout 打开本应用时把一次性 ticket 带到地址栏，
 * 前端读取后调用本接口换取 yudao 自己的令牌。</p>
 */
@Data
public class AuthIamLoginReqVO {

    /** ft-iam 签发的一次性票据 */
    @NotBlank(message = "ticket 不能为空")
    private String ticket;

}
