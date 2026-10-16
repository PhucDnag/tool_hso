# Lửa, sách, dầu và khu 2

Mở **AUTO → Lửa, dầu & khu 2**. Các chức năng mặc định tắt; cấu hình lưu riêng theo server và nhân vật.

- **UP khu 2**: chọn **Ngọc**, **Chỉ vé**, hoặc **Vé trước, ngọc sau**. Auto chọn theo nhãn lựa chọn server gửi về, không cố định vị trí nút. Chế độ vé trước chuyển sang ngọc khi server báo thiếu vé hoặc không đưa ra lựa chọn vé.

  Auto đi tới gần biển **Khu**, xin danh sách khu từ server rồi mới chọn khu 2. Trong lúc đi tới biển, auto đánh quái của menu UP tạm nhường điều khiển di chuyển. Nếu server không trả danh sách hoặc map không có khu đã chọn, auto không gửi lệnh chọn khu mù.
- **Khu dự phòng**: chọn khu khác khu 2. Khi server báo hết giới hạn 6 giờ trong ngày, auto nhớ ngày bị chặn và chuyển sang khu này. Ngày mới (giờ Việt Nam) có thể thử lại; server vẫn quyết định có cho vào hay không. Lỗi đầy/khoá khu tạm dùng khu dự phòng và chờ 10 phút trước khi thử lại.
- **Lửa thiêng**, **Sách tri thức**, **Bình dầu**: bật riêng từng món. Auto tự tìm đúng tên trong túi, không cần chọn vật phẩm thủ công. Chỉ dùng vật phẩm có sẵn, không mua thêm.
- **Thời gian sử dụng cố định**: Lửa thiêng mỗi **1 giờ**, Sách tri thức mỗi **2 giờ**. Không còn menu chỉnh chu kỳ.
- **Bình dầu theo lửa**: chỉ châm sau khi hành trang xác nhận đã đốt lửa thành công, tại đúng map/khu của lửa. Dùng lần lượt, cách nhau ít nhất 2 giây và đợi xác nhận từng bình; tối đa **3 bình cho một lửa**. Khi server báo không thể châm thêm, dừng châm cho lửa đó; lần đốt lửa thành công tiếp theo mới bắt đầu lượt dầu mới. Bộ đếm và thời hạn được lưu theo nhân vật.
- Không hiển thị dòng trạng thái **Đã tắt** trong menu Hỗ trợ UP.

Chức năng chạy khi đang auto đánh quái tại map UP, tạm nghỉ khi GoBack, bán đồ, cường hóa, chết hoặc mở menu. GoBack phụ trách đường đi; chức năng khu 2 phụ trách chọn khu để hai chức năng không đổi khu qua lại.

Khi bật **UP khu 2**, GoBack đưa nhân vật về map và tọa độ đã lưu, còn hỗ trợ UP quản lý khu 2 và khu dự phòng. GoBack không kéo nhân vật về khu cũ sau khi đổi khu. Khu GoBack đã lưu vẫn được giữ và dùng lại khi tắt **UP khu 2**. Khi GoBack tắt, map đã lưu không chặn hỗ trợ UP tại map hiện tại. Bật **UP khu 2** tự đóng menu và hiển thị trạng thái; cần bật auto đánh quái. Các mục cấu hình khác cần đóng menu để auto thực hiện.

Bật lại lửa/dầu/sách sau **Tắt tất cả** sẽ chạy bình thường khi đủ điều kiện. Lỗi trả phí khu 2 chỉ tạm dừng chuyển khu, không khóa các vật phẩm. Sau khi dùng vật phẩm không được xác nhận, bật lại mục đó để thử lại ngay; lần dùng thành công vẫn giữ chu kỳ để tránh dùng lãng phí. Cấu hình cũ được chuyển sang thời hạn 1 giờ/2 giờ. Tự nhận diện lại vật phẩm không xóa thời gian chờ hoặc số bình dầu đã châm.

Không gửi lặp yêu cầu trả phí khi kết quả chưa rõ. Menu lạ được giữ để người dùng xử lý; trạng thái hiển thị ở bảng góc trái. Vật phẩm không có cập nhật số lượng sau 15 giây sẽ tự tắt mục tương ứng, không dùng liên tiếp.

Đã kiểm thử bằng phản hồi server mô phỏng; chưa kiểm thử trên tài khoản/server thật.
