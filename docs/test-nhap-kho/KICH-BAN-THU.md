# Kịch bản thử: nhập kho, Excel, chuyển kho, kiểm kê

Bộ dữ liệu bám đúng dữ liệu mẫu V2 (chi nhánh, nhà cung cấp, sản phẩm, serial có sẵn) nên bạn **không cần tự tìm dữ liệu**: mọi serial, giá, tên cần gõ đều ghi sẵn bên dưới (có thể copy). Các file Excel nằm trong thư mục `data/`.

## 0. Chuẩn bị

| Việc | Chi tiết |
|---|---|
| Chạy ứng dụng | `npm run css:build`, rồi `./mvnw spring-boot:run` (Flyway áp V3, V4, V5; lần đầu `SpecBootstrap` nạp danh mục CPU/GPU/RAM/ổ cứng từ biến thể có sẵn) |
| Tài khoản kho | `kho01` / `Lap@12345` → http://localhost:8083/staff/login |
| Tài khoản admin | `admin` / `Admin@123` → http://localhost:8084/admin/login (duyệt giá). Dùng cửa sổ ẩn danh riêng vì cookie phiên dùng chung giữa các cổng |
| Tài khoản bán hàng | `sales01` / `Lap@12345` (chỉ dùng thử phân quyền) |
| Quy ước dữ liệu thử | Serial bắt đầu bằng `TEST-`; **ghi chú phiếu nhập bắt đầu bằng `TEST`**; tên sản phẩm mới bắt đầu bằng `TEST `; phạm vi phiên kiểm kê bắt đầu bằng `TEST`. Nhờ vậy `data/cleanup-test-data.sql` dọn sạch được |
| Thứ tự | Nhóm A → B → C → D → E → F. Nhóm F (kiểm kê) dùng Kho tổng Long Biên, nhóm A–E **không** nhập vào kho này nên Kho tổng vẫn đúng 9 máy |

**Dữ liệu có sẵn dùng trong kịch bản (từ V2)**

| Chi nhánh | Nội dung liên quan |
|---|---|
| Lapstore Cầu Giấy | `SWIFTGO14-I5-16-512`: serial 001 đã bán, **002–006 còn hàng** (tồn 5) |
| Lapstore Hà Đông | `ROG-G16-I7-16-1T-002` và `KATANA15-I7-16-512-001` **đang bị giữ** theo serial; `ROG-G16-I7-16-512-001` đã bán |
| Kho tổng Long Biên | `E14G6-U7-16-1T-001…006` (6 máy) và `PROART16-U9-32-1T-001…003` (3 máy), **cả 9 còn hàng, không bị giữ** |

Nhà cung cấp: FPT Trading, Synnex FPT, Digiworld. Màu: Bạc, Đen, Xám không gian, Xanh đêm.

Mỗi kịch bản ghi **Dữ liệu nhập → Các bước → Kết quả mong đợi**. Chỗ ghi *"phiếu PN…"* là mã tự sinh dạng `PNyyyyMMdd-HHmmss-nnn`.

---

## A. Nhập hàng cơ bản (sản phẩm và biến thể đã có)

### A1. Nhập 5 máy vào biến thể đã có
**Dữ liệu**
- Nhà cung cấp `Synnex FPT`, chi nhánh `Lapstore Cầu Giấy`, ghi chú `TEST A1 nhập 5 máy Swift Go`
- Sản phẩm có sẵn: `Acer – Acer Swift Go 14` · Màu: **Bạc** · CPU: **Intel Core i5-13500H** · GPU: **Intel Iris Xe** · RAM **16 GB** · Ổ cứng **512 GB**
- Giá vốn `15900000`; serial:
```
TEST-SG14-001
TEST-SG14-002
TEST-SG14-003
TEST-SG14-004
TEST-SG14-005
```
(hoặc nạp bằng file `data/nhap-kho-sg14-5-may.xlsx` ở kịch bản D2)

