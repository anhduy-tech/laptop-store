package com.lapstore.sales.service;

import java.util.List;

import com.lapstore.sales.dto.CampaignRequest;
import com.lapstore.sales.dto.CampaignRow;
import com.lapstore.sales.dto.ScopeOptions;
import com.lapstore.sales.dto.VoucherRequest;
import com.lapstore.sales.dto.VoucherRow;

/** Quản lý campaign và voucher (WH-03). Quyền CAMPAIGN_MANAGE (campaign) / VOUCHER_MANAGE (voucher). */
public interface CampaignService {
    List<CampaignRow> campaigns();

    List<VoucherRow> vouchers();

    ScopeOptions scopeOptions();

    void createCampaign(CampaignRequest request, String username);

    /** Tạm dừng (CANCELLED) hoặc bật lại (ACTIVE/SCHEDULED theo thời gian). Campaign đã ENDED thì không bật lại. */
    void setCampaignActive(long id, boolean active, String username);

    void createVoucher(VoucherRequest request, String username);

    /** Tạm dừng (CLOSED) hoặc bật lại (ACTIVE). Lượt đã giữ cho đơn đang mở vẫn được chốt. */
    void setVoucherActive(long id, boolean active, String username);
}
