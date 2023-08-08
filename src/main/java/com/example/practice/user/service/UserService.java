package com.example.practice.user.service;

import com.example.practice.user.User;
import com.example.practice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User create(String username, String password){
        return userRepository.save(User.builder()
                .username(username)
                .password(password)
                .build());
    }

    public User getUserById(Long id){
        return userRepository.findById(id).get();
    }

    public User updateUsername(Long id, String username){
        User user = userRepository.findById(id).get();
        user.setUsername(username);
        return userRepository.save(user);
    }

    public void deleteUser(Long id){
        User user = userRepository.findById(id).get();
        userRepository.delete(user);
    }
}
