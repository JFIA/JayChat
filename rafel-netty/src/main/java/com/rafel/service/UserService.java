package com.rafel.service;

import com.rafel.pojo.Users;

public interface UserService {

    boolean queryUserNameIsExist(String username);

    Users userLogin(Users user) throws Exception;

    Users userRegist(Users user) throws Exception;

    Users updateUserInfo(Users user);
}
