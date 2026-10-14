# Tự động cho pet ăn

Mở **Auto → Tự động cho pet ăn**.

1. Chọn pet đang trang bị hoặc trong hành trang. Pet trong kho không được dùng.
2. **Ăn đồ**: chọn các màu đồ, cấp đồ tối đa và chu kỳ tính bằng giây, rồi bật.
3. **Ăn kẹo**: chọn vật phẩm kẹo trong hành trang, đặt chu kỳ tính bằng giây, rồi bật.
   Danh sách hiển thị vật phẩm tiêu hao/nguyên liệu để người chơi chọn đúng kẹo mà server hỗ trợ cho pet.
4. Hai chế độ bật/tắt độc lập; mặc định đều tắt. Cấu hình lưu riêng theo server và nhân vật.

Mỗi lượt dùng một món/một lần cho ăn theo lệnh gốc của game. Ăn đồ chỉ lấy trang bị thường loại 0–6,
không khóa, chưa cộng, đúng màu và không vượt cấp đã chọn. Không lấy đồ đang mặc, đồ được chức năng
cường hóa bảo vệ, cánh hay trứng pet. Đồ đã dành cho pet được bỏ qua khi tự bán.

Kẹo được ưu tiên khi đến chu kỳ. Hết vật phẩm hoặc không tìm thấy pet đã chọn thì chờ,
không tự mua và không chuyển sang pet khác. Chu kỳ cho phép 2–86400 giây; cấp đồ 1–1000.
Tạm ngưng khi chết, mất kết nối, chuyển map, mở hộp thoại/menu, cường hóa hoặc đang xử lý tự bán.

Sau khi gửi lệnh, chờ hành trang cập nhật trước khi cho ăn tiếp. Nếu sau 15 giây chưa thấy
vật phẩm giảm/mất, chế độ tương ứng tự tắt; kiểm tra trạng thái trong menu trước khi bật lại.
Server quyết định pet có nhận vật phẩm hay không.

Kiểm chứng: `tests/test_pet_feed.py` kiểm tra bộ lọc, gói lệnh, chu kỳ, phản hồi, timeout và lưu cấu hình.
Bản JAR đã biên dịch; chưa kiểm chứng với tài khoản trên server thật.