**Các bước:** Nhập kho → chọn sản phẩm → tick các ô trên → dán serial vào dòng sinh ra → *Xác nhận nhập kho*.

**Mong đợi**
- Dòng sinh ra có nhãn xanh **"Đã có · SWIFTGO14-I5-16-512"**, ô giá bán bị khóa ("Giữ giá hiện tại").
- Thông báo: `Đã nhập kho phiếu PN…: 5 máy.` (không có dòng "Tạo … mới").
- Tồn kho (`/staff/warehouse/stock`, tìm `SWIFTGO14`): Cầu Giấy **5 → 10**. Serial `TEST-SG14-001…005` ở trạng thái `IN_STOCK`.
- SQL: `data/verify-queries.sql`, câu 1 và câu 3.

### A2. Nhiều biến thể trong một phiếu (nhân tổ hợp, có biến thể đã có và biến thể mới)
**Dữ liệu**
- `Digiworld`, chi nhánh `Lapstore Hà Đông`, ghi chú `TEST A2 tổ hợp E14`
- Sản phẩm: `Lenovo – Lenovo ThinkPad E14 Gen 6` · Màu **Đen**
- CPU: tick **Intel Core Ultra 5 125U** và **Intel Core Ultra 7 155U** · GPU: tick **Intel Graphics** · RAM **16 GB** · Ổ cứng tick **512 GB** và **1 TB**

**Các bước:** thanh thông tin trên khối phải ghi `1 màu × 2 CPU × 1 GPU × 1 RAM × 2 ổ cứng = 4 biến thể`. Điền cho 4 dòng (giá vốn / giá bán / serial):

| CPU | Ổ cứng | Trạng thái dự kiến | Giá vốn | Giá bán | Serial |
|---|---|---|---|---|---|
| Ultra 5 125U | 512 GB | Đã có `E14G6-U5-16-512` | 18000000 | (khóa) | `TEST-E14-001` |
| Ultra 5 125U | 1 TB | **Biến thể mới** | 21000000 | 25990000 | `TEST-E14-002` |
| Ultra 7 155U | 512 GB | **Biến thể mới** | 22000000 | 27990000 | `TEST-E14-003` |
| Ultra 7 155U | 1 TB | Đã có `E14G6-U7-16-1T` | 23000000 | (khóa) | `TEST-E14-004` |

**Mong đợi**
- Thông báo: `Đã nhập kho phiếu PN…: 4 máy. Tạo 2 cấu hình mới, đang chờ admin duyệt giá bán rồi mới mở bán.`
- Tồn Hà Đông: `E14G6-U5-16-512` +1, `E14G6-U7-16-1T` +1 (dòng mới), 2 SKU mới mỗi cái +1. Hai biến thể mới có trạng thái **Chưa mở bán** (Tồn kho và Sản phẩm).
- **Duyệt giá:** đăng nhập admin → *Duyệt giá* → thấy 2 đề xuất, lý do `Giá bán khởi tạo khi nhập hàng (phiếu PN…)` → duyệt cả hai → biến thể chuyển **Đang bán**, giá hiện đúng 25.990.000 / 27.990.000 ₫ và hiện ở cửa hàng (`/shop`).
- Thử bật bán khi chưa duyệt: vào *Sửa* biến thể mới (trước khi duyệt), đặt trạng thái cấu hình "Đang bán" → lỗi `chưa có giá bán được duyệt`.

### A3. Quên tick GPU tạo ra biến thể khác
Làm như A2 nhưng **không tick GPU**: dòng sinh ra có cột GPU `Không có` và nhãn **Biến thể mới** (vì biến thể sẵn có là `Intel Graphics`). Đây là hành vi đúng: máy không GPU là biến thể khác. (Không xác nhận phiếu, chỉ quan sát rồi đóng tab.)

---

## B. Tạo sản phẩm mới ngay khi nhập kho

