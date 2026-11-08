package com.lapstore.catalog.service;

import com.lapstore.catalog.dto.NewProductSpec;
import com.lapstore.catalog.dto.ResolvedVariant;
import com.lapstore.catalog.dto.VariantSpec;

/**
 * Tạo sản phẩm / biến thể khi nhập hàng. Chạy chung transaction với phiếu nhập (REQUIRED) nên phiếu lỗi thì
 * sản phẩm vừa tạo cũng rollback. Tạo mới cần quyền PRODUCT_MANAGE; chỉ tìm biến thể đã có thì không cần.
 */
public interface VariantProvisioningService {
    /** @return id sản phẩm mới (status ACTIVE). Trùng tên trong cùng hãng -> BusinessException. */
    long createProduct(NewProductSpec spec);

    /**
     * Tìm biến thể theo (sản phẩm, màu, CPU, RAM, ổ cứng); chưa có thì tạo với status INACTIVE kèm một
     * đề xuất giá PENDING (BR-06: giá bán đầu tiên cũng phải được admin duyệt, duyệt xong biến thể mới tự bán).
     *
     * @param priceReason lý do ghi vào đề xuất giá
     */
    ResolvedVariant findOrCreateVariant(VariantSpec spec, String priceReason, String username);
}
