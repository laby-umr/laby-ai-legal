package com.laby.framework.banner.core;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.util.ClassUtils;

import java.util.concurrent.TimeUnit;

/**
 * 项目启动成功后，提供文档相关的地址
 *
 * @author 芋道源码
 */
@Slf4j
public class BannerApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(1, TimeUnit.SECONDS); // 延迟 1 秒，保证输出到结尾
            log.info("\n----------------------------------------------------------\n\t" +
                            "项目启动成功！\n\t" +
                            "接口文档: \t{} \n\t" +
                            "开发文档: \t{} \n\t" +
                            "视频教程: \t{} \n" +
                            "----------------------------------------------------------",
                    "https://doc.iocoder.cn/api-doc/",
                    "https://doc.iocoder.cn",
                    "https://t.zsxq.com/02Yf6M7Qn");

            // 数据报表
            if (isNotPresent("com.laby.module.report.framework.security.config.SecurityConfiguration")) {
                System.out.println("[报表模块 laby-module-report - 已禁用][参考 https://doc.iocoder.cn/report/ 开启]");
            }
            // 工作流
            if (isNotPresent("com.laby.module.bpm.framework.flowable.config.BpmFlowableConfiguration")) {
                System.out.println("[工作流模块 laby-module-bpm - 已禁用][参考 https://doc.iocoder.cn/bpm/ 开启]");
            }
            // 商城系统
            if (isNotPresent("com.laby.module.trade.framework.web.config.TradeWebConfiguration")) {
                System.out.println("[商城系统 laby-module-mall - 已禁用][参考 https://doc.iocoder.cn/mall/build/ 开启]");
            }
            // ERP 系统
            if (isNotPresent("com.laby.module.erp.framework.web.config.ErpWebConfiguration")) {
                System.out.println("[ERP 系统 laby-module-erp - 已禁用][参考 https://doc.iocoder.cn/erp/build/ 开启]");
            }
            // WMS 仓库管理系统
            if (isNotPresent("com.laby.module.wms.framework.web.config.WmsWebConfiguration")) {
                System.out.println("[WMS 仓库管理系统 laby-module-wms - 已禁用][参考 https://doc.iocoder.cn/wms/build/ 开启]");
            }
            // CRM 系统
            if (isNotPresent("com.laby.module.crm.framework.web.config.CrmWebConfiguration")) {
                System.out.println("[CRM 系统 laby-module-crm - 已禁用][参考 https://doc.iocoder.cn/crm/build/ 开启]");
            }
            // MES 系统
            if (isNotPresent("com.laby.module.mes.framework.web.config.MesWebConfiguration")) {
                System.out.println("[MES 系统 laby-module-mes - 已禁用][参考 https://doc.iocoder.cn/mes/build/ 开启]");
            }
            // 微信公众号
            if (isNotPresent("com.laby.module.mp.framework.mp.config.MpConfiguration")) {
                System.out.println("[微信公众号 laby-module-mp - 已禁用][参考 https://doc.iocoder.cn/mp/build/ 开启]");
            }
            // 支付平台
            if (isNotPresent("com.laby.module.pay.framework.pay.config.PayConfiguration")) {
                System.out.println("[支付系统 laby-module-pay - 已禁用][参考 https://doc.iocoder.cn/pay/build/ 开启]");
            }
            // AI 大模型
            if (isNotPresent("com.laby.module.ai.framework.web.config.AiWebConfiguration")) {
                System.out.println("[AI 大模型 laby-module-ai - 已禁用][参考 https://doc.iocoder.cn/ai/build/ 开启]");
            }
            // IoT 物联网
            if (isNotPresent("com.laby.module.iot.framework.web.config.IotWebConfiguration")) {
                System.out.println("[IoT 物联网 laby-module-iot - 已禁用][参考 https://doc.iocoder.cn/iot/build/ 开启]");
            }
            // IM 即时通讯
            if (isNotPresent("com.laby.module.im.framework.web.config.ImWebConfiguration")) {
                System.out.println("[IM 即时通讯 laby-module-im - 已禁用][参考 https://doc.iocoder.cn/im/build/ 开启]");
            }
        });
    }

    private static boolean isNotPresent(String className) {
        return !ClassUtils.isPresent(className, ClassUtils.getDefaultClassLoader());
    }

}
