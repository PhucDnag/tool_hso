# Hướng Dẫn Sử Dụng Bản Mod MENU UP - HONG PHUC HSO
**Knight Online v403 (Hiệp Sĩ Online)**

> **Tác giả:** **HONG PHUC HSO**  
> **Phiên bản:** **MENU UP v1.6**  
> **Bản quyền:** Đã tích hợp chữ ký và bản quyền **HONG PHUC HSO** trực tiếp vào client game.

---

## 1. Hệ Thống MENU UP Tổng Hợp

Toàn bộ các tính năng hỗ trợ tự động đã được quy tụ vào **MENU UP - HONG PHUC**:

- **Nút cảm ứng `AUTO` trên màn hình** (vị trí bên trái dưới icon chat, tọa độ `x=8, y=105`):
  - Chạm vào nút này sẽ mở trực tiếp **`MENU UP - HONG PHUC`**.
  - Nút sẽ sáng **xanh lá rực rỡ** khi có bất kỳ chức năng UP nào đang bật (Auto Đánh quái, GoBack, Bơm HP/MP, Thú cưỡi, Auto Chat, Tự đăng nhập lại, hoặc Auto Hồi sinh).
- **Trong Menu Cài đặt (Tab Chức năng)**: Đã đổi thành mục **`Menu UP [HONG PHUC]`**.

---

## 2. Tính Năng: GoBack - Tự Động Tìm Đường Về Map UP (Nâng Cấp Toàn Diện v1.6.1)

Khi bạn cắm máy train/up quái, nếu chẳng may bị **mất kết nối đăng nhập lại về làng**, **bị quái đánh chết hồi sinh về làng**, hoặc **bị bất kỳ tác nhân nào đưa ra khỏi map đang UP**, tính năng **GoBack** sẽ lập tức hoạt động đưa bạn quay trở lại đúng vị trí đang UP:

### Điểm Mới Nổi Bật & Khắc Phục Lỗi Đứng Im Ở Làng:
1. **Chọn Map UP Từ Danh Sách (1 Chạm Bắt Đầu Ngay Từ Làng)**:
   - Bạn đang đứng ở trong làng và muốn bắt đầu đi up? Không cần phải tự chạy bộ ra tận nơi mới lưu được vị trí!
   - Vào **`Menu UP -> Cài đặt GoBack -> Chọn Map UP từ danh sách`**:
     - Danh sách đầy đủ các map train quái từ cấp thấp đến cấp cao: *Bìa Rừng, Hang Lửa, Rừng Ảo Giác, Cánh Đồng Sói, Thung Lũng Kỳ Bí, Hồ Ký Ức, Bờ Biển, Vực Đá, Nghĩa Địa Tàu Đắm, Đầm Lầy, Sa Mạc, Đồi Cát, Vực Lún, Nghĩa Địa Cát, Rừng Chết, Suối Ma, Thung Lũng Đá, Hầm Mộ Tầng 1, Rừng Chuột, Rừng Hoa Đỏ, Vịnh Caribe...*
     - Chỉ cần chạm vào map mong muốn, nhân vật sẽ **ngay lập tức xuất phát chạy ra khỏi làng** và vượt qua các bản đồ để tới bãi quái!
2. **Khắc phục triệt để lỗi nhân vật đứng im ở làng**:
   - **Hỗ trợ toàn bộ 5 làng khởi đầu**: Đã bổ sung liên kết cho cả **Làng Sói Trắng (Map 1), Làng Ánh Sáng (54), Làng Gió (56), Làng Sét (58), Làng Lửa (60)** và Ngôi Làng Nhỏ (0). Nhân vật thuộc bất kỳ trường phái/lớp nhân vật nào khi hồi sinh về làng đều tìm được đường đi.
   - **Hệ thống di chuyển đa cấp (Multi-Tier Movement Engine)**:
     - Khắc phục đặc thù bản đồ làng có diện tích lớn khiến thuật toán tìm đường mặc định của game bị quá giới hạn bước đi.
     - Tích hợp 3 cấp độ sinh đường: *Cấp 1 (A\* toàn bản đồ) -> Cấp 2 (Waypoint dẫn hướng 15 ô trung gian) -> Cấp 3 (Direct Stepping Road)*. Đảm bảo nhân vật luôn có đường di chuyển hợp lệ trong `player.posTransRoad`, kích hoạt cơ chế sải bước của game để chạy thẳng ra cổng làng.
     - Khởi tạo đồng bộ tọa độ `toX = player.x; toY = player.y` giúp nhân vật nhận diện bước đi ngay từ khung hình đầu tiên.
   - **Bộ chọn cổng thông minh (Fallback Portal Selector)**: Khi server gửi tên cổng bị lệch mã UTF hoặc không có tên, tool tự động phân tích cấu trúc bản đồ (ở Làng Sói Trắng tự động chọn cổng bên phải hướng ra Bìa Rừng) để bước qua cổng chính xác 100%.
   - **Cơ chế bảo vệ không lưu nhầm làng**: Khi bạn đứng trong làng và vô tình bật Auto Đánh Quái, tool sẽ chặn không lưu làng làm Map UP để tránh trường hợp tưởng nhầm đã đến nơi và đứng im.
