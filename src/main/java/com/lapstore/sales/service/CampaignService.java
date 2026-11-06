package com.lapstore.sales.service;

import java.util.List;

import com.lapstore.sales.dto.CampaignRequest;
import com.lapstore.sales.dto.CampaignRow;
import com.lapstore.sales.dto.VoucherRow;

/** Campaign và voucher (WH-03). */
public interface CampaignService {
    List<CampaignRow> campaigns();

    List<VoucherRow> vouchers();

    void createCampaign(CampaignRequest request, String username);
}
