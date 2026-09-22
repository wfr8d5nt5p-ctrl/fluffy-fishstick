package com.sky.controller.admin;

import com.sky.context.BaseContext;
import com.sky.mapper.StoreMapper;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;

/**
 * 管理端 - 数据统计相关接口
 */
@RestController("adminReportController")
@RequestMapping("/admin/report")
@Api(tags = "数据统计相关接口")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private StoreMapper storeMapper;

    /**
     * 营业额统计
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("营业额统计：{} ~ {}", begin, end);
        return Result.success(reportService.getTurnoverStatistics(begin, end));
    }

    /**
     * 用户统计（新增用户 + 累计用户）
     */
    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("用户统计：{} ~ {}", begin, end);
        return Result.success(reportService.getUserStatistics(begin, end));
    }

    /**
     * 订单统计
     */
    @GetMapping("/orderStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> orderStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("订单统计：{} ~ {}", begin, end);
        return Result.success(reportService.getOrderStatistics(begin, end));
    }

    /**
     * 导出运营数据报表（Excel，按店铺隔离：平台看全平台，商家看自家店）
     */
    @GetMapping("/export")
    @ApiOperation("导出运营数据")
    public void exportBusinessData(
            @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate end,
            HttpServletResponse response) throws IOException {
        if (begin == null) begin = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();
        log.info("导出运营数据：{} ~ {}", begin, end);

        // ===== 1. 取数（均已按当前店铺隔离） =====
        TurnoverReportVO turnover = reportService.getTurnoverStatistics(begin, end);
        UserReportVO user = reportService.getUserStatistics(begin, end);
        OrderReportVO order = reportService.getOrderStatistics(begin, end);
        List<SalesTop10ReportVO> top10 = reportService.getSalesTop10(begin, end);

        String[] dateLs = turnover.getDateList().split(",");
        String[] turnLs = turnover.getTurnoverList().split(",");
        String[] newLs = user.getNewUserList().split(",");
        String[] totalLs = user.getTotalUserList().split(",");
        String[] orderLs = order.getOrderCountList().split(",");
        String[] validLs = order.getValidOrderCountList().split(",");
        String[] doneLs = order.getCompletedOrderCountList().split(",");
        String[] cancelLs = order.getCancelOrderCountList().split(",");

        Long storeId = BaseContext.getCurrentStoreId();
        String scopeName = (storeId == null ? "全平台" : storeMapper.getById(storeId).getName()) + "（" + (storeId == null ? "平台" : "本店") + "）";

        // ===== 2. 组装 Excel =====
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("运营数据报表");

        Font bold = wb.createFont();
        bold.setBold(true);
        bold.setColor(IndexedColors.WHITE.getIndex());
        CellStyle header = wb.createCellStyle();
        header.setFont(bold);
        header.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        header.setFillPattern(CellStyle.SOLID_FOREGROUND);

        Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(IndexedColors.ORANGE.getIndex());
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(titleFont);

        int rowIdx = 0;
        // 标题
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
        XSSFRow t0 = sheet.createRow(rowIdx++);
        t0.createCell(0).setCellValue("蛋筒到家 · 运营数据报表");
        t0.getCell(0).setCellStyle(titleStyle);
        for (int c = 1; c <= 8; c++) t0.createCell(c);
        // 统计口径 + 区间
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 8));
        XSSFRow t1 = sheet.createRow(rowIdx++);
        t1.createCell(0).setCellValue("统计范围：" + scopeName + "       时间：" + begin + " ~ " + end);
        for (int c = 1; c <= 8; c++) t1.createCell(c);

        // 表头
        String[] cols = {"日期", "营业额(元)", "新增用户", "累计用户", "订单总数", "有效订单", "已完成", "已取消", "订单完成率"};
        XSSFRow head = sheet.createRow(rowIdx++);
        for (int c = 0; c < cols.length; c++) {
            head.createCell(c).setCellValue(cols[c]);
            head.getCell(c).setCellStyle(header);
        }

        // 明细
        double turnSum = 0;
        int newSum = 0, doneSum = 0, cancelSum = 0;
        String totalUserEnd = totalLs.length > 0 ? totalLs[totalLs.length - 1] : "0";
        for (int i = 0; i < dateLs.length; i++) {
            double turn = parseDouble(turnLs, i);
            int oCount = parseInt(orderLs, i), vCount = parseInt(validLs, i);
            int done = parseInt(doneLs, i), cancel = parseInt(cancelLs, i);
            double rate = oCount == 0 ? 0.0 : (double) done / oCount;
            turnSum += turn;
            newSum += parseInt(newLs, i);
            doneSum += done;
            cancelSum += cancel;

            org.apache.poi.xssf.usermodel.XSSFRow r = sheet.createRow(rowIdx++);
            r.createCell(0).setCellValue(dateLs[i]);
            r.createCell(1).setCellValue(turn);
            r.createCell(2).setCellValue(parseInt(newLs, i));
            r.createCell(3).setCellValue(parseInt(totalLs, i));
            r.createCell(4).setCellValue(oCount);
            r.createCell(5).setCellValue(vCount);
            r.createCell(6).setCellValue(done);
            r.createCell(7).setCellValue(cancel);
            r.createCell(8).setCellValue(String.format("%.2f%%", rate * 100));
        }

        // 合计行
        double totalRate = order.getTotalOrderCount() == 0 ? 0.0 : (double) order.getValidOrderCount() / order.getTotalOrderCount();
        org.apache.poi.xssf.usermodel.XSSFRow sum = sheet.createRow(rowIdx);
        sum.createCell(0).setCellValue("合计");
        sum.createCell(1).setCellValue(turnSum);
        sum.createCell(2).setCellValue(newSum);
        sum.createCell(3).setCellValue(totalUserEnd);
        sum.createCell(4).setCellValue(order.getTotalOrderCount());
        sum.createCell(5).setCellValue(order.getValidOrderCount());
        sum.createCell(6).setCellValue(doneSum);
        sum.createCell(7).setCellValue(cancelSum);
        sum.createCell(8).setCellValue(String.format("%.2f%%", totalRate * 100));

        for (int c = 0; c <= 8; c++) sheet.setColumnWidth(c, 14 * 256);

        // 销量 Top10
        XSSFSheet s2 = wb.createSheet("销量排名Top10");
        String[] h2 = {"排名", "菜品名称", "销量"};
        org.apache.poi.xssf.usermodel.XSSFRow hrow = s2.createRow(0);
        for (int c = 0; c < h2.length; c++) {
            hrow.createCell(c).setCellValue(h2[c]);
            hrow.getCell(c).setCellStyle(header);
        }
        if (top10 == null || top10.isEmpty()) {
            s2.createRow(1).createCell(0).setCellValue("该时间段暂无销量数据");
        } else {
            int idx = 1;
            for (SalesTop10ReportVO vo : top10) {
                org.apache.poi.xssf.usermodel.XSSFRow r = s2.createRow(idx);
                r.createCell(0).setCellValue(idx);
                r.createCell(1).setCellValue(vo.getName());
                r.createCell(2).setCellValue(vo.getNumber());
                idx++;
            }
        }
        s2.setColumnWidth(0, 8 * 256);
        s2.setColumnWidth(1, 30 * 256);
        s2.setColumnWidth(2, 12 * 256);

        // ===== 3. 写出 =====
        String fileName = "蛋筒到家运营数据报表_" + begin + "_" + end + ".xlsx";
        fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        wb.write(response.getOutputStream());
        wb.close();
    }

    private double parseDouble(String[] arr, int i) {
        try { return arr != null && i < arr.length && !arr[i].isEmpty() ? Double.parseDouble(arr[i]) : 0.0; }
        catch (NumberFormatException e) { return 0.0; }
    }

    private int parseInt(String[] arr, int i) {
        try { return arr != null && i < arr.length && !arr[i].isEmpty() ? Integer.parseInt(arr[i]) : 0; }
        catch (NumberFormatException e) { return 0; }
    }
}