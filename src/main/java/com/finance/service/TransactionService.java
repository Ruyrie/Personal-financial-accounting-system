package com.finance.service;

import com.finance.dao.TransactionDao;
import com.finance.dto.CategoryStats;
import com.finance.dto.MonthlyStats;
import com.finance.dto.PageResult;
import com.finance.dto.TransactionFilter;
import com.finance.entity.Category;
import com.finance.entity.Transaction;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
/**
 * 收支记录业务服务，负责记账记录的分页查询、保存校验、统计和 Excel 导出。
 */
public class TransactionService {
    private final TransactionDao transactionDao;
    private final CategoryService categoryService;
    private final UserService userService;

    public TransactionService(TransactionDao transactionDao, CategoryService categoryService, UserService userService) {
        this.transactionDao = transactionDao;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    /**
     * 查询当前用户最近的收支记录，用于首页最近记录模块。
     */
    public List<Transaction> findRecent(int limit) {
        return transactionDao.findRecent(userService.currentUserId(), limit);
    }

    /**
     * 查询当前用户指定收支记录，不存在时抛出业务异常。
     */
    public Transaction findById(Long id) {
        return transactionDao.findById(userService.currentUserId(), id)
                .orElseThrow(() -> new IllegalArgumentException("收支记录不存在"));
    }

    /**
     * 根据筛选条件查询当前用户的分页收支记录。
     */
    public PageResult<Transaction> findPage(TransactionFilter filter) {
        Long userId = userService.currentUserId();
        long total = transactionDao.count(userId, filter);
        List<Transaction> items = transactionDao.findPage(userId, filter);
        int totalPages = (int) Math.ceil((double) total / filter.getSize());
        return new PageResult<>(items, filter.getPage(), filter.getSize(), total, totalPages);
    }

    /**
     * 按当前筛选条件生成 Excel 文件字节数组。
     */
    public byte[] exportExcel(TransactionFilter filter) {
        // 导出复用列表页筛选条件，但不分页，确保导出的数据范围和页面筛选一致。
        List<Transaction> transactions = transactionDao.findForExport(userService.currentUserId(), filter);
        // try-with-resources 自动关闭 Workbook 和输出流，避免文件资源泄漏。
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            // 根据收入/支出/全部生成不同工作表名称。
            Sheet sheet = workbook.createSheet(exportSheetName(filter));
            String[] headers = {"日期", "类型", "分类", "金额", "备注", "创建时间"};
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);
            // 第 0 行写表头。
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            // 从第 1 行开始逐条写入收支记录。
            for (int i = 0; i < transactions.size(); i++) {
                Transaction transaction = transactions.get(i);
                Row row = sheet.createRow(i + 1);
                writeCell(row, 0, transaction.getRecordDate().toString(), bodyStyle);
                writeCell(row, 1, transaction.getType() == 1 ? "收入" : "支出", bodyStyle);
                writeCell(row, 2, transaction.getCategoryName(), bodyStyle);
                writeCell(row, 3, transaction.getAmount().toPlainString(), bodyStyle);
                writeCell(row, 4, transaction.getRemark() == null ? "" : transaction.getRemark(), bodyStyle);
                writeCell(row, 5, transaction.getCreatedAt().toString(), bodyStyle);
            }
            // 根据内容自动调整列宽，让下载后的 Excel 更易读。
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            // 将 Workbook 写入内存字节流，Controller 再把字节流返回给浏览器下载。
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("导出Excel失败", exception);
        }
    }

    /**
     * 新增或更新收支记录，并在保存前执行业务校验。
     */
    public Transaction save(Transaction transaction) {
        validate(transaction);
        Long userId = userService.currentUserId();
        if (transaction.getId() == null) {
            Transaction created = transactionDao.create(userId, transaction);
            return findById(created.getId());
        }
        if (transactionDao.update(userId, transaction) == 0) {
            throw new IllegalArgumentException("收支记录不存在");
        }
        return findById(transaction.getId());
    }

    /**
     * 删除当前用户的一条收支记录。
     */
    public void delete(Long id) {
        if (transactionDao.delete(userService.currentUserId(), id) == 0) {
            throw new IllegalArgumentException("收支记录不存在");
        }
    }

    /**
     * 统计指定月份的收入、支出和结余。
     */
    public MonthlyStats monthlyStats(YearMonth month) {
        return transactionDao.monthlyStats(userService.currentUserId(), month);
    }

    /**
     * 统计指定月份的支出分类占比。
     */
    public List<CategoryStats> expenseStatsByCategory(YearMonth month) {
        return transactionDao.expenseStatsByCategory(userService.currentUserId(), month);
    }

    /**
     * 统计指定月份的收入分类占比。
     */
    public List<CategoryStats> incomeStatsByCategory(YearMonth month) {
        return transactionDao.incomeStatsByCategory(userService.currentUserId(), month);
    }

    /**
     * 创建 Excel 表头样式。
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * 根据导出类型选择 Excel 工作表名称。
     */
    private String exportSheetName(TransactionFilter filter) {
        if (Integer.valueOf(1).equals(filter.getType())) {
            return "收入情况";
        }
        if (Integer.valueOf(2).equals(filter.getType())) {
            return "支出情况";
        }
        return "总体收支";
    }

    /**
     * 创建 Excel 内容行样式。
     */
    private CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.HAIR);
        return style;
    }

    /**
     * 向 Excel 行写入一个文本单元格。
     */
    private void writeCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * 校验收支记录的分类、类型、金额、日期和备注长度。
     */
    private void validate(Transaction transaction) {
        // 分类是记账记录的必填项，没有分类无法进行分类统计。
        if (transaction.getCategoryId() == null) {
            throw new IllegalArgumentException("必须选择分类");
        }
        // 通过当前用户查询分类，顺便保证不能使用其他用户的分类。
        Category category = categoryService.findById(transaction.getCategoryId());
        if (transaction.getType() == null) {
            // 如果前端没有传类型，则以分类类型作为记录类型。
            transaction.setType(category.getType());
        }
        if (!category.getType().equals(transaction.getType())) {
            // 防止“支出记录选择收入分类”这种错误数据进入数据库。
            throw new IllegalArgumentException("记录类型必须与分类类型一致");
        }
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            // 金额必须为正数，0 或负数没有记账意义。
            throw new IllegalArgumentException("金额必须大于0");
        }
        if (transaction.getRecordDate() == null) {
            // 未选择日期时默认使用当天，降低表单填写成本。
            transaction.setRecordDate(LocalDate.now());
        }
        if (StringUtils.hasText(transaction.getRemark()) && transaction.getRemark().length() > 200) {
            // 控制备注长度，和数据库 remark VARCHAR(200) 保持一致。
            throw new IllegalArgumentException("备注不能超过200字");
        }
    }
}
