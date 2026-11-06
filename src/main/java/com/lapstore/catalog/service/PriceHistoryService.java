package com.lapstore.catalog.service;

import java.util.List;

import com.lapstore.catalog.dto.PriceProposalRequest;
import com.lapstore.catalog.dto.PriceProposalView;

/**
 * Đề xuất / duyệt giá (mục 4.6). Cài đặt nên có
 * {@code @PreAuthorize("hasAuthority('PRICE_PROPOSE')")} cho propose và PRICE_APPROVE cho approve/reject,
 * và kiểm tra khoảng giá không chồng nhau dưới khóa theo variant_id.
 */
public interface PriceHistoryService {
    void propose(PriceProposalRequest request, String username);

    List<PriceProposalView> pending();

    int pendingCount();

    void approve(long proposalId, String username);

    void reject(long proposalId, String username);
}