### B1. Sản phẩm mới + hãng mới + giá trị thuộc tính mới
**Dữ liệu**
- `Digiworld`, chi nhánh `Lapstore Hà Đông`, ghi chú `TEST B1 sản phẩm mới`
- Chọn **Tạo sản phẩm mới**: Hãng **+ Hãng mới… → `Gigabyte`**; danh mục: chọn mục laptop gaming; Tên `TEST Gigabyte AORUS 15 (2025)`; Dòng `AORUS`; Năm `2025`; Mô tả bất kỳ
- Màu **Đen**
- CPU: gõ ở ô "+ Thêm" → **`AMD Ryzen 7 8845HS`** (chưa có → thêm mới, hiện nhãn "(mới)")
- GPU: tick **RTX 4060 8GB**
- RAM: tick **16 GB** và gõ `24GB` (mới) · Ổ cứng: tick **512 GB** và gõ `2TB` (đã có → tự tick "2 TB")

→ `1 × 1 × 1 × 2 × 2 = 4 biến thể`. Điền 4 dòng, giá vốn `27000000`, giá bán `31990000` (dùng "Áp cho mọi dòng"), serial `TEST-AORUS-001…004` (mỗi dòng một serial).

**Mong đợi**
- Thông báo: `…: 4 máy. Tạo 1 sản phẩm mới. Tạo 4 cấu hình mới, đang chờ admin duyệt giá…`
- Màn *Sản phẩm* tìm `AORUS` thấy sản phẩm với 4 biến thể "Chưa mở bán"; *Thuộc tính*: CPU có `AMD Ryzen 7 8845HS`, RAM có `24 GB`; Hãng `Gigabyte` xuất hiện trong ô chọn hãng lần sau.
- Mở **chi tiết sản phẩm** (bấm tên) thấy tab mới tên `SP: TEST Gigabyte AORUS 15 (2025)`.

### B2. Chống trùng thuộc tính (không phân biệt hoa/thường, dấu, khoảng trắng)
Trong khối sản phẩm bất kỳ, ô "+ Thêm" của CPU lần lượt gõ rồi Enter:

| Gõ | Kết quả mong đợi |
|---|---|
| `intel core i5 13500h` | `Đã có "Intel Core i5-13500H", đã chọn giá trị đó.` (không tạo thêm chip) |
| `INTEL CORE I5-13500H` | như trên |
| `amd ryzen 7 8845hs` | (sau B1) `Đã có "AMD Ryzen 7 8845HS"…` |
| `  Intel   Core  i9-14900HX ` | `Đã thêm "Intel Core i9-14900HX"` (khoảng trắng thừa được gộp) |

RAM: `16gb` → đã có "16 GB"; `1tb` ở ô ổ cứng → đã có "1 TB"; `abc` → lỗi đỏ `Nhập số GB hợp lệ…`; để trống rồi bấm Thêm → `Nhập giá trị CPU.`
**Kiểm tra phía server:** *Thuộc tính → CPU → ô "Thêm CPU mới"* gõ `INTEL CORE I5-13500H` → lỗi `Đã có "Intel Core i5-13500H"… không tạo trùng`.

### B3. GPU không bắt buộc (máy không có GPU rời)
`Digiworld`, `Lapstore Hà Đông`, ghi chú `TEST B3 không GPU`; tạo sản phẩm mới tên `TEST Laptop văn phòng không GPU`, hãng `Acer`, danh mục văn phòng, màu **Bạc**, CPU **Intel Core i5-13500H**, **không tick GPU**, RAM 8 GB, ổ cứng 256 GB; giá vốn `9000000`, giá bán `12990000`, serial `TEST-NOGPU-001`.
**Mong đợi:** dòng ghi GPU `Không có`; sau khi duyệt giá, trang chi tiết sản phẩm cột GPU hiện **Không có**.

