package com.lapstore.catalog.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lapstore.catalog.dto.ProductAdminRow;
import com.lapstore.catalog.dto.ProductEditRequest;
import com.lapstore.catalog.dto.ProductEditView;
import com.lapstore.catalog.dto.ReceiptCatalog;

/**
 * Quản lý thông tin sản phẩm cho nhân viên kho. Xem: quyền PORTAL_WAREHOUSE. Sửa: PRODUCT_MANAGE.
 * Giá KHÔNG sửa ở đây (BR-06): đi qua PriceHistoryService (đề xuất -> admin duyệt).
 */
public interface ProductAdminService {
    /** @param q khớp tên hoặc SKU; @param brand tên hãng; @param status ACTIVE | INACTIVE; null = không lọc */
    Page<ProductAdminRow> search(String q, String brand, String status, Pageable pageable);

    List<String> brandNames();

    /** @throws com.lapstore.common.exception.NotFoundException nếu không có cấu hình này */
    ProductEditView get(long variantId);

    /** Sửa sản phẩm (dùng chung mọi cấu hình của nó) và cấu hình này trong một transaction. */
    void update(long variantId, ProductEditRequest request);

    /** Danh mục cho màn nhập kho (hãng, danh mục, màu, sản phẩm, biến thể đã có). Quyền PORTAL_WAREHOUSE. */
    ReceiptCatalog receiptCatalog();
}
