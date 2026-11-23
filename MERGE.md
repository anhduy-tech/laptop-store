# Merge duy + main (giữ phần tốt nhất)

Ngày merge: 2026-11-23

## Chiến lược

- **Nền: nhánh `duy`** — POS bán quầy, PricingEngine, HoldExpiryJob, SerialHoldAdmin, Campaign/Voucher đầy đủ, UI counter, migration V6.
- **Lấy thêm từ `main`**:
  - Spring Data repositories: `CartRepository`, `CartItemRepository`, `CustomerOrderRepository`, `OrderItemRepository`, `OrderStatusHistoryRepository`, `OrderDiscountLineRepository`, `PaymentRepository`, `InvoiceRepository`
  - `SalesSupport` (helper: mã đơn, upsert khách, finalize serial, record status, …)
  - Query bổ sung: `InventoryBalanceRepository.sumAvailableByVariantId` / `findById_VariantId`, `SerialItemRepository.findByOrderItem_IdIn`
  - `VoucherRedemptionRepository` gộp API của cả hai nhánh
  - File `.env` (`DB_PASSWORD=123456`, khớp `application.yml` của duy)

## Không đưa sang (cố ý)

- `CounterCheckoutService` / `CounterTx` của main (API mỏng hơn duy).
- `CartServiceImpl` / `OnlineOrderServiceImpl` / `OrderQueryServiceImpl` của main (duy dùng `EntityManager` + `PricingService`; chức năng online + giá tốt hơn hoặc tương đương).
- `SalesServicesTest` của main (constructor phụ thuộc service main, không compile với API duy). Giữ `PricingEngineTest` của duy.

## Gợi ý bước tiếp

1. Push thư mục này lên nhánh `duy`.
2. Mở PR từ `main` → `duy` (hoặc ngược lại tùy workflow): conflict sẽ ít vì đã gộp sẵn phần tốt.
3. (Tùy chọn) Refactor dần `CartServiceImpl` / `OrderQueryServiceImpl` sang dùng repository mới thay `EntityManager`.
4. (Tùy chọn) Gọi `SalesSupport.finalizeHeldSerial` / `generateOrderCode` từ `CounterTx` để bỏ code trùng.

## Kiểm tra nhanh sau khi pull

```bash
./mvnw -q test -Dtest=PricingEngineTest
./mvnw -q -DskipTests package
```
