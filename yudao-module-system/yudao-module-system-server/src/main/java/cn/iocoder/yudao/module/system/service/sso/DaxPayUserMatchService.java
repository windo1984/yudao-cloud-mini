package cn.iocoder.yudao.module.system.service.sso;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.framework.sso.config.DaxPaySsoProperties;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SSO_DAXPAY_PARAM_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SSO_DAXPAY_USER_CREATE_FAIL;

/**
 * DaxPay 账号 → yudao 账号的匹配服务。
 *
 * <p>账号主数据在 DaxPay，yudao 只做「同名匹配」：能匹配上就用，匹配不上按配置自动开通。
 * 两个系统的账号规则不同，因此映射必须确定性且无碰撞：</p>
 *
 * <table>
 *     <tr><th>DaxPay account</th><th>yudao username</th></tr>
 *     <tr><td>纯字母数字、4~30 位</td><td>原样使用</td></tr>
 *     <tr><td>含 {@code _ - . @} 等字符，或长度不满足</td><td>{@code dx + SHA-256(account) 前 10 位}</td></tr>
 * </table>
 *
 * <p>为什么不「清洗掉非法字符」：{@code user-1} 与 {@code user1} 会被清洗成同一个账号，
 * 两个 DaxPay 用户就落到同一个 yudao 用户上，属于权限串号。哈希映射保证一一对应。</p>
 *
 * @author 芋道源码
 */
@Slf4j
@Service
public class DaxPayUserMatchService {

    /** yudao 账号规则：4~30 位纯字母数字（见 UserSaveReqVO#username 的 @Pattern） */
    private static final Pattern YUDAO_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{4,30}$");

    /** 昵称最大长度（UserSaveReqVO#nickname 的 @Size） */
    private static final int NICKNAME_MAX_LENGTH = 30;

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private PermissionService permissionService;

    @Resource
    private DaxPaySsoProperties properties;

    /**
     * 匹配本地账号，不存在则按配置自动开通。
     *
     * @param account DaxPay 账号
     * @param name    DaxPay 用户名称，用于昵称
     * @return 匹配结果（用户 + 是否本次新建）
     */
    public MatchResult matchOrCreate(String account, String name) {
        String username = convertUsername(account);
        AdminUserDO user = adminUserService.getUserByUsername(username);
        if (user != null) {
            return new MatchResult(user, false);
        }
        if (!properties.isAutoCreate()) {
            throw exception(SSO_DAXPAY_USER_CREATE_FAIL,
                    "本地不存在账号 " + username + "，且未开启自动创建（yudao.sso.daxpay.auto-create）");
        }
        return new MatchResult(createUser(username, nicknameOf(account, name)), true);
    }

    /**
     * 匹配结果
     *
     * @param user    匹配/新建到的本地用户
     * @param created 是否本次新建
     */
    public record MatchResult(AdminUserDO user, boolean created) {
    }

    /**
     * DaxPay 账号 → yudao 账号
     */
    public String convertUsername(String account) {
        String value = StrUtil.trimToEmpty(account);
        if (StrUtil.isEmpty(value)) {
            throw exception(SSO_DAXPAY_PARAM_INVALID, "DaxPay 账号为空，无法匹配本地账号");
        }
        if (YUDAO_USERNAME_PATTERN.matcher(value).matches()) {
            return value;
        }
        // 含非法字符或长度不够：用哈希保证确定性与唯一性
        return properties.getUsernameFallbackPrefix() + DigestUtil.sha256Hex(value).substring(0, 10);
    }

    private AdminUserDO createUser(String username, String nickname) {
        UserSaveReqVO reqVO = new UserSaveReqVO();
        reqVO.setUsername(username);
        reqVO.setNickname(nickname);
        // 该密码只为满足建号校验，用户永远不会用它登录（登录只走 DaxPay）
        reqVO.setPassword(RandomUtil.randomString(properties.getRandomPasswordLength()));
        Long userId = adminUserService.createUser(reqVO);
        // 分配默认角色，否则登录后后台没有任何菜单
        if (CollUtil.isNotEmpty(properties.getDefaultRoleIds())) {
            permissionService.assignUserRole(userId, new HashSet<>(properties.getDefaultRoleIds()));
        } else {
            log.warn("[createUser][账号({}) 已自动开通，但未配置 default-role-ids，登录后无任何菜单权限]", username);
        }
        log.info("[createUser][由 DaxPay 身份自动开通 yudao 账号({})，userId({})]", username, userId);
        return adminUserService.getUser(userId);
    }

    private String nicknameOf(String account, String name) {
        String nickname = StrUtil.blankToDefault(name, account);
        return StrUtil.maxLength(nickname, NICKNAME_MAX_LENGTH);
    }

}