### B4. Hai biến thể chỉ khác GPU (cần migration V4)
`Digiworld`, `Lapstore Hà Đông`, ghi chú `TEST B4 chỉ khác GPU`; sản phẩm có sẵn `ASUS – ASUS ROG Strix G16 (2025)`; màu **Đen**; CPU **Intel Core i7-14650HX**; GPU tick **cả `RTX 4050 6GB` và `RTX 4060 8GB`**; RAM **16 GB**; ổ cứng **512 GB**.
→ 2 dòng: `RTX 4050 6GB` = **Đã có `ROG-G16-I7-16-512`**; `RTX 4060 8GB` = **Biến thể mới**. Giá vốn `28000000`; giá bán dòng mới `36990000`; serial `TEST-ROG-4050-001`, `TEST-ROG-4060-001`.
**Mong đợi:** nhập thành công, tạo 1 cấu hình mới (SKU dạng `ASUSROGSTRIXG162-RTX40608-16-512-DEN`). Trước V4 sẽ báo trùng; sau V4 phải qua.

---

## C. Trường hợp lỗi và kiểm tra dữ liệu nhập

Mỗi ca dùng khối sản phẩm có sẵn `Acer Swift Go 14` như A1 (chi nhánh Cầu Giấy) trừ khi ghi khác; **không có phiếu nào được tạo** nếu ca báo lỗi.

| # | Dữ liệu / thao tác | Mong đợi |
|---|---|---|
| C1 | Hai dòng cùng serial `TEST-DUP-001` | Thanh dưới chữ đỏ `Serial bị lặp…`; bấm xác nhận thấy lỗi `Serial bị lặp trong phiếu: TEST-DUP-001` |
| C2 | Serial đã có trong hệ thống `SWIFTGO14-I5-16-512-001` (đã bán) + `TEST-C2-001` | Lỗi từ server `Serial đã tồn tại trong hệ thống: SWIFTGO14-I5-16-512-001.` **Dữ liệu đã nhập vẫn còn nguyên trong form** (khôi phục từ bản nháp trình duyệt); không máy nào được ghi (kiểm tra `TEST-C2-001` không tồn tại) |
| C3 | Có serial nhưng **bỏ trống giá vốn** | `…nhập giá vốn lớn hơn 0.` |
| C4 | Biến thể mới (VD tick thêm màu Đen) có serial nhưng **bỏ trống giá bán** | `…đây là biến thể mới, nhập giá bán lớn hơn 0.` |
| C5 | Không nhập serial nào ở mọi dòng | `Chưa có serial nào…` |
| C6 | Serial dài 61 ký tự: `TEST-` + 56 chữ `X` | Lỗi `dài quá 60 ký tự` (từ server) |
| C7 | Chế độ "Tạo sản phẩm mới" nhưng để trống tên / chưa chọn danh mục / chưa chọn hãng | Lỗi tương ứng `nhập tên sản phẩm mới.` / `chọn danh mục.` / `chọn hãng.` |
| C8 | Chế độ "Chọn sản phẩm có sẵn" nhưng chưa chọn sản phẩm | `chọn sản phẩm có sẵn, hoặc chuyển sang "Tạo sản phẩm mới".` |
| C9 | Xóa toàn bộ khối sản phẩm rồi gửi | `Chưa có serial nào…` (khối cuối bị xóa sẽ tự tạo khối trống) |
| C10 | Đóng tab Nhập kho khi đang có dữ liệu | Tab có chấm vàng; đóng sẽ hỏi `có dữ liệu chưa lưu` |
| C11 | Đăng nhập `sales01` rồi mở `/staff/warehouse/receipts` (cổng 8083) | Bị từ chối (403 hoặc về đăng nhập) |
| C12 | (Tùy chọn) Thu quyền tạo sản phẩm: chạy `DELETE FROM role_permission WHERE role_id=(SELECT id FROM role WHERE code='WAREHOUSE_PROMO') AND permission_id=(SELECT id FROM permission WHERE code='PRODUCT_MANAGE'); COMMIT;` rồi đăng xuất/đăng nhập `kho01` và thử B1 | Lỗi `Bạn không có quyền tạo sản phẩm hoặc cấu hình mới (cần quyền PRODUCT_MANAGE)`; nhập vào biến thể đã có (A1) vẫn được. Khôi phục: `INSERT INTO role_permission (role_id, permission_id) SELECT r.id, p.id FROM role r JOIN permission p ON p.code='PRODUCT_MANAGE' WHERE r.code='WAREHOUSE_PROMO'; COMMIT;` |

