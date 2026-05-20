package com.guang.common.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * MyBatis-Plus 代码生成器
 * 针对 RESMS 项目进行模块化生成
 */
public class MyBatisPlusGenerator {
//
//    // 数据库配置
//    private static final String URL = "jdbc:mysql://localhost:3306/resms?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf-8";
//    private static final String USERNAME = "root";
//    private static final String PASSWORD = "123456"; // 请确保数据库密码正确，或通过系统变量传入
//    private static final String AUTHOR = "blackDuck";
//
//    public static void main(String[] args) {
//        // 1. 定义模块与表的映射关系
//        Map<String, String[]> moduleTables = new HashMap<>();
//
//        // 系统模块
//        moduleTables.put("resms-system", new String[] {
//                "sys_dept", "sys_menu", "sys_notification", "sys_operation_log",
//                "sys_permission", "sys_role", "sys_role_menu", "sys_role_permission",
//                "sys_user", "sys_user_notification", "sys_user_role",
//                "tb_chat_message", "tb_chat_session", "tb_chat_session_member"
//        });
//
//        // 房产模块
//        moduleTables.put("resms-house", new String[] {
//                "tb_project", "tb_house", "tb_house_image", "tb_house_status_log",
//                "tb_new_house_extend", "tb_rent_house_extend", "tb_second_house_extend"
//        });
//
//        // 交易与客户模块
//        moduleTables.put("resms-trade", new String[] {
//                "tb_customer", "tb_transaction", "tb_user_browse_history", "tb_user_favorite"
//        });
//
//        // 财务模块
//        moduleTables.put("resms-finance", new String[] {
//                "tb_payment", "tb_commission"
//        });
//
//        // 2. 获取项目根路径 (假设在 resms 目录下运行)
//        // 注意：由于在 resms-common 模块中运行，我们需要回退到根目录
//        String projectRoot = System.getProperty("user.dir");
//        if (projectRoot.endsWith("resms-common")) {
//            projectRoot = projectRoot.substring(0, projectRoot.lastIndexOf("resms-common") - 1);
//        } else if (!projectRoot.endsWith("resms")) {
//            // 如果是在 xmG 目录下运行
//            projectRoot = projectRoot + "/resms";
//        }
//
//        final String finalProjectRoot = projectRoot;
//
//        // 3. 循环生成各模块代码
//        moduleTables.forEach((module, tables) -> {
//            String modulePath = finalProjectRoot + "/resms-modules/" + module;
//            String outputDir = modulePath + "/src/main/java";
//            String xmlDir = modulePath + "/src/main/resources/mapper";
//            String packageName = "com.guang." + module.replace("resms-", "");
//
//            System.out.println(">>> 正在为模块 [" + module + "] 生成代码...");
//
//            FastAutoGenerator.create(URL, USERNAME, PASSWORD)
//                    .globalConfig(builder -> {
//                        builder.author(AUTHOR)
//                                .enableSpringdoc() // 开启 SpringDoc (OpenAPI 3) 支持
//                                .outputDir(outputDir)
//                                .disableOpenDir();
//                    })
//                    .packageConfig(builder -> {
//                        builder.parent(packageName)
//                                .entity("entity")
//                                .service("service")
//                                .serviceImpl("service.impl")
//                                .mapper("mapper")
//                                .controller("controller")
//                                .pathInfo(Collections.singletonMap(OutputFile.xml, xmlDir));
//                    })
//                    .strategyConfig(builder -> {
//                        builder.addInclude(tables)
//                                .addTablePrefix("sys_", "tb_") // 移除前缀
//                                .entityBuilder()
//                                .enableLombok()
//                                .enableTableFieldAnnotation()
//                                .serviceBuilder()
//                                .formatServiceFileName("%sService")
//                                .controllerBuilder()
//                                .enableRestStyle();
//                    })
//                    .templateEngine(new VelocityTemplateEngine())
//                    .execute();
//        });
//
//        System.out.println(">>> 所有模块代码生成完成！");
//    }
}
