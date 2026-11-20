package com.bookstore.service;

import com.bookstore.dto.RegisterForm;
import com.bookstore.dto.UserForm;
import com.bookstore.model.Role;
import com.bookstore.model.User;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public UserService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Xác thực đăng nhập người dùng
     */
    @Transactional(readOnly = true)
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Đối với đồ án môn học cơ bản, so khớp trực tiếp mật khẩu (dễ debug và kiểm tra)
            if (user.getPassword().equals(password)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    /**
     * Đăng ký tài khoản khách hàng mới
     */
    public User register(RegisterForm form) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại trong hệ thống");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác");
        }

        User user = new User();
        user.setUsername(form.getUsername());
        user.setPassword(form.getPassword());
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setPhone(form.getPhone());
        user.setAddress(form.getAddress());
        user.setRole(Role.ROLE_CUSTOMER);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllWithOrders();
    }

    @Transactional(readOnly = true)
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsers();
        }
        return userRepository.searchUsers(keyword.trim());
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Thêm mới hoặc cập nhật người dùng từ Quản trị viên
     */
    public User saveUserFromAdmin(UserForm form) {
        String username = form.getUsername().trim();
        String email = form.getEmail().trim();

        User user;
        if (form.getId() == null) {
            // Thêm mới
            if (userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Tên đăng nhập '" + username + "' đã được sử dụng!");
            }
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email '" + email + "' đã được sử dụng!");
            }
            if (form.getPassword() == null || form.getPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Mật khẩu không được để trống khi tạo tài khoản mới!");
            }

            user = new User();
            user.setUsername(username);
            user.setPassword(form.getPassword().trim());
        } else {
            // Cập nhật
            user = userRepository.findById(form.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + form.getId()));

            if (userRepository.existsByUsernameAndIdNot(username, form.getId())) {
                throw new IllegalArgumentException("Tên đăng nhập '" + username + "' đã được sử dụng!");
            }
            if (userRepository.existsByEmailAndIdNot(email, form.getId())) {
                throw new IllegalArgumentException("Email '" + email + "' đã được sử dụng!");
            }

            user.setUsername(username);
            // Nếu có nhập mật khẩu mới thì cập nhật, nếu trống thì giữ nguyên mật khẩu cũ
            if (form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
                user.setPassword(form.getPassword().trim());
            }
        }

        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setPhone(form.getPhone() != null ? form.getPhone().trim() : null);
        user.setAddress(form.getAddress() != null ? form.getAddress().trim() : null);
        user.setRole(form.getRole() != null ? form.getRole() : Role.ROLE_CUSTOMER);

        return userRepository.save(user);
    }

    /**
     * Xóa tài khoản người dùng an toàn
     */
    public void deleteUser(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new IllegalArgumentException("Bạn không thể tự xóa tài khoản của chính mình!");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + id));

        // Kiểm tra xem người dùng có đơn hàng không
        if (orderRepository.existsByUserId(id)) {
            long orderCount = orderRepository.countByUserId(id);
            throw new IllegalArgumentException("Không thể xóa tài khoản này vì đã có " + orderCount + " đơn hàng trong lịch sử giao dịch!");
        }

        userRepository.delete(user);
    }

    /**
     * Đổi vai trò tài khoản (Admin <-> Khách hàng)
     */
    public void changeUserRole(Long id, Role newRole, Long currentAdminId) {
        if (id.equals(currentAdminId) && newRole != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Bạn không thể tự hạ quyền quản trị viên của chính mình!");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + id));

        user.setRole(newRole);
        userRepository.save(user);
    }
}
