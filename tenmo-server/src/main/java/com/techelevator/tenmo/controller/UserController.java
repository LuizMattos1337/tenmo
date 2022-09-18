package com.techelevator.tenmo.controller;

import com.techelevator.tenmo.dao.UserDao;
import com.techelevator.tenmo.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
@PreAuthorize("isAuthenticated()")
public class UserController {

    private UserDao userDao;

    public UserController(UserDao userDao){
        this.userDao=userDao;
    }

    @GetMapping(value = "user")
    public List<User> findAllUsernamesAndIds(Principal principal){
        List<User> allUsers = userDao.findAllUsernamesAndIds(principal);
        List<User> otherUsers = new ArrayList<>();
        User currentUser = userDao.findByUsernameWithoutPassword(principal.getName());
        for(User user:allUsers){
            if(!user.getUsername().equals(currentUser.getUsername())){
                otherUsers.add(user);
            }
        }
        return otherUsers;
    }
}
