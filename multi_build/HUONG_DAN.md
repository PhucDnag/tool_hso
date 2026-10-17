# HONG PHUC HSO ghép nick — J2ME Loader Android

Các bản x2, x3, x5, x10 được ghép từ `KnightOnline_v403.jar`; mã SHA-256 của bản nguồn và từng file nằm trong `build_report.json`. Bản game đơn không bị thay thế.

## Cài và mở nick

1. Chép **một** file JAR phù hợp vào Android, thêm file đó trong J2ME Loader và chạy. File JAD kèm theo là tùy chọn.
2. Màn hình đầu có danh sách Nick 1 đến Nick N. Chọn Nick 1 và đăng nhập tài khoản thứ nhất.
3. Chạm nút **Nick 1/N** ở góc trên bên phải màn hình game, hoặc nhấn phím **#** của bàn phím giả lập, để trở về danh sách.
4. Chọn Nick 2 và đăng nhập tài khoản khác; làm tương tự với các nick còn lại. Các phiên đã mở tiếp tục chạy trong cùng ứng dụng. Mỗi lần chỉ một nick được hiển thị và nhận thao tác.
5. **Tiếp tục** quay về màn hình nick vừa xem. **Thoát tất cả** có xác nhận và ngắt mọi phiên.

Các ô nick chưa mở không khởi tạo game, không mở kết nối. Bạn có thể chỉ mở 2 nick trong bản x10. Không cần nhập mật khẩu vào bộ quản lý phiên; đăng nhập trực tiếp trong màn hình game của mỗi nick.

## Dữ liệu và cấu hình

- Kết nối, nhân vật, auto và dữ liệu lưu được tách theo từng ô nick.
- Bản đơn và các bản x2/x3/x5/x10 có sẵn dữ liệu map đã khảo sát, xuất từ MicroEmulator ngày 17/10/2026: server có bộ dữ liệu đầy đủ gồm 101 map, 74 map đã khảo sát và 61 khoảng level. Dữ liệu được nạp theo đúng server và chỉ bổ sung phần còn thiếu; dữ liệu hiện có của từng nick được ưu tiên. Không sao chép tài khoản, mật khẩu hoặc vị trí UP cá nhân. Server còn lại có dữ liệu đường đi định dạng cũ; level chưa xác thực được bỏ để học lại.
- Xóa dữ liệu trong một nick chỉ xử lý dữ liệu thuộc nick đó. Gỡ ứng dụng game ghép hoặc xóa dữ liệu từ J2ME Loader có thể ảnh hưởng toàn bộ các nick của bản ghép.
- Các bản x2/x3/x5/x10 là các ứng dụng riêng; không tự đồng bộ dữ liệu giữa chúng.
- Lệnh thoát trong game đưa về danh sách nick; dùng **Thoát tất cả** trong danh sách để kết thúc toàn bộ ứng dụng.

## Tải máy và phiên nền

Tài nguyên ảnh/âm thanh trong JAR chỉ lưu một bản. Trạng thái game và bộ nhớ khi chạy vẫn tăng theo số nick đã mở; x10 không có nghĩa là dùng RAM như một nick.

Vòng cập nhật game giữ mục tiêu 25 lần/giây cho mỗi nick. Phiên không hiển thị vẽ vào một bộ đệm ảnh dùng chung tối đa khoảng 5 lần/giây; giai đoạn lên thuyền vẫn vẽ theo nhịp cập nhật để giữ logic gốc. Tốc độ thực tế phụ thuộc thiết bị và server. Nút **ACC** viền vàng, nền xanh đậm ở mép phải, khoảng 1/3 chiều cao màn hình; kích thước 50 × 30 pixel. Dòng dưới hiển thị ô hiện tại/tổng số ô. Bấm để mở danh sách nick.

Nên thử x2 trước, tăng dần số nick nếu máy vẫn ổn định. Để ứng dụng ở phía trước khi cần các nick tiếp tục hoạt động; Android hoặc trình giả lập có thể tạm dừng ứng dụng khi chuyển sang ứng dụng khác hay tắt màn hình. Không cam kết chạy nền qua cơ chế tiết kiệm pin của Android.

## Đã kiểm tra và giới hạn

- Kiểm tra cấu trúc ZIP, lớp Java của đủ 2/3/5/10 phiên và xác minh bytecode với JVM.
- Kiểm tra singleton kết nối riêng và ghi/đọc dữ liệu RMS cùng khóa nhưng khác nội dung ở từng nick.
- Khởi động đủ số phiên trên MicroEmulator ở chế độ không giao diện, chặn toàn bộ mạng; kiểm tra tick nền, đổi nick, quyền điều khiển màn hình và dừng vòng chạy.
- **Chưa thử trên thiết bị Android/J2ME Loader thật, chưa xác minh nhiều tài khoản đăng nhập server đồng thời, và chưa đo RAM/CPU/pin trên điện thoại.** Đây là bản ghép cần thử thực tế, không phải xác nhận tương thích trên mọi máy.
- Nếu một ô báo lỗi khởi động/chạy, các ô khác vẫn độc lập. Thoát toàn bộ rồi mở lại để thử lại ô lỗi; không tự mở lặp liên tục.

Thiết kế màn hình và RMS đã đối chiếu mã nguồn J2ME Loader:
[Display](https://github.com/nikita36078/J2ME-Loader/blob/master/app/src/main/java/javax/microedition/lcdui/Display.java),
[AndroidRecordStoreManager](https://github.com/nikita36078/J2ME-Loader/blob/master/app/src/main/java/javax/microedition/rms/impl/AndroidRecordStoreManager.java).
