package cn.kmbeast.aop;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.em.RoleEnum;
import cn.kmbeast.service.UserService;
import cn.kmbeast.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * 接口鉴权保护切面
 */
@Aspect
@Component
public class ProtectorAspect {

    @Around("@annotation(cn.kmbeast.aop.Protector)")
    public Object auth(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("token");
        String requestUri = request.getRequestURI();

        // 关键修改：匹配完整路径中的特征部分
        // 包含 context-path + 新接口路径的完整匹配
        if (requestUri.contains("/api/personal-heath/v1.0/public/ai/generate-image") ||
                // 保留原有路径的兼容匹配
                requestUri.contains("/api/proxy/generate-image")) {
            System.out.println("匹配到免认证路径：" + requestUri); // 新增日志用于验证
            return proceedingJoinPoint.proceed();
        }

        // 以下原有逻辑不变...
        if (token == null) {
            return ApiResult.error("身份认证失败，请先登录");
        }
        Claims claims = JwtUtil.fromToken(token);
        if (claims == null) {
            return ApiResult.error("身份认证失败，请先登录");
        }
        Integer userId = claims.get("id", Integer.class);
        Integer roleId = claims.get("role", Integer.class);
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Protector protectorAnnotation = signature.getMethod().getAnnotation(Protector.class);
        if (protectorAnnotation == null) {
            return ApiResult.error("身份认证失败，请先登录");
        }
        String role = protectorAnnotation.role();
        if (!"".equals(role)) {
            if (!Objects.equals(RoleEnum.ROLE(Math.toIntExact(roleId)), role)) {
                return ApiResult.error("无操作权限");
            }
        }
        LocalThreadHolder.setUserId(userId, roleId);
        Object result = proceedingJoinPoint.proceed();
        LocalThreadHolder.clear();
        return result;
    }
}