---

## D. Excel (Apache POI)

Nút **Nhập từ Excel** có ở: mỗi dòng biến thể của Nhập kho, ô serial của Chuyển kho, ô quét của Kiểm kê. Tải **file mẫu** bằng nút "Tải file Excel mẫu".

| # | Thao tác | Mong đợi |
|---|---|---|
| D1 | Tải file mẫu, mở bằng Excel | Sheet `Serial` (cột A, tiêu đề `Serial`, 3 dòng mẫu `SN-MAU-xxxx`) và sheet `Hướng dẫn`; cột A đã định dạng văn bản |
| D2 | A1 nhưng nạp serial bằng `data/nhap-kho-sg14-5-may.xlsx` ở dòng biến thể | Ô serial điền 5 dòng; dưới nút hiện `Đọc 5 serial từ nhap-kho-sg14-5-may.xlsx.`; số máy trên thanh dưới cập nhật |
| D3 | Nạp lại đúng file đó lần nữa | `Đọc 5 serial … (5 dòng đã có sẵn).` và ô serial **không bị nhân đôi** |
| D4 | `data/01-serial-hop-le-8-may.xlsx` (8 serial `TEST-XL-001…008`) | 8 dòng, giữ đúng thứ tự |
| D5 | `data/02-serial-lap-va-dong-trong.xlsx` (có dòng lặp, dòng trống, khoảng trắng thừa) | Chỉ còn **4** serial: `TEST-DUP-001…004` |
| D6 | `data/03-khong-tieu-de-so-dai.xlsx` (không tiêu đề; 3 ô là **số** dài, 1 ô văn bản `00123456`) | 4 dòng: `356938035643809`, `356938035643817`, `356938035643825`, `00123456`. Số không bị đổi thành `3.56938E+14`, số 0 đầu giữ nguyên |
| D7 | `data/04-serial-o-cot-C-co-tieu-de.xlsx` (serial ở cột C, tiêu đề `Số serial`) | 5 serial `TEST-COL-001…005` (nhận đúng cột theo tiêu đề) |
| D8 | `data/05-file-hong.xlsx` (thực chất là văn bản) | Chữ đỏ `Không đọc được file Excel…`, ô serial không đổi |
| D9 | `data/06-chi-co-tieu-de.xlsx` | `Không tìm thấy serial nào trong file…` |
| D10 | `data/07-du-lieu-o-sheet-2.xlsx` | Chỉ đọc sheet đầu nên báo `Không tìm thấy serial nào…` |
| D11 | Xuất: *Tồn kho* → **Xuất Excel** (thử thêm có lọc chi nhánh) | File `ton-kho.xlsx` đúng số dòng đang lọc, có cột Tồn/Đang giữ/Khả dụng, dòng tiêu đề cố định, có bộ lọc |
| D12 | Xuất: *Tồn kho → Serial* của `SWIFTGO14-I5-16-512` tại Cầu Giấy → **Xuất Excel** | `serial-SWIFTGO14-I5-16-512.xlsx` cột A `Serial`; cột Giá vốn **chỉ có** nếu tài khoản có quyền `COST_VIEW` (mặc định admin có; đăng nhập `kho01` kiểm tra xem vai trò kho có quyền này không) |
| D13 | Vòng lặp: nạp file D12 vào ô serial của Chuyển kho | Đọc được đúng các serial vừa xuất (file xuất nhập lại được) |
| D14 | Nạp file hợp lệ vào ô quét của Kiểm kê (xem nhóm F) | Điền vào textarea; bấm *Ghi nhận* |

---

## E. Chuyển kho hai bước và nhận hàng

Cần A1 đã chạy (có `TEST-SG14-001…005` ở Cầu Giấy).

