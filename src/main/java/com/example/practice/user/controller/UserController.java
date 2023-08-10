package com.example.practice.user.controller;

import com.example.practice.user.User;
import com.example.practice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<Void> createUser(String username, String password){
        userService.create(username, password);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Long userId){
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/list")
    public ResponseEntity<List<User>> getUsers(){
        return ResponseEntity.ok(userService.getUserList());
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable Long userId, String username){
        return ResponseEntity.ok(userService.updateUsername(userId, username));
    }

    @DeleteMapping("delete/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId){
        userService.deleteUser(userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
