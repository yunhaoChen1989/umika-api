package ca.umika.api.common.logging;

import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ModuleLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ModuleLoggingAspect.class);
    private static final Set<String> INTERNAL_MODULES = Set.of("common");

    @Around("""
            within(ca.umika.api..*) &&
            (
                @within(org.springframework.stereotype.Service) ||
                @within(org.springframework.web.bind.annotation.RestController)
            ) &&
            !within(ca.umika.api.common.logging..*) &&
            !within(ca.umika.api.common.web.ApiExceptionHandler)
            """)
    public Object logModuleCall(ProceedingJoinPoint joinPoint) throws Throwable {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        String module = resolveModule(targetClass);
        String method = resolveMethodName(joinPoint);
        long startedAt = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long durationMs = System.currentTimeMillis() - startedAt;
            if (shouldLog(module)) {
                log.info("module call completed module={} class={} method={} durationMs={}",
                        module, targetClass.getSimpleName(), method, durationMs);
            }
            return result;
        } catch (Throwable ex) {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.warn("module call failed module={} class={} method={} durationMs={} exception={} message={}",
                    module, targetClass.getSimpleName(), method, durationMs, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    private boolean shouldLog(String module) {
        return !INTERNAL_MODULES.contains(module);
    }

    private String resolveMethodName(ProceedingJoinPoint joinPoint) {
        if (joinPoint.getSignature() instanceof MethodSignature signature) {
            return signature.getName();
        }
        return joinPoint.getSignature().getName();
    }

    private String resolveModule(Class<?> targetClass) {
        Package classPackage = targetClass.getPackage();
        if (classPackage == null) {
            return "unknown";
        }
        String packageName = classPackage.getName();
        String prefix = "ca.umika.api.";
        if (!packageName.startsWith(prefix)) {
            return "unknown";
        }
        String remainder = packageName.substring(prefix.length());
        int dot = remainder.indexOf('.');
        return dot >= 0 ? remainder.substring(0, dot) : remainder;
    }
}
