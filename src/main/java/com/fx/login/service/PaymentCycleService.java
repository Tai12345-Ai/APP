
package com.fx.login.service;

import com.fx.login.model.*;
import com.fx.login.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentCycleService {

    @Autowired
    private PaymentCycleRepository paymentCycleRepository;

    @Autowired
    private UnpaidService unpaidService;

    @Autowired
    private ResidentService residentService;

    @Autowired
    private SessionService sessionService;

    // ====================== BASIC CRUD OPERATIONS ======================

    // Lưu/cập nhật cycle entity (chỉ admin hoặc hệ thống)
    public PaymentCycleEntity save(PaymentCycleEntity cycle) {
        User currentUser = sessionService.getCurrentUser();
        if (currentUser != null && !sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền lưu chu kỳ");
        }
        return paymentCycleRepository.save(cycle);
    }

    // Xóa chu kỳ theo ID (chỉ admin)
    public void delete(Long id) {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền xóa chu kỳ");
        }

        if (!paymentCycleRepository.existsById(id)) {
            throw new EntityNotFoundException("Không tìm thấy chu kỳ với ID: " + id);
        }

        paymentCycleRepository.deleteById(id);
    }

    // Xóa cycle entity (chỉ admin)
    public void delete(PaymentCycleEntity cycle) {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền xóa chu kỳ");
        }
        paymentCycleRepository.delete(cycle);
    }

    // ====================== EXISTING METHODS ======================

    // Tạo chu kỳ thanh toán mới (chỉ admin)
    public PaymentCycleEntity createPaymentCycle(FeeEntity fee, String cycleType, LocalDate nextDue) {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền tạo chu kỳ");
        }

        PaymentCycleEntity cycle = new PaymentCycleEntity();
        cycle.setFee(fee);
        cycle.setCycleType(cycleType);
        cycle.setNextDue(nextDue);
        cycle.setLastGenerated(LocalDate.now());
        cycle.setActive(true);

        return paymentCycleRepository.save(cycle);
    }

    // Tạo khoản thu đột xuất (chỉ admin)
    public void createOneTimePayment(FeeEntity fee, LocalDate dueDate, String description) {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền tạo khoản thu đột xuất");
        }

        if (fee == null) {
            throw new RuntimeException("Phí không được để trống");
        }

        if (dueDate == null) {
            throw new RuntimeException("Ngày đến hạn không được để trống");
        }

        // Lấy danh sách tất cả cư dân
        List<Resident> residents = residentService.findAll();

        if (residents.isEmpty()) {
            throw new RuntimeException("Không tìm thấy cư dân nào trong hệ thống");
        }

        for (Resident resident : residents) {
            UnpaidEntity unpaid = new UnpaidEntity();
            unpaid.setFeeID(fee.getId());
            unpaid.setResidentName(resident.getFullName());
            unpaid.setApartmentName(resident.getApartmentNumber());
            unpaid.setTotalPayment(fee.getAmountDue());
            unpaid.setDueDate(dueDate.toString());
            unpaid.setStatus("PENDING");
            unpaid.setDescription(description != null ? description : "Khoản thu đột xuất - " + fee.getFeeName());

            unpaidService.createUnpaid(unpaid);
        }
    }

    // Tạo khoản thu cho một chu kỳ cụ thể (hệ thống + admin)
    public void generateFeesForCycle(PaymentCycleEntity cycle) {
        // Cho phép hệ thống hoặc admin gọi
        if (sessionService.getCurrentUser() != null && !sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền tạo khoản thu thủ công");
        }

        FeeEntity fee = cycle.getFee();

        // Tạo khoản chưa thanh toán cho tất cả cư dân
        List<Resident> residents = residentService.findAll();

        for (Resident resident : residents) {
            // Kiểm tra xem đã tạo khoản thu cho chu kỳ này chưa
            boolean alreadyExists = checkIfFeeAlreadyGenerated(cycle, resident, LocalDate.now());

            if (!alreadyExists) {
                UnpaidEntity unpaid = new UnpaidEntity();
                unpaid.setFeeID(fee.getId());
                unpaid.setResidentName(resident.getFullName());
                unpaid.setApartmentName(resident.getApartmentNumber());
                unpaid.setTotalPayment(fee.getAmountDue());
                unpaid.setDueDate(cycle.getNextDue().toString());
                unpaid.setStatus("PENDING");
                unpaid.setDescription(createDescription(fee, cycle));

                unpaidService.createUnpaid(unpaid);
            }
        }

        // Cập nhật thông tin chu kỳ
        updateCycleAfterGeneration(cycle);
    }

    private boolean checkIfFeeAlreadyGenerated(PaymentCycleEntity cycle, Resident resident, LocalDate generationDate) {
        // Kiểm tra xem đã tạo khoản thu cho chu kỳ này trong tháng hiện tại chưa
        List<UnpaidEntity> existingFees = unpaidService.findByFeeID(cycle.getFee().getId());

        return existingFees.stream().anyMatch(unpaid ->
                resident.getFullName().equals(unpaid.getResidentName()) &&
                        unpaid.getDueDate() != null &&
                        unpaid.getDueDate().equals(cycle.getNextDue().toString())
        );
    }

    private String createDescription(FeeEntity fee, PaymentCycleEntity cycle) {
        String cycleDisplay = getCycleTypeDisplay(cycle.getCycleType());
        return "Phí " + fee.getFeeName() + " theo chu kỳ " + cycleDisplay;
    }

    private String getCycleTypeDisplay(String cycleType) {
        switch (cycleType) {
            case "MONTHLY": return "hàng tháng";
            case "QUARTERLY": return "hàng quý";
            case "YEARLY": return "hàng năm";
            case "ONE_TIME": return "đột xuất";
            default: return cycleType;
        }
    }

    public void updateCycleAfterGeneration(PaymentCycleEntity cycle) {
        cycle.setLastGenerated(LocalDate.now());

        // Cập nhật ngày đến hạn tiếp theo
        if (!"ONE_TIME".equals(cycle.getCycleType())) {
            LocalDate nextDue = calculateNextDueDate(cycle.getNextDue(), cycle.getCycleType());
            cycle.setNextDue(nextDue);
        } else {
            // Nếu là khoản thu đột xuất, vô hiệu hóa chu kỳ
            cycle.setActive(false);
        }

        paymentCycleRepository.save(cycle);
    }

    private LocalDate calculateNextDueDate(LocalDate currentDue, String cycleType) {
        switch (cycleType) {
            case "MONTHLY":
                return currentDue.plusMonths(1);
            case "QUARTERLY":
                return currentDue.plusMonths(3);
            case "YEARLY":
                return currentDue.plusYears(1);
            default:
                return currentDue;
        }
    }

    // ====================== READ OPERATIONS ======================

    // Lấy tất cả chu kỳ (chỉ admin)
    public List<PaymentCycleEntity> findAll() {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền xem chu kỳ");
        }
        return paymentCycleRepository.findAll();
    }

    // Lấy chu kỳ đang hoạt động (hệ thống + admin)
    public List<PaymentCycleEntity> findActiveCycles() {
        // Cho phép hệ thống (currentUser == null) hoặc admin
        if (sessionService.getCurrentUser() != null && !sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền xem chu kỳ hoạt động");
        }
        return paymentCycleRepository.findByActiveTrue();
    }

    // Tìm chu kỳ theo ID (chỉ admin)
    public Optional<PaymentCycleEntity> findById(Long id) {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền xem chu kỳ");
        }
        return paymentCycleRepository.findById(id);
    }

    // Tìm chu kỳ theo fee ID (chỉ admin)
    public List<PaymentCycleEntity> findByFeeId(Long feeId) {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền xem chu kỳ theo phí");
        }
        return paymentCycleRepository.findByFeeId(feeId);
    }

    // ====================== UTILITY METHODS ======================

    // Xóa chu kỳ (chỉ admin) - phương thức cũ được đổi tên để tránh trùng
    public void deleteCycle(Long id) {
        delete(id); // Gọi method delete() mới
    }

    // Bật/tắt chu kỳ (chỉ admin)
    public void toggleCycleStatus(Long id) {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền thay đổi trạng thái chu kỳ");
        }

        Optional<PaymentCycleEntity> cycleOpt = paymentCycleRepository.findById(id);
        if (cycleOpt.isPresent()) {
            PaymentCycleEntity cycle = cycleOpt.get();
            cycle.setActive(!cycle.isActive());
            paymentCycleRepository.save(cycle);
        } else {
            throw new EntityNotFoundException("Không tìm thấy chu kỳ với ID: " + id);
        }
    }

    // Kiểm tra chu kỳ có tồn tại không
    public boolean existsById(Long id) {
        return paymentCycleRepository.existsById(id);
    }

    // Đếm số chu kỳ đang hoạt động
    public long countActiveCycles() {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền xem thống kê chu kỳ");
        }
        return paymentCycleRepository.findByActiveTrue().size();
    }
}