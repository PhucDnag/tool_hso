# Lửa, sách, dầu và khu 2

Mở **AUTO → Lửa, dầu & khu 2**. Các chức năng mặc định tắt; cấu hình lưu riêng theo server và nhân vật.

- **UP khu 2**: chọn **Ngọc**, **Chỉ vé**, hoặc **Vé trước, ngọc sau**. Auto chọn theo nhãn lựa chọn server gửi về, không cố định vị trí nút. Chế độ vé trước chuyển sang ngọc khi server báo thiếu vé hoặc không đưa ra lựa chọn vé.
- **Khu dự phòng**: chọn khu khác khu 2. Khi server báo hết giới hạn 6 giờ trong ngày, auto nhớ ngày bị chặn và chuyển sang khu này. Ngày mới (giờ Việt Nam) có thể thử lại; server vẫn quyết định có cho vào hay không. Lỗi đầy/khoá khu tạm dùng khu dự phòng và chờ 10 phút trước khi thử lại.
- **Lửa thiêng**, **Sách tri thức**, **Bình dầu**: bật riêng từng món. Auto tìm đúng tên trong túi; có thể chọn lại vật phẩm trong menu nếu tên server khác. Chỉ dùng vật phẩm có sẵn, không mua thêm.
- **Chu kỳ sử dụng**: mặc định 30 phút, có thể chọn 1–120 phút. Đây là cấu hình nhắc dùng lại, không phải thời hạn được xác nhận của vật phẩm; chỉnh theo thời hạn trên server bạn chơi. Bình dầu chỉ dùng sau khi số lượng Lửa thiêng giảm xác nhận đã sử dụng, tại đúng map/khu đó, trong chu kỳ lửa đã đặt.

Chức năng chạy khi đang auto đánh quái tại map UP, tạm nghỉ khi GoBack, bán đồ, cường hóa, chết hoặc mở menu. GoBack phụ trách đường đi; chức năng khu 2 phụ trách chọn khu để hai chức năng không đổi khu qua lại.

Không gửi lặp yêu cầu trả phí khi kết quả chưa rõ. Menu lạ được giữ để người dùng xử lý; trạng thái hiển thị ở bảng góc trái. Vật phẩm không có cập nhật số lượng sau 15 giây sẽ tự tắt mục tương ứng, không dùng liên tiếp.

Đã kiểm thử bằng phản hồi server mô phỏng; chưa kiểm thử trên tài khoản/server thật.
