package cn.iocoder.yudao.module.system.controller.admin.sso;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.system.controller.admin.sso.vo.SsoDaxPayExchangeRespVO;
import cn.iocoder.yudao.module.system.framework.sso.core.SsoExchangeSigner;
import cn.iocoder.yudao.module.system.service.sso.DaxPayExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - DaxPay 单点登录（内部接口）
 *
 * <p>账号密码体系以 DaxPay 为准，登录入口是 DaxPay 的登录页；ft-gateway 校验 DaxPay 登录态后，
 * 调用本接口把 DaxPay 身份换成 yudao 访问令牌，再由网关注入 {@code Authorization: Bearer} 转发给下游。</p>
 *
 * <p>安全边界：本接口 {@code @PermitAll}（不校验 yudao 令牌，因为调用方此刻还没有令牌），
 * 依赖 {@code X-Sso-Sign} 的 HMAC 签名 + 时间戳容忍窗口做内部信任，密钥只存在于网关与 yudao 的配置中心。
 * 生产环境应保证该路径只能从网关内网访问（网络层隔离）。</p>
 *
 * <p>路径前缀：控制器位于 {@code controller.admin} 包，框架自动加 {@code /admin-api}，
 * 实际地址为 {@code /admin-api/system/sso/daxpay/exchange}。</p>
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - DaxPay 单点登录（内部接口）")
@RestController
@RequestMapping("/system/sso")
public class SsoController {

    @Resource
    private DaxPayExchangeService daxPayExchangeService;

    @PostMapping("/daxpay/exchange")
    @PermitAll
    @Operation(summary = "DaxPay 身份换取 yudao 访问令牌",
            description = "仅供 ft-gateway 内部调用；携带 HMAC 签名，不校验 DaxPay 密码")
    public CommonResult<SsoDaxPayExchangeRespVO> exchange(
            @RequestBody String rawBody,
            @RequestHeader(value = SsoExchangeSigner.HEADER_TIMESTAMP, required = false) String timestamp,
            @RequestHeader(value = SsoExchangeSigner.HEADER_NONCE, required = false) String nonce,
            @RequestHeader(value = SsoExchangeSigner.HEADER_SIGN, required = false) String sign) {
        return success(daxPayExchangeService.exchange(rawBody, timestamp, nonce, sign));
    }

}
