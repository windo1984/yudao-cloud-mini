package cn.iocoder.yudao.module.system.service.sso;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.controller.admin.sso.vo.SsoDaxPayExchangeReqVO;
import cn.iocoder.yudao.module.system.controller.admin.sso.vo.SsoDaxPayExchangeRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.framework.sso.config.DaxPaySsoProperties;
import cn.iocoder.yudao.module.system.framework.sso.core.SsoExchangeSigner;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SSO_DAXPAY_DISABLED;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SSO_DAXPAY_PARAM_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SSO_DAXPAY_SIGN_INVALID;

/**
 * DaxPay 身份换取 yudao 访问令牌。
 *
 * <p>链路位置：浏览器 → ft-gateway（校验 DaxPay 的 Sa-Token 登录态）→ 本服务（签发 yudao 令牌）→ 下游业务服务。
 * 因此本服务<b>不校验任何 DaxPay 密码</b>，也<b>不向 DaxPay 发起任何调用</b>：
 * DaxPay 账号密码是唯一登录凭据，本服务只做本地账号匹配与令牌签发。</p>
 *
 * @author 芋道源码
 */
@Slf4j
@Service
public class DaxPayExchangeService {

    @Resource
    private DaxPaySsoProperties properties;

    @Resource
    private DaxPayUserMatchService userMatchService;

    @Resource
    private OAuth2TokenService oauth2TokenService;

    /**
     * 校验网关签名并签发 yudao 访问令牌
     *
     * @param rawBody   原始请求体（签名基于原文，不能先反序列化再算）
     * @param timestamp 网关时间戳（毫秒）
     * @param nonce     一次性随机串
     * @param sign      签名值
     */
    public SsoDaxPayExchangeRespVO exchange(String rawBody, String timestamp, String nonce, String sign) {
        if (!properties.isEnable()) {
            throw exception(SSO_DAXPAY_DISABLED);
        }
        if (StrUtil.isBlank(properties.getExchangeSecret())) {
            throw exception(SSO_DAXPAY_SIGN_INVALID, "未配置 yudao.sso.daxpay.exchange-secret");
        }
        this.validateTimestamp(timestamp);
        if (!SsoExchangeSigner.verify(properties.getExchangeSecret(), timestamp, nonce, rawBody, sign)) {
            throw exception(SSO_DAXPAY_SIGN_INVALID, "签名不匹配");
        }

        SsoDaxPayExchangeReqVO reqVO = JsonUtils.parseObject(rawBody, SsoDaxPayExchangeReqVO.class);
        if (reqVO == null || StrUtil.isBlank(reqVO.getAccount())) {
            throw exception(SSO_DAXPAY_PARAM_INVALID, "请求体无法解析或 account 为空");
        }

        // 1. 匹配本地账号（不存在则按配置自动开通）
        DaxPayUserMatchService.MatchResult match = userMatchService.matchOrCreate(reqVO.getAccount(), reqVO.getName());
        // 2. 签发 yudao 令牌（跳过密码校验，等价于 AdminAuthServiceImpl#createTokenAfterLoginSuccess 的后半段）
        OAuth2AccessTokenDO tokenDO = oauth2TokenService.createAccessToken(
                match.user().getId(), UserTypeEnum.ADMIN.getValue(), properties.getClientId(), null);
        log.info("[exchange][DaxPay 账号({}) 换取 yudao 令牌成功，userId({})，created({})]",
                reqVO.getAccount(), match.user().getId(), match.created());
        return new SsoDaxPayExchangeRespVO()
                .setUserId(match.user().getId())
                .setUsername(match.user().getUsername())
                .setAccessToken(tokenDO.getAccessToken())
                .setRefreshToken(tokenDO.getRefreshToken())
                .setExpiresTime(tokenDO.getExpiresTime())
                .setCreated(match.created());
    }

    /**
     * 校验网关时间戳，防止重放
     */
    private void validateTimestamp(String timestamp) {
        long value;
        try {
            value = Long.parseLong(StrUtil.trimToEmpty(timestamp));
        } catch (NumberFormatException ex) {
            throw exception(SSO_DAXPAY_SIGN_INVALID, "缺少或非法的 X-Sso-Timestamp");
        }
        long diffSeconds = Math.abs(System.currentTimeMillis() - value) / 1000;
        if (diffSeconds > properties.getTimestampToleranceSeconds()) {
            throw exception(SSO_DAXPAY_SIGN_INVALID,
                    "请求时间戳已过期（偏差 " + diffSeconds + " 秒，容忍 " + properties.getTimestampToleranceSeconds() + " 秒）");
        }
    }

}
