# Hướng Dẫn Sử Dụng Bản Mod MENU UP - HONG PHUC HSO
**Knight Online v403 (Hiệp Sĩ Online)**

> **Tác giả:** **HONG PHUC HSO**  
> **Phiên bản:** **MENU UP v1.5**  
> **Bản quyền:** Đã tích hợp chữ ký và bản quyền **HONG PHUC HSO** trực tiếp vào client game.

---

## 1. Hệ Thống MENU UP Tổng Hợp

Toàn bộ các tính năng hỗ trợ tự động đã được quy tụ vào **MENU UP - HONG PHUC**:

- **Nút cảm ứng `AUTO` trên màn hình** (vị trí bên trái dưới icon chat, tọa độ `x=8, y=105`):
  - Chạm vào nút này sẽ mở trực tiếp **`MENU UP - HONG PHUC`**.
  - Nút sẽ sáng **xanh lá rực rỡ** khi có bất kỳ chức năng UP nào đang bật (Auto Đánh quái, Bơm HP/MP, Thú cưỡi, Auto Chat, Tự đăng nhập lại, hoặc Auto Hồi sinh).
- **Trong Menu Cài đặt (Tab Chức năng)**: Đã đổi thành mục **`Menu UP [HONG PHUC]`**.

---

## 2. Tính Năng: Auto Đánh Quái (Tự Động Đánh & Focus Quái)

- **Cơ chế hoạt động**:
  - Khi bật: Tool tự động kích hoạt chế độ đánh quái, tự động tìm và **focus (chọn mục tiêu) vào quái gần nhất**, tự động dùng kỹ năng hoặc đòn đánh thường.
  - Khi quái chết: Tool lập tức tìm và chuyển mục tiêu sang con quái gần nhất kế tiếp.
- **Tự động kích hoạt lại sau khi ngắt kết nối hoặc hồi sinh**:
  - Trạng thái Auto Đánh quái được lưu trực tiếp vào bộ nhớ RMS (`hso_up_autoattack`).
  - Khi gặp sự cố mất mạng, disconnect và tự động đăng nhập lại thành công: **Tool sẽ tự động tiếp tục đánh quái ngay lập tức mà bạn không cần phải bấm lại**.
  - Khi nhân vật kiệt sức và hồi sinh (về làng hoặc tại chỗ): **Tool cũng tự động tiếp tục đánh quái**.

---

## 3. Tính Năng: Auto Bơm Máu / Mana (HP / MP)

- **Menu cài đặt trực quan**: Trong MENU UP chọn **`Cài đặt Auto Bơm HP/MP`**:
  - **Bật / Tắt**: Bật hoặc tắt tính năng tự bơm máu/mana.
  - **Đổi nhanh %**: Chạm vào mục `% HP` hoặc `% MP` để chuyển đổi nhanh qua các mốc phổ biến (20%, 30%, 40%, 50%, 60%, 70%, 80%).
  - **Nhập % tùy ý**: Chạm vào `Nhập % HP tùy chỉnh` hoặc `Nhập % MP tùy chỉnh` để nhập chính xác bất kỳ số phần trăm nào từ `1%` đến `99%`.
- **Cơ chế bơm thông minh**:
  - Tự động quét tìm bình HP/MP trong hành trang (hoặc trên phím tắt nhanh).
  - Tự động kiểm tra thời gian hồi phục (delay 2 giây của game) để tránh lãng phí bình thuốc.

---

## 4. Tính Năng: Auto Chọn Thú Cưỡi (Đã Fix 100% Trong v1.5)

### Nguyên nhân lỗi trước đó:
- Game Knight Online HSO sử dụng cơ chế: **Thú cưỡi là một loại vật phẩm trong hành trang** (loại Dược phẩm / Potion, ID vật phẩm tương ứng: `62` = Ngựa Nâu, `63` = Ngựa Trắng, `64` = Ngựa Giáp, `65` = Ngựa Đỏ, `66` = Ngựa Đen).
- Để **lên ngựa**, game bắt buộc phải gửi gói tin **`Use_Potion(itemId)`** (Packet 32). Còn gói tin `useMount(-97)` server chỉ dùng cho việc **xuống ngựa** (`-1`).

### Các tùy chọn trong MENU `Auto Thú Cưỡi`:
- **TẮT Thú Cưỡi**: Không dùng thú cưỡi (tự xuống ngựa nếu đang cưỡi).
- **Bất kỳ trong túi**: Tool sẽ tự động quét túi đồ và ưu tiên gọi con ngựa/thú cưỡi bất kỳ bạn đang mang theo.
- **Chọn từ túi đồ (Quét)**: Quét hành trang hiện tại và hiển thị **Danh sách tất cả các loại thú cưỡi bạn đang có trong túi** (kèm tên và số lượng thực tế), chạm vào để chọn ngay!
- **Ngựa Nâu (ID 62)**: Chỉ gọi Ngựa Nâu.
- **Ngựa Trắng (ID 63)**: Chỉ gọi Ngựa Trắng.
- **Ngựa Chiến Giáp (ID 64)**: Chỉ gọi Ngựa Chiến Giáp.
- **Ngựa Đỏ (ID 65)**: Chỉ gọi Ngựa Đỏ (Xích Thố).
- **Ngựa Đen (ID 66)**: Chỉ gọi Ngựa Đen.

