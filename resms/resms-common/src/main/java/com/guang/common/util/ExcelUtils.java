package com.guang.common.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.guang.common.exception.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 导出工具类 (基于 EasyExcel)
 * 遵循 RESMS 架构规约：核心功能下沉至 resms-common
 */
public class ExcelUtils {

    /**
     * 常规列表导出 (非模板)
     *
     * @param response  HTTP 响应
     * @param fileName  导出文件名 (不含扩展名)
     * @param sheetName Sheet 名称
     * @param clazz     数据模型类 (带 @ExcelProperty 注解)
     * @param data      数据列表
     */
    public static <T> void exportExcel(HttpServletResponse response, String fileName, String sheetName, Class<T> clazz, List<T> data) {
        try {
            setExportResponseHeader(response, fileName);
            EasyExcel.write(response.getOutputStream(), clazz)
                    .autoCloseStream(Boolean.FALSE)
                    .registerWriteHandler(getSimpleStyleStrategy())
                    .sheet(sheetName)
                    .doWrite(data);
        } catch (IOException e) {
            throw new ApiException("Excel 导出失败：" + e.getMessage());
        }
    }

    /**
     * 基于模板的填充导出
     *
     * @param response     HTTP 响应
     * @param fileName     导出文件名 (不含扩展名)
     * @param templatePath 模板在 resources 下的路径 (如 "templates/export/house_tpl.xlsx")
     * @param data         待填充的数据列表
     */
    public static <T> void exportTemplate(HttpServletResponse response, String fileName, String templatePath, List<T> data) {
        try {
            setExportResponseHeader(response, fileName);
            InputStream templateInputStream = new ClassPathResource(templatePath).getInputStream();
            
            EasyExcel.write(response.getOutputStream())
                    .withTemplate(templateInputStream)
                    .autoCloseStream(Boolean.FALSE)
                    .sheet()
                    .doWrite(data);
        } catch (IOException e) {
            throw new ApiException("Excel 模板导出失败：" + e.getMessage());
        }
    }

    /**
     * 模板填充导出（单对象）—— 填充模板中的 {var} 占位符
     * <p>
     * 模板中写 {@code {name}}、{@code {date}} 等占位符，传入 data 对象后自动替换。
     * 适用于导入模板预填默认值、合同/报告单次导出等场景。
     *
     * @param response     HTTP 响应
     * @param fileName     导出文件名 (不含扩展名)
     * @param templatePath 模板在 resources 下的路径 (如 "templates/import/house_tpl.xlsx")
     * @param data         填充数据对象（Map 或 @ExcelProperty 注解的 VO）
     */
    public static <T> void exportTemplateFill(HttpServletResponse response, String fileName, String templatePath, T data) {
        try {
            setExportResponseHeader(response, fileName);
            InputStream templateInputStream = new ClassPathResource(templatePath).getInputStream();

            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream())
                    .withTemplate(templateInputStream)
                    .autoCloseStream(Boolean.FALSE)
                    .build();
            WriteSheet writeSheet = EasyExcel.writerSheet().build();
            excelWriter.fill(data, writeSheet);
            excelWriter.finish();
        } catch (IOException e) {
            throw new ApiException("Excel 模板填充导出失败：" + e.getMessage());
        }
    }

    /**
     * 模板填充导出（数据列表）—— 填充模板中的 {.var} 循环占位符
     * <p>
     * 模板中写 {@code {.name}}、{@code {.date}} 等列表占位符，传入 List 后自动生成多行。
     * 适用于导入模板预填多条示例数据、批量导出等场景。
     *
     * @param response     HTTP 响应
     * @param fileName     导出文件名 (不含扩展名)
     * @param templatePath 模板在 resources 下的路径
     * @param dataList     填充数据列表
     */
    public static <T> void exportTemplateListFill(HttpServletResponse response, String fileName, String templatePath, List<T> dataList) {
        try {
            setExportResponseHeader(response, fileName);
            InputStream templateInputStream = new ClassPathResource(templatePath).getInputStream();

            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream())
                    .withTemplate(templateInputStream)
                    .autoCloseStream(Boolean.FALSE)
                    .build();
            WriteSheet writeSheet = EasyExcel.writerSheet().build();
            excelWriter.fill(dataList, writeSheet);
            excelWriter.finish();
        } catch (IOException e) {
            throw new ApiException("Excel 模板列表填充导出失败：" + e.getMessage());
        }
    }

    /**
     * 模板填充导出（单对象 + 数据列表混合）—— 同时填充 {var} 和 {.var}
     * <p>
     * 适用于同一模板中既有单次占位符（标题、日期）又有循环行（数据列表）的场景。
     *
     * @param response     HTTP 响应
     * @param fileName     导出文件名 (不含扩展名)
     * @param templatePath 模板在 resources 下的路径
     * @param single       单次填充数据（{var}）
     * @param dataList     列表填充数据（{.var}）
     */
    public static <T, R> void exportTemplateFillWithList(HttpServletResponse response, String fileName, String templatePath, T single, List<R> dataList) {
        try {
            setExportResponseHeader(response, fileName);
            InputStream templateInputStream = new ClassPathResource(templatePath).getInputStream();

            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream())
                    .withTemplate(templateInputStream)
                    .autoCloseStream(Boolean.FALSE)
                    .build();
            WriteSheet writeSheet = EasyExcel.writerSheet().build();
            excelWriter.fill(single, writeSheet);
            excelWriter.fill(dataList, writeSheet);
            excelWriter.finish();
        } catch (IOException e) {
            throw new ApiException("Excel 模板混合填充导出失败：" + e.getMessage());
        }
    }

    /**
     * 设置导出响应头
     */
    private static void setExportResponseHeader(HttpServletResponse response, String fileName) throws IOException {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
        // 允许前端获取 filename (处理跨域)
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }

    /**
     * 获取基础样式策略 (可选)
     */
    private static HorizontalCellStyleStrategy getSimpleStyleStrategy() {
        // 内容策略
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        
        // 头策略
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        headWriteCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontHeightInPoints((short) 12);
        headWriteFont.setBold(true);
        headWriteCellStyle.setWriteFont(headWriteFont);
        
        return new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);
    }
}
