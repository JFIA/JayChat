package com.rafel.service.Impl;

import com.rafel.mapper.UsersMapper;
import com.rafel.org.n3r.idworker.Sid;
import com.rafel.pojo.Users;
import com.rafel.service.UserService;
import com.rafel.util.FastDFSClient;
import com.rafel.util.FileUtils;
import com.rafel.util.MD5Utils;
import com.rafel.util.QRCodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;

public class UserServiceImpl implements UserService {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private Sid sid;

    @Autowired
    private QRCodeUtils qrCodeUtils;

    @Autowired
    private FastDFSClient fastDFSClient;

    // 如果其他bean调用这个方法,在其他bean中声明事务,那就用事务.如果其他bean没有声明事务,那就不用事务
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUserNameIsExist(String username) {

        Users user = new Users();

        user.setUsername(username);

        Users users = usersMapper.selectOne(user);

        return users != null;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users userLogin(Users user) throws Exception {

        Example example = new Example(Users.class);

        Example.Criteria criteria = example.createCriteria();

        criteria.andEqualTo("username", user.getUsername());
        criteria.andEqualTo("password", MD5Utils.getMD5Str(user.getPassword()));

        Users users = usersMapper.selectOneByExample(example);

        return users;
    }

    // 支持当前事务，如果当前没有事务，就新建一个事务
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users userRegist(Users user) throws Exception {

        String id = sid.nextShort();

        user.setId(id);
        user.setNickname(user.getUsername());
        user.setFaceImage("");
        user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
        // 为每个用户生成一个唯一的二维码
        String qrCodePath = "main/src/resources/" + id + "qrcode.png";
        qrCodeUtils.createQRCode(qrCodePath, "jaychat_qrcode:" + user.getUsername());
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);

        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        user.setQrcode(qrCodeUrl);

        usersMapper.insertSelective(user);

        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUserInfo(Users user) {

        usersMapper.updateByPrimaryKeySelective(user);

        return selectUserById(user.getId());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    Users selectUserById(String id) {

        return usersMapper.selectByPrimaryKey(id);
    }
}