| # | Thao tác | Mong đợi |
|---|---|---|
| E1 | *Chuyển kho*: nguồn **Lapstore Cầu Giấy**, đích **Lapstore Hà Đông**, nạp `data/chuyen-kho-3-may-test-sg14.xlsx` (3 serial `TEST-SG14-001…003`) | `Đã xuất 3 máy, đang vận chuyển (phiếu CK…)`. Tồn Cầu Giấy `SWIFTGO14-I5-16-512` 10 → **7**; 3 serial ở trạng thái `IN_TRANSIT` |
| E2 | *Phiếu chuyển* (menu) | Phiếu CK… **Đang vận chuyển**, tuyến Cầu Giấy → Hà Đông, 3 máy |
| E3 | Mở phiếu → **Đã nhận đủ hàng** → xác nhận | Phiếu thành **Đã nhận**; 3 serial `IN_STOCK` tại Hà Đông; tồn Hà Đông `SWIFTGO14-I5-16-512` **+3**; tab quay về chi tiết phiếu |
| E4 | Chuyển máy đã bán: `SWIFTGO14-I5-16-512-001` | Lỗi trạng thái không còn hàng |
| E5 | Chuyển máy đang bị giữ: `ROG-G16-I7-16-1T-002` (Hà Đông → Cầu Giấy) | Lỗi máy đang được giữ |
| E6 | Nguồn = đích | `Chi nhánh nguồn và đích phải khác nhau.` |
| E7 | Serial không tồn tại `KHONG-CO-001` | `Không tìm thấy serial: KHONG-CO-001.` |
| E8 | Máy thuộc chi nhánh khác nguồn (nguồn Hà Đông, máy `TEST-SG14-004` ở Cầu Giấy) | `không nằm ở chi nhánh nguồn` |
| E9 | Mở lại phiếu đã nhận | Không còn nút nhận hàng; bấm nhận lại (gửi thủ công) báo phiếu không ở trạng thái đang vận chuyển |

---

## F. Kiểm kê (dùng **Kho tổng Long Biên**, đang có đúng 9 máy)

Menu *Kiểm kê* → *+ Phiên kiểm kê mới*. Quyền: `kho01` (và admin) có `STOCK_STOCKTAKE` từ migration V5.

### F1. Khớp hoàn toàn
- Chi nhánh `Kho tổng Long Biên`, ghi chú `TEST F1 khớp`
- Mong đợi ngay khi tạo: **Cần kiểm 9**, đã quét 0, chưa quét 9
- Nạp `data/kiem-ke-kho-tong-khop-9-may.xlsx` → *Ghi nhận*: `Đã ghi nhận 9 máy.` Tiến độ 9/9
- *Đóng phiên*: Thiếu **0**, Dư **0**, **không có** khung "Áp dụng điều chỉnh"

### F2. Có thiếu và có dư
- Phiên mới `TEST F2 thiếu dư` (chi nhánh Kho tổng; phiên F1 đã đóng nên được tạo)
- Nạp `data/kiem-ke-kho-tong-thieu-du.xlsx` (7 máy đúng + `SWIFTGO14-I5-16-512-003`, `ROG-G16-I7-16-512-001`, `KHONG-TON-TAI-001`)
- **Mong đợi khi ghi nhận:** thông báo xanh `Đã ghi nhận 9 máy (2 máy dư, không thuộc danh sách của chi nhánh).`; thông báo đỏ `Không có trong hệ thống (1 serial): KHONG-TON-TAI-001…`. Bộ đếm: khớp 7, chưa quét 2, dư 2
- Lọc "Dư": 2 dòng, ghi chú `Hệ thống ghi: IN_STOCK tại Lapstore Cầu Giấy` và `… SOLD tại Lapstore Hà Đông`
- *Đóng phiên*: Thiếu **2** (`E14G6-U7-16-1T-005`, `-006`)
- *Áp dụng điều chỉnh*: `Đã điều chỉnh: 1 máy dư chuyển về chi nhánh, 2 máy thiếu chuyển DEFECTIVE và trừ tồn.` và cảnh báo `1 dòng không điều chỉnh được: ROG-G16-I7-16-512-001: trạng thái SOLD cần xử lý thủ công`
- **Kiểm tra tồn:** Kho tổng `E14G6-U7-16-1T` **6 → 4**; `SWIFTGO14-I5-16-512` xuất hiện ở Kho tổng **1**, Cầu Giấy giảm 1; hai máy E14G6 thành `DEFECTIVE`; mỗi dòng đã xử lý có nhãn "Đã điều chỉnh"
- Nhấn *Áp dụng điều chỉnh* lần nữa (nếu còn nút): chỉ còn dòng SOLD, vẫn bị bỏ qua, không làm thay đổi tồn

