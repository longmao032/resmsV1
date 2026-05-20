package com.guang.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;

import java.util.Date;

/**
 * 业务编号生成工具类
 *
 * @author blackDuck
 */
public class CodeGeneratorUtil {

    /**
     * 生成项目编号: PJ + yyyyMMdd + 4位随机数
     */
    public static String generateProjectNo() {
        return "PJ" + DateUtil.format(new Date(), "yyyyMMdd") + RandomUtil.randomNumbers(4);
    }

    /**
     * 生成房源编号: H + yyyyMMdd + 4位随机数
     */
    public static String generateHouseNo() {
        return "H" + DateUtil.format(new Date(), "yyyyMMdd") + RandomUtil.randomNumbers(4);
    }

    /**
     * 生成客户编号: CUST + yyyyMMdd + 4位随机数
     */
    public static String generateCustomerNo() {
        return "CUST" + DateUtil.format(new Date(), "yyyyMMdd") + RandomUtil.randomNumbers(4);
    }

    /**
     * 生成交易编号: TR + yyyyMMdd + 6位随机数
     */
    public static String generateTransactionNo() {
        return "TR" + DateUtil.format(new Date(), "yyyyMMdd") + RandomUtil.randomNumbers(6);
    }

    /**
     * 生成收据编号: RCP + yyyyMMdd + 4位随机数
     */
    public static String generateReceiptNo() {
        return "RCP" + DateUtil.format(new Date(), "yyyyMMdd") + RandomUtil.randomNumbers(4);
    }

    /**
     * 生成过户编号: TG + yyyyMMdd + 4位随机数
     */
    public static String generateTransferNo() {
        return "TG" + DateUtil.format(new Date(), "yyyyMMdd") + RandomUtil.randomNumbers(4);
    }
}
