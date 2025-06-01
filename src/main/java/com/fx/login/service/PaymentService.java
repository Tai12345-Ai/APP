package com.fx.login.service;

import com.fx.login.model.PaymentEntity;
import com.fx.login.model.UnpaidEntity;
import com.fx.login.model.User;
import com.fx.login.repo.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UnpaidService unpaidService;

    @Autowired
    private SessionService sessionService;

    /**
     * Xử lý thanh toán cho khoản chưa thanh toán
     */
    public PaymentEntity processPayment(Long unpaidId, BigDecimal amount, String paymentMethod) {
        // Kiểm tra quyền
        User currentUser = sessionService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Chưa đăng nhập");
        }

        // Lấy thông tin khoản chưa thanh toán
        Optional<UnpaidEntity> unpaidOpt = unpaidService.findById(unpaidId);
        if (unpaidOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy khoản phí cần thanh toán");
        }

        UnpaidEntity unpaid = unpaidOpt.get();

        // Kiểm tra quyền thanh toán
        if (sessionService.isResident()) {
            // Resident chỉ có thể thanh toán của mình
            boolean canPay = currentUser.getResidentFullName().equals(unpaid.getResidentName()) &&
                    currentUser.getApartmentNumber() != null &&
                    currentUser.getApartmentNumber().equals(unpaid.getApartmentName());

            if (!canPay) {
                throw new SecurityException("Bạn chỉ có thể thanh toán cho căn hộ của mình");
            }
        }
        // Admin có thể thanh toán cho bất kỳ ai

        // Kiểm tra trạng thái
        if ("COMPLETED".equals(unpaid.getStatus())) {
            throw new RuntimeException("Khoản này đã được thanh toán");
        }

        // Kiểm tra số tiền
        BigDecimal totalPayment = new BigDecimal(unpaid.getTotalPayment().replace(",", ""));
        if (amount.compareTo(totalPayment) > 0) {
            throw new RuntimeException("Số tiền thanh toán vượt quá số tiền cần thanh toán");
        }

        // Tạo payment record
        PaymentEntity payment = new PaymentEntity();
        payment.setUnpaid(unpaid);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaidBy(currentUser.getFullname());
        payment.setNotes("Thanh toán bởi: " + currentUser.getFullname());

        // Kiểm tra thanh toán đầy đủ hay một phần
        if (amount.compareTo(totalPayment) == 0) {
            payment.setStatus("COMPLETED");
            unpaid.setStatus("COMPLETED");
        } else {
            payment.setStatus("PARTIAL");
            // Cập nhật số tiền còn lại
            BigDecimal remaining = totalPayment.subtract(amount);
            unpaid.setTotalPayment(remaining.toString());
        }

        // Lưu payment và cập nhật unpaid
        PaymentEntity savedPayment = paymentRepository.save(payment);
        unpaidService.save(unpaid);

        return savedPayment;
    }


// Thêm các method này vào PaymentService

    // Lấy danh sách tất cả thanh toán (chỉ admin)
    public List<PaymentEntity> findAll() {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền xem tất cả thanh toán");
        }
        return paymentRepository.findAll();
    }

    // Lấy lịch sử thanh toán của resident hiện tại
    public List<PaymentEntity> findMyPayments() {
        User currentUser = sessionService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Chưa đăng nhập");
        }

        if (sessionService.isAdmin()) {
            return findAll(); // Admin xem tất cả
        }

        if (sessionService.isResident()) {
            return paymentRepository.findAll().stream()
                    .filter(payment -> {
                        // Kiểm tra xem payment này có thuộc về resident hiện tại không
                        String paidBy = payment.getPaidBy();
                        return paidBy != null && paidBy.equals(currentUser.getResidentFullName());
                    })
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    // Lưu payment entity
    public PaymentEntity save(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

    /**
     * Tìm thanh toán theo ID (có phân quyền)
     */
    public Optional<PaymentEntity> findById(Long id) {
        Optional<PaymentEntity> paymentOpt = paymentRepository.findById(id);

        if (paymentOpt.isEmpty()) {
            return paymentOpt;
        }

        PaymentEntity payment = paymentOpt.get();
        User currentUser = sessionService.getCurrentUser();

        // Admin xem được tất cả
        if (sessionService.isAdmin()) {
            return paymentOpt;
        }

        // Resident chỉ xem được thanh toán của mình
        if (sessionService.isResident() && currentUser != null) {
            UnpaidEntity unpaid = payment.getUnpaid();
            boolean canView = currentUser.getResidentFullName().equals(unpaid.getResidentName()) &&
                    currentUser.getApartmentNumber() != null &&
                    currentUser.getApartmentNumber().equals(unpaid.getApartmentName());

            return canView ? paymentOpt : Optional.empty();
        }

        return Optional.empty();
    }

    /**
     * Tìm thanh toán theo unpaid ID (có phân quyền)
     */
    public List<PaymentEntity> findByUnpaidId(Long unpaidId) {
        // Kiểm tra quyền truy cập unpaid trước
        Optional<UnpaidEntity> unpaidOpt = unpaidService.findById(unpaidId);
        if (unpaidOpt.isEmpty()) {
            return List.of();
        }

        return paymentRepository.findAll().stream()
                .filter(payment -> payment.getUnpaid().getId().equals(unpaidId))
                .collect(Collectors.toList());
    }

    /**
     * Tìm thanh toán theo trạng thái (có phân quyền)
     */
    public List<PaymentEntity> findByStatus(String status) {
        return findAll().stream()
                .filter(payment -> status.equals(payment.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Xóa thanh toán (chỉ admin)
     */
    public void deletePayment(Long id) {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền xóa thanh toán");
        }

        if (!paymentRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy thanh toán");
        }

        paymentRepository.deleteById(id);
    }
}