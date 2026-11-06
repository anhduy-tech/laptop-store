package com.lapstore.catalog.service;

import java.util.List;

import com.lapstore.catalog.dto.ProductCard;
import com.lapstore.catalog.dto.ProductDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Tra cứu danh mục cho khách (SH-01, SH-02). Chỉ trả sản phẩm đang bán. */
public interface CatalogService {
    /** @param q từ khóa tên (có thể rỗng); @param brand tên hãng (rỗng = tất cả) */
    Page<ProductCard> searchProducts(String q, String brand, Pageable pageable);

    List<String> brandNames();

    /** @throws com.lapstore.common.exception.NotFoundException nếu không có hoặc đã ngừng bán */
    ProductDetail getProduct(long productId);
}
