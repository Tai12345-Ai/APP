package com.fx.login.service;

import com.fx.login.model.PendingUser;
import com.fx.login.model.User;
import com.fx.login.repo.PendingUserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class PendingUserService {
    @Autowired
    private PendingUserRepo pendingUserRepo;
    @Autowired
    private ServiceUtil serviceUtil;

    public PendingUser save(PendingUser entity) {
        return serviceUtil.getPendingUserRepo().save(entity);
    }

    public PendingUser update(PendingUser entity) {
        return serviceUtil.getPendingUserRepo().save(entity);
    }

    public void delete(PendingUser entity) {
        serviceUtil.getPendingUserRepo().delete(entity);
    }

    public void delete(Long id) {
        serviceUtil.getPendingUserRepo().deleteById(id);
    }

    public Optional<PendingUser> find(Long id) {
        return serviceUtil.getPendingUserRepo().findById(id);
    }

    public List<PendingUser> findAll() {return serviceUtil.getPendingUserRepo().findAll();}

    public boolean authenticate(String username, String password){
        Optional<PendingUser> user = this.findByEmail(username);
        if(user.isEmpty()){
            return false;
        }else{
            PendingUser u = user.get();
            if(password.equals(u.getPassword())) return true;
            else return false;
        }
    }

    public Optional<PendingUser> findByEmail(String email) {
        return serviceUtil.getPendingUserRepo().findByEmail(email);
    }

    public void deleteInBatch(List<PendingUser> users) {
        serviceUtil.getPendingUserRepo().deleteInBatch(users);
    }
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Transactional
    public boolean deleteUserByEmail(String email) {
        if (pendingUserRepo.existsByEmail(email)) {
            pendingUserRepo.deleteByEmail(email);
            return true;
        }
        return false;
    }
}
