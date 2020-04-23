package com.rafel.controller;


import com.rafel.pojo.Users;
import com.rafel.pojo.bo.UsersBO;
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
        boolean usernameIsExist = userService.queryUserNameIsExist(user.getUsername());

        if (usernameIsExist) {

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

}