> [!NOTE]
> **Lưu ý cơ chế game về Ngựa Nâu (ID 62):**
> Game HSO quy định: Ngựa Nâu chỉ dùng để chạy di chuyển map, **không thể đánh quái khi đang cưỡi Ngựa Nâu** (game sẽ báo *"Xuống ngựa để tấn công!"*).
> Nếu bạn muốn vừa cưỡi ngựa vừa tự đánh quái, hãy dùng **Ngựa Trắng (ID 63) trở lên**!

- **Tự động lên lại thú cưỡi**: Khi mất kết nối đăng nhập lại, hoặc sau khi hồi sinh từ cõi chết, tool sẽ tự động lên lại thú cưỡi đã chọn.

---

## 5. Tính Năng: Auto Hồi Sinh Thông Minh (Bảo Vệ Ngọc Tuyệt Đối)

Trong **MENU UP**, tùy chọn **`Auto Hồi Sinh: [TẮT / Về Làng / Tại Chỗ]`**:

1. **Giao diện đếm ngược 3 giây trên màn hình**:
   - Khi nhân vật kiệt sức, hiển thị bảng đếm ngược viền xanh neon nổi bật:
     ```
     [HONG PHUC HSO] AUTO HỒI SINH
     Hồi sinh tại chỗ (Có vật phẩm): 3s
     (Chạm vào đây để HỦY)
     ```
   - Chạm trực tiếp vào bảng để HỦY hồi sinh lần này nếu muốn.
2. **Cơ chế Hồi sinh Tại Chỗ thông minh & An toàn**:
   - **Bước 1 (Kiểm tra hành trang):** Quét túi đồ xem có vật phẩm hồi sinh không (*Thánh giá, Thánh giá hồi sinh, Cỏ hồi sinh, Vé hồi sinh, Bùa hồi sinh...*). Nếu có, lập tức sử dụng vật phẩm để hồi sinh tại chỗ.
   - **Bước 2 (Kiểm tra yêu cầu Vàng):** Nếu không có vật phẩm mà game yêu cầu trả phí bằng **VÀNG** (*vàng, vang, xu, gold, coin*), tool sẽ tự động đồng ý dùng **VÀNG** để hồi sinh tại chỗ.
   - **Bước 3 (TUYỆT ĐỐI KHÔNG DÙNG NGỌC):** Nếu hồi sinh tại chỗ yêu cầu bằng **NGỌC** (*ngọc, ngoc, gem, ruby*), tool sẽ **LẬP TỨC TỪ CHỐI** và **TỰ ĐỘNG CHUYỂN SANG VỀ LÀNG** để bảo vệ toàn bộ ngọc của bạn!
   - **Bước 4 (Tự động về làng an toàn):** Nếu không đủ điều kiện hồi sinh tại chỗ hoặc không có vàng/vật phẩm, tool sẽ tự động đưa nhân vật **Về Làng** hồi máu.

---

## 6. Tính Năng: Tự Động Đăng Nhập Lại Khi Mất Kết Nối (Auto Reconnect)

1. **Bắt trọn vẹn sự kiện ngắt kết nối (Fix lỗi popup đè màn hình)**:
   - Chặn đứng hộp thoại lỗi `Bạn bị mất kết nối với máy chủ.`, tự động dọn sạch popup và chuyển về màn hình đăng nhập `LoginScreen`.
2. **Delay ngẫu nhiên 10 - 15 giây chống quét Tool**:
   - Delay ngẫu nhiên từ 10 đến 15 giây (mỗi lần một con số khác nhau như 11s, 14s, 10s...) để tránh bị game ban nick do nghi ngờ dùng bot.
3. **Giao diện đếm ngược trực quan**:
   - Hiển thị thanh thông báo đếm ngược viền vàng sang trọng ở đỉnh màn hình:
     ```
     [HONG PHUC HSO] TỰ ĐỘNG ĐĂNG NHẬP
     Đăng nhập lại sau: 12s | NV: thuitoang14
     (Chạm vào đây để HỦY)
     ```
4. **Tự động vào đúng nhân vật cũ & tiếp tục train**:
   - Hết thời gian đếm ngược, tự động kết nối, đăng nhập và chọn chính xác nhân vật bạn đang chơi trước đó, sau đó tự động tiếp tục Auto Đánh quái và Auto Thú cưỡi.

---

## 7. Tính Năng: Auto Chat Chống Spam

- Mở mục **`Cài đặt Auto Chat`** trong MENU UP:
  - Bật / Tắt Auto Chat.
  - **Số ngẫu nhiên: BẬT / TẮT**: Tự động chèn số ngẫu nhiên từ `10` đến `999` đầu câu (ví dụ `548, a di đà phật`) để tránh bị game chặn chat do spam tin trùng nhau.
  - Cài đặt nội dung (dùng dấu `;` để luân phiên nhiều câu khác nhau).
  - Cài đặt thời gian giãn cách giữa các lần gửi (tính bằng giây).

---

## 8. File Game Đã Đóng Gói Sẵn Sàng

- **[KnightOnline_v403.jar](file:///d:/Code/tool/KnightOnline_v403.jar)**: File game chính thức đã tích hợp toàn bộ tính năng.
- **[KnightOnline_v403_AutoChat.jar](file:///d:/Code/tool/KnightOnline_v403_AutoChat.jar)**: Bản đóng gói dự phòng.
