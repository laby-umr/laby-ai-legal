package com.laby.module.ai.tool.function;

import com.laby.framework.common.util.object.BeanUtils;
import com.laby.framework.security.core.LoginUser;
import com.laby.framework.security.core.util.SecurityFrameworkUtils;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.system.api.user.AdminUserApi;
import com.laby.module.system.api.user.dto.AdminUserRespDTO;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工具：用户信息查询
 *
 * 同时，也是展示 ToolContext 上下文的使用
 *
 * @author Ren
 */
@Component("user_profile_query")
public class UserProfileQueryToolFunction {

    @Resource
    private AdminUserApi adminUserApi;

    @Data
    @JsonClassDescription("用户信息查询")
    public static class Request {

        /**
         * 用户编号
         */
        @JsonPropertyDescription("用户编号，例如说：1。如果查询自己，则 id 为空")
        private Long id;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        /**
         * 用户ID
         */
        private Long id;
        /**
         * 用户昵称
         */
        private String nickname;

        /**
         * 手机号码
         */
        private String mobile;
        /**
         * 用户头像
         */
        private String avatar;

    }

    @Tool(name = "user_profile_query", description = "用户信息查询")
    public Response queryUserProfile(
            @ToolParam(name = "id", description = "用户编号，例如说：1。如果查询自己，则 id 为空") Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return new Response();
        }
        if (id == null) {
            LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
            if (loginUser == null) {
                return new Response();
            }
            id = loginUser.getId();
        }
        Long userId = id;
        return TenantUtils.execute(tenantId, () -> {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            return BeanUtils.toBean(user, Response.class);
        });
    }

}
