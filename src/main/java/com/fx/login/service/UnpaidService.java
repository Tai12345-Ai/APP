package com.fx.login.service;

import javax.persistence.EntityNotFoundException;

import com.fx.login.model.UnpaidEntity;
import com.fx.login.model.User;
import com.fx.login.repo.UnpaidRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UnpaidService {
    @Autowired
    private UnpaidRepo unpaidRepository;

    @Autowired
    private SessionService sessionService;

    public UnpaidService(UnpaidRepo unpaidRepository) {
        this.unpaidRepository = unpaidRepository;
    }

    // ====================== ADMIN METHODS ======================

    // Lấy danh sách tất cả khoản phí (chỉ admin)
    public List<UnpaidEntity> findAll() {
        User currentUser = sessionService.getCurrentUser();
        if (currentUser == null) {
            // Cho phép hệ thống (auto-generation) gọi method này
            return unpaidRepository.findAll();
        }

        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền xem tất cả khoản phí");
        }
        return unpaidRepository.findAll();
    }

    // Tạo mới (chỉ admin hoặc hệ thống)
    public UnpaidEntity createUnpaid(UnpaidEntity unpaid) {
        // Cho phép tạo nếu là admin hoặc không có user (hệ thống tự động)
        User currentUser = sessionService.getCurrentUser();
        if (currentUser != null && !sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền tạo khoản phí");
        }
        return unpaidRepository.save(unpaid);
    }

    // Cập nhật thông tin (chỉ admin)
    public UnpaidEntity updateUnpaid(Long id, UnpaidEntity unpaidDetails) {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền cập nhật khoản phí");
        }

        UnpaidEntity unpaid = unpaidRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khoản phí"));

        unpaid.setResidentName(unpaidDetails.getResidentName());
        unpaid.setApartmentName(unpaidDetails.getApartmentName());
        unpaid.setTotalPayment(unpaidDetails.getTotalPayment());
        unpaid.setDueDate(unpaidDetails.getDueDate());
        unpaid.setFeeID(unpaidDetails.getFeeID());

        if (unpaidDetails.getStatus() != null) {
            unpaid.setStatus(unpaidDetails.getStatus());
        }

        return unpaidRepository.save(unpaid);
    }

    // Xóa khoản phí (chỉ admin)
    public void deleteUnpaid(Long id) {
        if (!sessionService.isAdmin()) {
            throw new SecurityException("Chỉ admin mới có quyền xóa khoản phí");
        }

        if (!unpaidRepository.existsById(id)) {
            throw new EntityNotFoundException("Không tìm thấy khoản phí");
        }
        unpaidRepository.deleteById(id);
    }

    // ====================== RESIDENT METHODS ======================

    // Lấy khoản phí của resident hiện tại
    public List<UnpaidEntity> findMyUnpaidFees() {
        User currentUser = sessionService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Chưa đăng nhập");
        }

        if (sessionService.isAdmin()) {
            return findAll(); // Admin xem tất cả
        }

        if (sessionService.isResident()) {
            return unpaidRepository.findAll().stream()
                    .filter(unpaid ->
                            currentUser.getResidentFullName().equals(unpaid.getResidentName()) &&
                                    currentUser.getApartmentNumber() != null &&
                                    currentUser.getApartmentNumber().equals(unpaid.getApartmentName())
                    )
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    // ====================== COMMON METHODS ======================

    // Lấy thông tin khoản phí theo ID (có kiểm tra quyền)
    public Optional<UnpaidEntity> findById(Long id) {
        Optional<UnpaidEntity> unpaidOpt = unpaidRepository.findById(id);

        if (unpaidOpt.isEmpty()) {
            return unpaidOpt;
        }

        UnpaidEntity unpaid = unpaidOpt.get();
        User currentUser = sessionService.getCurrentUser();

        // Cho phép hệ thống truy cập
        if (currentUser == null) {
            return unpaidOpt;
        }

        // Admin xem được tất cả
        if (sessionService.isAdmin()) {
            return unpaidOpt;
        }

        // Resident chỉ xem được của mình
        if (sessionService.isResident()) {
            boolean canView = currentUser.getResidentFullName().equals(unpaid.getResidentName()) &&
                    currentUser.getApartmentNumber() != null &&
                    currentUser.getApartmentNumber().equals(unpaid.getApartmentName());

            return canView ? unpaidOpt : Optional.empty();
        }

        return Optional.empty();
    }

    // Phương thức này được gọi từ PaymentService (internal)
    public UnpaidEntity save(UnpaidEntity unpaid) {
        return unpaidRepository.save(unpaid);
    }

    // Tìm các khoản phí theo trạng thái (có phân quyền)
    public List<UnpaidEntity> findByStatus(String status) {
        User currentUser = sessionService.getCurrentUser();

        if (currentUser == null || sessionService.isAdmin()) {
            return unpaidRepository.findByStatus(status);
        }

        // Resident chỉ thấy của mình
        return findMyUnpaidFees().stream()
                .filter(unpaid -> status.equals(unpaid.getStatus()))
                .collect(Collectors.toList());
    }

    // Tìm các khoản phí theo feeID (chỉ admin hoặc hệ thống)
    public List<UnpaidEntity> findByFeeID(Long feeID) {
        return unpaidRepository.findByFeeID(feeID);
    }
}