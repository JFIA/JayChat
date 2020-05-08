package com.rafel.controller;


import com.rafel.enums.OperatorFriendRequestTypeEnum;
import com.rafel.enums.SearchFriendsStatusEnum;
import com.rafel.pojo.Users;
import com.rafel.pojo.bo.UsersBO;
import com.rafel.pojo.vo.MyFriendsVO;
import com.rafel.pojo.vo.UsersVO;
import com.rafel.service.UserService;
import com.rafel.util.FastDFSClient;
import com.rafel.util.FileUtils;
import com.rafel.util.JSONResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController()
@RequestMapping("u")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;

    @PostMapping("/registOrLogin")
    // 返回自定义响应结构
    public JSONResult registOrLogin(@RequestBody Users user) throws Exception {

        if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword())) {
            return JSONResult.errorMsg("用户名和密码不能为空！");
        }

        Users loginUser = null;

        // 存在登陆，不存在注册
        Users users = userService.getUserName(user.getUsername());

        if (users != null) {

            loginUser = userService.userLogin(user);

            if (loginUser == null) return JSONResult.errorMsg("用户名或密码错误");

        } else {

            loginUser = userService.userRegist(user);

        }

        UsersVO usersVO = new UsersVO();

        BeanUtils.copyProperties(loginUser, usersVO);

        return JSONResult.ok(usersVO);
    }

    /**
     * @Description: 上传用户头像
     */
    @PostMapping("/uploadFaceBase64")
    public JSONResult uploadFaceBase64(@RequestBody UsersBO userBO) throws Exception {

        // 获取前端传过来的base64字符串, 然后转换为文件对象再上传
        String base64Data = userBO.getFaceData();
        String userFacePath = "/resources/" + userBO.getUserId() + "userface64.png";
        FileUtils.base64ToFile(userFacePath, base64Data);

        // 上传文件到fastdfs
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println(url);


        // 获取缩略图的url
        String thump = "_80x80.";
        String arr[] = url.split("\\.");
        String thumpImgUrl = arr[0] + thump + arr[1];

        // 更细用户头像
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setFaceImage(thumpImgUrl);
        user.setFaceImageBig(url);

        Users result = userService.updateUserInfo(user);

        return JSONResult.ok(result);
    }

    @PostMapping("/setNickname")
    public JSONResult setNickname(@RequestBody UsersBO userBO) throws Exception {

        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setNickname(userBO.getNickname());

        Users result = userService.updateUserInfo(user);

        return JSONResult.ok(result);
    }

    /***
     *@Description: 搜索好友，根据账号匹配查询
     */

    @PostMapping("/search")
    public JSONResult searchUser(@RequestParam String myUserId, @RequestParam String friendName) {

        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendName)) {
            return JSONResult.errorMsg("输入错误！");
        }

        // 前置条件 - 1. 搜索的用户如果不存在，返回[无此用户]
        // 前置条件 - 2. 搜索账号是你自己，返回[不能添加自己]
        // 前置条件 - 3. 搜索的朋友已经是你的好友，返回[该用户已经是你的好友]

        int status = userService.searchFriendByName(myUserId, friendName);

        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            Users users = userService.getUserName(friendName);

            UsersVO usersVO = new UsersVO();
            BeanUtils.copyProperties(users, usersVO);

            return JSONResult.ok(usersVO);

        } else {
            String msgByKey = SearchFriendsStatusEnum.getMsgByKey(status);

            return JSONResult.errorMsg(msgByKey);
        }

    }

    @PostMapping("/addFriendRequest")
    public JSONResult addFriendRequest(@RequestParam String myUserId, @RequestParam String friendUsername)
            throws Exception {

        // 0. 判断 myUserId friendUsername 不能为空
        if (StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)) {
            return JSONResult.errorMsg("查询错误！");
        }

        // 前置条件 - 1. 搜索的用户如果不存在，返回[无此用户]
        // 前置条件 - 2. 搜索账号是你自己，返回[不能添加自己]
        // 前置条件 - 3. 搜索的朋友已经是你的好友，返回[该用户已经是你的好友]
        int status = userService.searchFriendByName(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            userService.sendFriendRequest(myUserId, friendUsername);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return JSONResult.errorMsg(errorMsg);
        }

        return JSONResult.ok();
    }

    @PostMapping("/queryFriendRequests")
    public JSONResult queryFriendRequests(@RequestParam String userId) {

        // 0. 判断不能为空
        if (StringUtils.isBlank(userId)) {
            return JSONResult.errorMsg("输入错误！");
        }

        // 1. 查询用户接受到的朋友申请
        return JSONResult.ok(userService.getFriendRequestList(userId));
    }

    /**
     * @Description: 接受方 通过或者忽略朋友请求
     */
    @PostMapping("/operFriendRequest")
    public JSONResult operFriendRequest(@RequestParam String acceptUserId, @RequestParam String sendUserId,
                                        @RequestParam Integer operType) {

        // 0. acceptUserId sendUserId operType 判断不能为空
        if (StringUtils.isBlank(acceptUserId)
                || StringUtils.isBlank(sendUserId)
                || operType == null) {
            return JSONResult.errorMsg("输入有误！");
        }

        // 1. 如果operType 没有对应的枚举值，则直接抛出空错误信息
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))) {
            return JSONResult.errorMsg("操作有误！");
        }

        if (operType == OperatorFriendRequestTypeEnum.IGNORE.type) {
            // 2. 判断如果忽略好友请求，则直接删除好友请求的数据库表记录
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        } else if (operType == OperatorFriendRequestTypeEnum.PASS.type) {
            // 3. 判断如果是通过好友请求，则互相增加好友记录到数据库对应的表
            //	   然后删除好友请求的数据库表记录
            userService.passFriendRequest(sendUserId, acceptUserId);
        }

        // 4. 数据库查询好友列表
        List<MyFriendsVO> myFriends = userService.getMyFriends(acceptUserId);

        return JSONResult.ok(myFriends);
    }

    @PostMapping("/myFriends")
    public JSONResult myFriends(@RequestParam String userId) {
        // 0. userId 判断不能为空
        if (StringUtils.isBlank(userId)) {
            return JSONResult.errorMsg("操作有误！");
        }

        // 1. 数据库查询好友列表
        List<MyFriendsVO> myFriends = userService.getMyFriends(userId);

        return JSONResult.ok(myFriends);
    }


}