3. **Tự động chuyển Khu vực (Area) và Tọa độ ban đầu**:
   - Khi đã tới đúng Map UP mục tiêu: Tool kiểm tra Khu vực, nếu khác khu đã lưu sẽ tự động gọi `Change_Area` để **chuyển về đúng Khu vực bạn đã chọn**.
   - Tiếp tục di chuyển đến chính xác tọa độ `(farmX, farmY)` ban đầu.
4. **Tự động tiếp tục UP**:
   - Ngay khi đã tới nơi: Tool tự động bật lại **`Auto Đánh Quái`** (focus quái, thi triển chiêu thức) và tiếp tục cưỡi thú cưỡi đã chọn.
5. **Tối ưu tốc độ & An toàn khi GoBack**:
   - **Tạm dừng đánh quái dọc đường**: Trong suốt hành trình GoBack qua các map trung gian, tool tạm ngưng đánh quái để nhân vật chạy thẳng một mạch về map UP nhanh nhất mà không bị vướng bận.
   - **Duy trì Auto Thú Cưỡi**: Nhân vật luôn được cưỡi ngựa/thú cưỡi để đạt tốc độ di chuyển tối đa.
   - **Duy trì Auto Bơm Máu**: Nếu bị quái trên đường đánh trúng, Auto HP/MP vẫn tự động bơm máu bảo vệ an toàn tính mạng.
6. **Giao diện HUD điều hướng trên màn hình**:
   - Trong quá trình GoBack, hiển thị thanh trạng thái màu xanh Cyan hiện đại ở đỉnh màn hình:
     ```
     [HONG PHUC HSO] GOBACK
     Về: Rừng Chết (Khu 3) | Đang đến: Nghĩa địa cát (Còn 2 map)
     (Chạm vào đây để HỦY)
     ```
   - Người chơi có thể **chạm trực tiếp vào thanh HUD này** để HỦY GoBack bất cứ lúc nào nếu muốn điều khiển tay.

---

## 3. Tính Năng: Auto Đánh Quái (Tự Động Đánh & Focus Quái)

- **Cơ chế hoạt động**:
  - Khi bật: Tool tự động kích hoạt chế độ đánh quái, tự động tìm và **focus (chọn mục tiêu) vào quái gần nhất**, tự động dùng kỹ năng hoặc đòn đánh thường.
  - Khi quái chết: Tool lập tức tìm và chuyển mục tiêu sang con quái gần nhất kế tiếp.
- **Tự động liên kết với GoBack**:
  - Khi kết hợp với GoBack: Bị ngắt kết nối, chết về làng -> GoBack đưa về map cũ -> Tự động đánh quái tiếp tục mà không cần can thiệp!

---

## 4. Tính Năng: Auto Bơm Máu / Mana (HP / MP)

- **Menu cài đặt trực quan**: Trong MENU UP chọn **`Cài đặt Auto Bơm HP/MP`**:
  - **Bật / Tắt**: Bật hoặc tắt tính năng tự bơm máu/mana.
  - **Đổi nhanh %**: Chạm vào mục `% HP` hoặc `% MP` để chuyển đổi nhanh qua các mốc phổ biến (20%, 30%, 40%, 50%, 60%, 70%, 80%).
  - **Nhập % tùy ý**: Chạm vào `Nhập % HP tùy chỉnh` hoặc `Nhập % MP tùy chỉnh` để nhập chính xác bất kỳ số phần trăm nào từ `1%` đến `99%`.
- **Cơ chế bơm thông minh**:
  - Tự động quét tìm bình HP/MP trong hành trang (hoặc trên phím tắt nhanh).
  - Tự động kiểm tra thời gian hồi phục (delay 2 giây của game) để tránh lãng phí bình thuốc.

---

## 5. Tính Năng: Auto Chọn Thú Cưỡi

- **Cơ chế gửi packet chuẩn**:
  - Sử dụng gói tin `Use_Potion(itemId)` (Packet 32) cho các loại ngựa ID `62..66`.
