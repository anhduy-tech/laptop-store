package com.lapstore.inventory.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.inventory.dto.StockRow;
import com.lapstore.inventory.dto.StockSerialsView;
import com.lapstore.inventory.dto.StocktakeItemRow;
import com.lapstore.inventory.dto.StocktakeRow;

@Service
class InventoryExcelServiceImpl implements InventoryExcelService {
    private static final DataFormatter FORMATTER = new DataFormatter();

    // ------------------------------------------------------------ đọc

    @Override
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public List<String> readSerials(InputStream in) {
        try (Workbook wb = WorkbookFactory.create(in)) {
            if (wb.getNumberOfSheets() == 0)
                throw new BusinessException("File Excel không có sheet nào.");
            Sheet sheet = wb.getSheetAt(0);
            int first = Math.max(sheet.getFirstRowNum(), 0);
            int column = 0;
            int start = first;
            Row head = sheet.getRow(first);
            if (head != null) {
                for (Cell c : head) {
                    String t = text(c).toLowerCase(Locale.ROOT);
                    if (t.contains("serial") || t.equals("imei") || t.contains("số máy")) {
                        column = c.getColumnIndex();
                        start = first + 1;
                        break;
                    }
                }
            }
            Set<String> out = new LinkedHashSet<>();
            for (int r = start; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null)
                    continue;
                String v = text(row.getCell(column));
                if (v.isEmpty())
                    continue;
                out.add(v);
                if (out.size() > MAX_SERIALS)
                    throw new BusinessException("File có quá " + MAX_SERIALS + " serial, hãy chia nhỏ file.");
            }
            if (out.isEmpty())
                throw new BusinessException("Không tìm thấy serial nào trong file (cột \"Serial\" hoặc cột A).");
            return List.copyOf(out);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new BusinessException("Không đọc được file Excel. Hãy dùng file .xlsx hoặc .xls, tải file mẫu nếu cần.");
        }
    }

    /** Giá trị ô dưới dạng chuỗi đã cắt khoảng trắng; số nguyên không bị đổi sang dạng khoa học (356938035643809). */
    private static String text(Cell c) {
        if (c == null)
            return "";
        CellType type = c.getCellType() == CellType.FORMULA ? c.getCachedFormulaResultType() : c.getCellType();
        if (type == CellType.NUMERIC && !org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(c)) {
            return BigDecimal.valueOf(c.getNumericCellValue()).stripTrailingZeros().toPlainString().trim();
        }
        return FORMATTER.formatCellValue(c).trim();
    }

    // ------------------------------------------------------------ mẫu

    @Override
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public byte[] serialTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Serial");
            CellStyle textStyle = wb.createCellStyle();
            textStyle.setDataFormat(wb.createDataFormat().getFormat("@")); // văn bản: không mất số 0 đầu, không thành 3.5E+14
            sheet.setDefaultColumnStyle(0, textStyle);
            header(wb, sheet, "Serial");
            String[] samples = { "SN-MAU-0001", "SN-MAU-0002", "SN-MAU-0003" };
            for (int i = 0; i < samples.length; i++)
                sheet.createRow(i + 1).createCell(0).setCellValue(samples[i]);
            sheet.setColumnWidth(0, 28 * 256);

            Sheet help = wb.createSheet("Hướng dẫn");
            String[] lines = {
                    "Cách dùng file mẫu",
                    "1. Điền mỗi serial một dòng ở cột A của sheet \"Serial\", bắt đầu từ dòng 2 (dòng 1 là tiêu đề).",
                    "2. Xóa các dòng mẫu SN-MAU-xxxx trước khi dùng.",
                    "3. Trong hệ thống bấm \"Nhập từ Excel\" ở ô serial rồi chọn file này.",
                    "4. Serial là duy nhất toàn hệ thống, tối đa 60 ký tự; dòng trống và serial lặp trong file được bỏ qua.",
                    "5. Tối đa " + MAX_SERIALS + " serial mỗi file. Cột A đã đặt kiểu văn bản để giữ nguyên số 0 đầu và số dài." };
            for (int i = 0; i < lines.length; i++)
                help.createRow(i).createCell(0).setCellValue(lines[i]);
            help.setColumnWidth(0, 110 * 256);
            return bytes(wb);
        } catch (IOException e) {
            throw new BusinessException("Không tạo được file mẫu.");
        }
    }

    // ------------------------------------------------------------ xuất

    @Override
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public byte[] exportStock(List<StockRow> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Tồn kho");
            header(wb, sheet, "Sản phẩm", "SKU", "Cấu hình", "Màu", "Chi nhánh", "Tồn", "Đang giữ", "Khả dụng",
                    "Ngưỡng cảnh báo", "Trạng thái bán");
            int r = 1;
            for (StockRow s : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s.productName());
                row.createCell(1).setCellValue(s.sku());
                row.createCell(2).setCellValue(s.variantName());
                row.createCell(3).setCellValue(s.colorName());
                row.createCell(4).setCellValue(s.branchName());
                row.createCell(5).setCellValue(s.onHand());
                row.createCell(6).setCellValue(s.held());
                row.createCell(7).setCellValue(s.available());
                row.createCell(8).setCellValue(s.lowStockThreshold());
                row.createCell(9).setCellValue("ACTIVE".equals(s.variantStatus()) ? "Đang bán" : "Chưa mở bán");
            }
            finish(sheet, 10, new int[] { 34, 24, 36, 16, 22, 8, 10, 10, 16, 16 });
            return bytes(wb);
        } catch (IOException e) {
            throw new BusinessException("Không tạo được file Excel.");
        }
    }

    @Override
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public byte[] exportSerials(StockSerialsView view, boolean withCost) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Cột A = "Serial", dòng 1 là tiêu đề: file xuất ra nhập lại được
            Sheet sheet = wb.createSheet("Serial");
            CellStyle textStyle = wb.createCellStyle();
            textStyle.setDataFormat(wb.createDataFormat().getFormat("@"));
            sheet.setDefaultColumnStyle(0, textStyle);
            if (withCost)
                header(wb, sheet, "Serial", "Trạng thái", "Nhập lúc", "Giá vốn");
            else
                header(wb, sheet, "Serial", "Trạng thái", "Nhập lúc");
            int r = 1;
            for (var s : view.serials()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s.serialNumber());
                row.createCell(1).setCellValue(s.status());
                row.createCell(2).setCellValue(s.receivedAt());
                if (withCost && s.costPrice() != null)
                    row.createCell(3).setCellValue(s.costPrice().doubleValue());
            }
            finish(sheet, withCost ? 4 : 3, withCost ? new int[] { 30, 14, 18, 16 } : new int[] { 30, 14, 18 });

            Sheet info = wb.createSheet("Thông tin");
            String[][] kv = { { "Sản phẩm", view.productName() }, { "SKU", view.sku() },
                    { "Cấu hình", view.variantName() }, { "Chi nhánh", view.branchName() },
                    { "Số dòng xuất", String.valueOf(view.serials().size()) } };
            for (int i = 0; i < kv.length; i++) {
                Row row = info.createRow(i);
                row.createCell(0).setCellValue(kv[i][0]);
                row.createCell(1).setCellValue(kv[i][1]);
            }
            info.setColumnWidth(0, 16 * 256);
            info.setColumnWidth(1, 50 * 256);
            return bytes(wb);
        } catch (IOException e) {
            throw new BusinessException("Không tạo được file Excel.");
        }
    }

    @Override
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public byte[] exportStocktake(StocktakeRow h, List<StocktakeItemRow> items) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Kết quả");
            CellStyle textStyle = wb.createCellStyle();
            textStyle.setDataFormat(wb.createDataFormat().getFormat("@"));
            sheet.setDefaultColumnStyle(0, textStyle);
            header(wb, sheet, "Serial", "Sản phẩm", "SKU", "Kết quả", "Hệ thống ghi", "Chi nhánh hệ thống", "Quét lúc",
                    "Đã điều chỉnh", "Ghi chú");
            int r = 1;
            for (StocktakeItemRow i : items) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(i.serialNumber());
                row.createCell(1).setCellValue(i.productName());
                row.createCell(2).setCellValue(i.sku());
                row.createCell(3).setCellValue(i.resultLabel());
                row.createCell(4).setCellValue(i.systemStatus());
                row.createCell(5).setCellValue(i.systemBranch());
                row.createCell(6).setCellValue(i.scannedAt());
                row.createCell(7).setCellValue(i.adjusted() ? "Có" : "");
                row.createCell(8).setCellValue(i.note() == null ? "" : i.note());
            }
            finish(sheet, 9, new int[] { 28, 34, 24, 12, 14, 22, 18, 14, 40 });

            Sheet sum = wb.createSheet("Tổng hợp");
            String[][] kv = { { "Mã phiên", h.code() }, { "Chi nhánh", h.branchName() }, { "Phạm vi", h.scope() },
                    { "Trạng thái", h.status() }, { "Bắt đầu", h.startedAt() + " (" + h.startedBy() + ")" },
                    { "Kết thúc", h.closedAt().isEmpty() ? "" : h.closedAt() + " (" + h.closedBy() + ")" },
                    { "Cần kiểm (theo hệ thống)", String.valueOf(h.expected()) },
                    { "Đã quét khớp", String.valueOf(h.scanned()) }, { "Thiếu", String.valueOf(h.missing()) },
                    { "Dư (không có trong danh sách chi nhánh)", String.valueOf(h.unexpected()) } };
            for (int i = 0; i < kv.length; i++) {
                Row row = sum.createRow(i);
                row.createCell(0).setCellValue(kv[i][0]);
                row.createCell(1).setCellValue(kv[i][1]);
            }
            sum.setColumnWidth(0, 40 * 256);
            sum.setColumnWidth(1, 50 * 256);
            return bytes(wb);
        } catch (IOException e) {
            throw new BusinessException("Không tạo được file Excel.");
        }
    }

    // ------------------------------------------------------------ tiện ích

    private static void header(Workbook wb, Sheet sheet, String... titles) {
        CellStyle st = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        st.setFont(f);
        st.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.LEFT);
        Row row = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(titles[i]);
            c.setCellStyle(st);
        }
    }

    /** Cố định dòng tiêu đề, bộ lọc tự động, độ rộng cột (đơn vị ký tự). */
    private static void finish(Sheet sheet, int columns, int[] widths) {
        sheet.createFreezePane(0, 1);
        if (sheet.getLastRowNum() >= 1)
            sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, columns - 1));
        for (int i = 0; i < widths.length; i++)
            sheet.setColumnWidth(i, widths[i] * 256);
    }

    private static byte[] bytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }
}
