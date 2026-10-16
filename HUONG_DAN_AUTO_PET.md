# Tự động cho pet ăn

Mở **Auto → Tự động cho pet ăn**.

1. Chọn pet đang trang bị hoặc trong hành trang. Pet trong kho không được dùng.
2. **Ăn đồ**: chọn màu xanh dương, vàng, tím hoặc cam, rồi bật. Không cho ăn đồ trắng hay xanh lá.
   Mã màu theo game: trắng 0, xanh dương 1, vàng 2, tím 3, cam 4, xanh lá 5.
   Các biến thể màu 20–49 được đối chiếu theo màu tên đồ mà game hiển thị.
3. **Ăn kẹo**: chọn kẹo trong hành trang rồi bật. Chỉ hiển thị Kẹo bạc hà, Kẹo the, Kẹo sữa và Kẹo hương cam.
4. Hai chế độ bật/tắt độc lập; mặc định đều tắt. Cấu hình lưu riêng theo server và nhân vật.

Mỗi lượt dùng một món/một lần cho ăn theo lệnh gốc của game. Ăn đồ lấy trang bị thường loại 0–6 và vũ khí loại 8–11,
chưa cộng và đúng màu đã chọn, bao gồm cả đồ khóa; không giới hạn cấp đồ. Không lấy mề đay bất kể màu,
đồ đang mặc, đồ được chức năng cường hóa bảo vệ, cánh hay trứng pet. Đồ đã dành cho pet được bỏ qua khi tự bán.

Khi bật cả hai chế độ và có đủ vật phẩm, đồ và kẹo được cho ăn luân phiên. Nếu một loại hết,
vẫn tiếp tục cho ăn loại còn lại. Hết vật phẩm hoặc không tìm thấy pet đã chọn thì chờ,
không tự mua và không chuyển sang pet khác. Chu kỳ cố định 2 giây giữa các lần cho ăn, dùng chung cho đồ và kẹo.
Tạm ngưng khi chết, mất kết nối, chuyển map hoặc chưa vào game.
Cho ăn ngầm khi mở menu, hộp thoại, hành trang, cửa hàng và các màn hình thao tác trong game;
không tự đóng giao diện đang dùng. Khi tự động cường hóa, chỉ loại món đang được cường hóa khỏi đồ cho ăn.
Hàng đợi bán chưa trống không chặn cho ăn; tự bán kiểm tra lại từng món và bỏ qua đồ dành cho pet.
Sau khi bật và chọn màu, pet bắt đầu ăn ngầm. Nếu vẫn không ăn, chọn **Kiểm tra cho ăn**
để xem số đồ hợp lệ và lý do bị loại (đã cộng, mề đay, sai màu hoặc loại đồ khác), cùng điều kiện đang chặn.

Sau khi gửi lệnh, chờ hành trang cập nhật trước khi cho ăn tiếp. Nếu sau 15 giây chưa thấy
vật phẩm giảm/mất, chế độ tương ứng tự tắt; kiểm tra trạng thái trong menu trước khi bật lại.
Thông báo kết quả của server dạng “+20 điểm cho ăn, +7” được chuyển thành bong bóng chat trên nhân vật:
“Đã cho pet ăn .. +20 điểm cho ăn, +7”, thay cho hộp thoại OK (áp dụng cả cho ăn thủ công).
Thông báo lỗi và câu hỏi xác nhận khác vẫn hiển thị bình thường. Không tạo thông báo chat trùng khi hành trang cập nhật.
Không hiển thị “Chờ đồ/kẹo hoặc chu kỳ”, “Đã cho pet ăn” hoặc “Đã tắt” trong menu.
Khi đọc cấu hình cũ, bỏ màu trắng/xanh lá, cấp đồ tối đa và chu kỳ cũ; xóa lựa chọn kẹo không hợp lệ.
Cấu hình màu phiên bản 1/2 được chuyển sang phiên bản 3 theo tên màu đã chọn, sửa lỗi mã màu bị lệch một vị trí.
Server quyết định pet có nhận vật phẩm hay không.

Kiểm chứng: `tests/test_pet_feed.py` kiểm tra bộ lọc, gói lệnh, chu kỳ, phản hồi, timeout và lưu cấu hình.
Bản JAR đã biên dịch; chưa kiểm chứng với tài khoản trên server thật.
