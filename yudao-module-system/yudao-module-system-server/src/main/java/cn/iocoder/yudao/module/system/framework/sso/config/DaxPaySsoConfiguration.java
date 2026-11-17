package cn.iocoder.yudao.module.system.framework.sso.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DaxPay 单点登录配置装配。
 *
 * <p>本方案不需要任何出站客户端 Bean：账号密码校验全部在 DaxPay 完成，
 * yudao 只提供 {@code /system/sso/daxpay/exchange} 一个内部接口。</p>
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DaxPaySsoProperties.class)
public class DaxPaySsoConfiguration {
}
