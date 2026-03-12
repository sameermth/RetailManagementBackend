package com.retailmanagement.modules.auth.service;

import com.retailmanagement.modules.auth.model.User;

import java.util.List;

public interface UserService {
    
    List<User> getAllUsers();
    
    User getUserById(Long id);
    
    void deactivateUser(Long id);
    
    void activateUser(Long id);
}
