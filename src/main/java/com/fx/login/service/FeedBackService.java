package com.fx.login.service;

import com.fx.login.model.FeedBack;
import com.fx.login.repo.FeedBackRepo;
import javax.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FeedBackService {

    @Autowired
    private FeedBackRepo feedBackRepo;

    public FeedBackService(FeedBackRepo feedBackRepo) {
        this.feedBackRepo = feedBackRepo;
    }

    // Lấy danh sách tất cả góp ý
    public List<FeedBack> findAll() {
        return feedBackRepo.findAll();
    }

    // Lấy thông tin góp ý theo ID
    public Optional<FeedBack> findById(Long id) {
        return feedBackRepo.findById(id);
    }

    // Tạo mới góp ý
    public FeedBack createFeedBack(FeedBack feedBack) {
        return feedBackRepo.save(feedBack);
    }

    // Cập nhật thông tin góp ý
    public FeedBack updateFeedBack(Long id, FeedBack feedBackDetails) {
        FeedBack feedBack = feedBackRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FeedBack not found"));
        feedBack.setTitle(feedBackDetails.getTitle());
        feedBack.setContent(feedBackDetails.getContent());
        feedBack.setCreatedAt(feedBackDetails.getCreatedAt());
        feedBack.setStatus(feedBackDetails.getStatus());
        feedBack.setSender(feedBackDetails.getSender());
        feedBack.setVisibleToUser(feedBackDetails.isVisibleToUser());  // Bổ sung cập nhật trường ẩn/hiện
        return feedBackRepo.save(feedBack);
    }

    // Xóa góp ý
    public void delete(Long id) {
        if (!feedBackRepo.existsById(id)) {
            throw new EntityNotFoundException("FeedBack not found");
        }
        feedBackRepo.deleteById(id);
    }

    // ✅ Cập nhật trạng thái ẩn/hiện dòng đối với người dùng
    public FeedBack updateVisibility(Long id, boolean visibleToUser) {
        FeedBack feedBack = feedBackRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FeedBack not found"));
        feedBack.setVisibleToUser(visibleToUser);
        return feedBackRepo.save(feedBack);
    }
}