- **Các tùy chọn trong MENU `Auto Thú Cưỡi`**:
  - **TẮT Thú Cưỡi**: Không dùng thú cưỡi (tự xuống ngựa nếu đang cưỡi).
  - **Bất kỳ trong túi**: Tool sẽ tự động quét túi đồ và ưu tiên gọi con ngựa/thú cưỡi bất kỳ bạn đang mang theo.
  - **Chọn từ túi đồ (Quét)**: Quét hành trang hiện tại và hiển thị **Danh sách tất cả các loại thú cưỡi bạn đang có trong túi** (kèm tên và số lượng thực tế), chạm vào để chọn ngay!
  - **Ngựa Nâu (ID 62)**, **Ngựa Trắng (ID 63)**, **Ngựa Chiến Giáp (ID 64)**, **Ngựa Đỏ (ID 65)**, **Ngựa Đen (ID 66)**.
- **Tự động lên lại thú cưỡi**: Khi mất kết nối đăng nhập lại, hoặc sau khi hồi sinh, hoặc trong khi đang GoBack tìm đường, tool sẽ tự động lên lại thú cưỡi.

---

## 6. Tính Năng: Auto Hồi Sinh Thông Minh (Bảo Vệ Ngọc Tuyệt Đối)

Trong **MENU UP**, tùy chọn **`Auto Hồi Sinh: [TẮT / Về Làng / Tại Chỗ]`**:

1. **Giao diện đếm ngược 3 giây trên màn hình**:
   - Khi nhân vật kiệt sức, hiển thị bảng đếm ngược viền xanh neon nổi bật. Chạm vào bảng để HỦY hồi sinh.
2. **Cơ chế Hồi sinh Tại Chỗ thông minh & An toàn**:
   - **Bước 1 (Kiểm tra hành trang):** Quét túi đồ xem có vật phẩm hồi sinh không (*Thánh giá, Cỏ hồi sinh, Vé hồi sinh...*). Nếu có, sử dụng vật phẩm để hồi sinh tại chỗ.
   - **Bước 2 (Kiểm tra yêu cầu Vàng):** Nếu không có vật phẩm mà game yêu cầu trả phí bằng **VÀNG**, tool sẽ tự động đồng ý dùng **VÀNG** để hồi sinh tại chỗ.
   - **Bước 3 (TUYỆT ĐỐI KHÔNG DÙNG NGỌC):** Nếu hồi sinh tại chỗ yêu cầu bằng **NGỌC**, tool sẽ **LẬP TỨC TỪ CHỐI** và **TỰ ĐỘNG CHUYỂN SANG VỀ LÀNG** để bảo vệ toàn bộ ngọc của bạn!
   - **Bước 4 (Kết hợp GoBack):** Sau khi về làng hồi sinh, tính năng GoBack sẽ tự động tìm đường đưa nhân vật trở lại điểm train ban đầu!

---

## 7. Tính Năng: Tự Động Đăng Nhập Lại Khi Mất Kết Nối (Auto Reconnect)

1. **Bắt trọn vẹn sự kiện ngắt kết nối**: Chặn đứng hộp thoại lỗi `Bạn bị mất kết nối với máy chủ.`, tự động dọn sạch popup và chuyển về màn hình đăng nhập.
2. **Delay ngẫu nhiên 10 - 15 giây chống quét Tool**: Delay ngẫu nhiên để tránh bị hệ thống game phát hiện bot.
3. **Giao diện đếm ngược trực quan**: Hiển thị thanh thông báo đếm ngược viền vàng sang trọng ở đỉnh màn hình (có thể chạm vào để HỦY).
4. **Tự động vào đúng nhân vật cũ & kích hoạt GoBack**:
   - Hết đếm ngược, tự động đăng nhập đúng nhân vật cũ.
   - Nếu nhân vật xuất hiện ở làng, GoBack sẽ tự động kích hoạt và dẫn đường quay lại đúng bãi quái đang up!

---

## 8. Tính Năng: Auto Chat Chống Spam

- Mở mục **`Cài đặt Auto Chat`** trong MENU UP:
  - Bật / Tắt Auto Chat.
  - **Số ngẫu nhiên: BẬT / TẮT**: Tự động chèn số ngẫu nhiên từ `10` đến `999` đầu câu (ví dụ `548, a di đà phật`) để tránh bị game chặn chat do spam tin trùng nhau.
  - Cài đặt nội dung (dùng dấu `;` để luân phiên nhiều câu khác nhau).
  - Cài đặt thời gian giãn cách giữa các lần gửi (tính bằng giây).

---

## 9. File Game Đã Đóng Gói Sẵn Sàng

- **[KnightOnline_v403.jar](file:///d:/Code/tool/KnightOnline_v403.jar)**: File game chính thức đã tích hợp toàn bộ tính năng GoBack v1.6.
- **[KnightOnline_v403_AutoChat.jar](file:///d:/Code/tool/KnightOnline_v403_AutoChat.jar)**: Bản đóng gói dự phòng.

