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
public class TransactionService {
    private final TransactionDao transactionDao;
    private final CategoryService categoryService;
    private final UserService userService;

    public TransactionService(TransactionDao transactionDao, CategoryService categoryService, UserService userService) {
        this.transactionDao = transactionDao;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    public List<Transaction> findRecent(int limit) {
        return transactionDao.findRecent(userService.currentUserId(), limit);
    }

    public Transaction findById(Long id) {
        return transactionDao.findById(userService.currentUserId(), id)
                .orElseThrow(() -> new IllegalArgumentException("收支记录不存在"));
    }

    public PageResult<Transaction> findPage(TransactionFilter filter) {
        Long userId = userService.currentUserId();
        long total = transactionDao.count(userId, filter);
        List<Transaction> items = transactionDao.findPage(userId, filter);
        int totalPages = (int) Math.ceil((double) total / filter.getSize());
        return new PageResult<>(items, filter.getPage(), filter.getSize(), total, totalPages);
    }

    public byte[] exportExcel(TransactionFilter filter) {
        List<Transaction> transactions = transactionDao.findForExport(userService.currentUserId(), filter);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("收支记录");
            String[] headers = {"日期", "类型", "分类", "金额", "备注", "创建时间"};
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
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
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("导出Excel失败", exception);
        }
    }

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

    public void delete(Long id) {
        if (transactionDao.delete(userService.currentUserId(), id) == 0) {
            throw new IllegalArgumentException("收支记录不存在");
        }
    }

    public MonthlyStats monthlyStats(YearMonth month) {
        return transactionDao.monthlyStats(userService.currentUserId(), month);
    }

    public List<CategoryStats> expenseStatsByCategory(YearMonth month) {
        return transactionDao.expenseStatsByCategory(userService.currentUserId(), month);
    }

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

    private CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.HAIR);
        return style;
    }

    private void writeCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void validate(Transaction transaction) {
        if (transaction.getCategoryId() == null) {
            throw new IllegalArgumentException("必须选择分类");
        }
        Category category = categoryService.findById(transaction.getCategoryId());
        if (transaction.getType() == null) {
            transaction.setType(category.getType());
        }
        if (!category.getType().equals(transaction.getType())) {
            throw new IllegalArgumentException("记录类型必须与分类类型一致");
        }
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        if (transaction.getRecordDate() == null) {
            transaction.setRecordDate(LocalDate.now());
        }
        if (StringUtils.hasText(transaction.getRemark()) && transaction.getRemark().length() > 200) {
            throw new IllegalArgumentException("备注不能超过200字");
        }
    }
}