### F3. Quét nhiều lần, quét lặp
- Phiên mới `TEST F3`: dán 3 serial `E14G6-U7-16-1T-001…003` → `Đã ghi nhận 3 máy.` Dán lại cùng 3 + `PROART16-U9-32-1T-001` → `Đã ghi nhận 1 máy. 3 máy đã quét trước đó.`
- Lọc "Chưa quét" thấy số còn lại giảm đúng; thử Xuất Excel khi đang mở

### F4. Ràng buộc và hủy
| Thao tác | Mong đợi |
|---|---|
| Tạo phiên thứ hai cho Kho tổng khi F3 còn mở | `…đang có một phiên kiểm kê chưa đóng…` |
| *Hủy phiên* F3 → xác nhận | Trạng thái `Đã hủy`; không đổi tồn; tạo lại phiên mới được |
| Quét vào phiên đã đóng/hủy (mở bằng liên kết cũ rồi gửi) | `Phiên … đã đóng, không quét thêm được.` |
| Quét ô trống | Trình duyệt chặn (ô bắt buộc); gửi chỉ có dấu cách → `Nhập ít nhất một serial.` |
| Đăng nhập `sales01` mở `/staff/warehouse/stocktakes` | Bị từ chối |

### F5. Xuất kết quả
Phiên F2 → **Xuất Excel**: sheet `Kết quả` (Serial, Sản phẩm, SKU, Kết quả Khớp/Thiếu/Dư, Hệ thống ghi, Chi nhánh hệ thống, Quét lúc, Đã điều chỉnh, Ghi chú) và sheet `Tổng hợp` (cần kiểm 9, đã quét khớp 7, thiếu 2, dư 2).

---

## G. Tab và giao diện khi thao tác dở

| # | Thao tác | Mong đợi |
|---|---|---|
| G1 | Nhập kho: tick dở vài ô → bấm *Chuyển kho* ở menu → quay lại tab *Nhập kho* | Các ô đã tick và serial đã nhập **còn nguyên** |
| G2 | Từ chi tiết sản phẩm bấm *← Danh sách* | Tab chi tiết đóng, quay về tab *Sản phẩm* (nạp lại) |
| G3 | Sửa sản phẩm → lưu | Tab sửa đóng, quay về tab chi tiết sản phẩm với dữ liệu mới |
| G4 | Bấm kính lúp, gõ `aorus`, Enter | Mở/chuyển sang tab *Sản phẩm* đã lọc; bấm ra ngoài khi ô trống thì thu lại |
| G5 | Đổi Top menu ↔ Sidebar, thu gọn sidebar bằng nút ở cuối menu | Lề thanh menu thẳng hàng với nội dung; không có thanh cuộn ngang |

## Dọn dữ liệu thử
Chạy `data/cleanup-test-data.sql` (sửa mốc `since` ở đầu file thành lúc bạn bắt đầu thử). File cũng hoàn tác điều chỉnh của F2 (2 máy `E14G6` và máy `SWIFTGO14-I5-16-512-003`) và tính lại tồn từ serial. Nếu không muốn đụng SQL: tạo lại schema dev (`flyway clean` + chạy lại) là cách nhanh nhất.
