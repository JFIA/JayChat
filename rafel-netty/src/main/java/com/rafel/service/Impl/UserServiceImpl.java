package com.rafel.service.Impl;

import com.rafel.enums.MsgActionEnum;
import com.rafel.enums.MsgSignFlagEnum;
import com.rafel.enums.SearchFriendsStatusEnum;
import com.rafel.mapper.*;
import com.rafel.netty.websocket.ChatMsg;
import com.rafel.netty.websocket.DataContent;
import com.rafel.netty.websocket.UserChannelRel;
import com.rafel.org.n3r.idworker.Sid;
import com.rafel.pojo.FriendsRequest;
import com.rafel.pojo.MyFriends;
import com.rafel.pojo.Users;
import com.rafel.pojo.vo.FriendRequestVO;
import com.rafel.pojo.vo.MyFriendsVO;
import com.rafel.service.UserService;
import com.rafel.util.*;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class UserServiceImpl implements UserService {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private MyFriendsMapper myFriendsMapper;

    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Autowired
    private ChatMsgMapper chatMsgMapper;

    @Autowired
    private UsersMapperCustom usersMapperCustom;

    @Autowired
    private Sid sid;

    @Autowired
    private QRCodeUtils qrCodeUtils;

    @Autowired
    private FastDFSClient fastDFSClient;

    // 如果其他bean调用这个方法,在其他bean中声明事务,那就用事务.如果其他bean没有声明事务,那就不用事务
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users getUserName(String username) {

        Users user = new Users();

        user.setUsername(username);

        Users users = usersMapper.selectOne(user);

        return users;
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
    @Override
    public int searchFriendByName(String myUserId, String friendName) {

        Users users = getUserName(friendName);

        if (users == null) return SearchFriendsStatusEnum.USER_NOT_EXIST.status;

        if (users.getId().equals(myUserId)) return SearchFriendsStatusEnum.NOT_YOURSELF.status;

        if (getFriendRelation(myUserId, users) != null) return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;

        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {
        // 根据用户名把朋友信息查询出来
        Users friend = getUserName(friendUsername);

        // 1. 查询发送好友请求记录表
        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId", myUserId);
        frc.andEqualTo("acceptUserId", friend.getId());
        FriendsRequest friendRequest = friendsRequestMapper.selectOneByExample(fre);
        if (friendRequest == null) {
            // 2. 如果不是你的好友，并且好友记录没有添加，则新增好友请求记录
            String requestId = sid.nextShort();

            FriendsRequest request = new FriendsRequest();
            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());
            friendsRequestMapper.insert(request);
        }


    }

    @Transactional(propagation = Propagation.SUPPORTS)
    protected MyFriends getFriendRelation(String myUserId, Users users) {

        Example example = new Example(MyFriends.class);
        Example.Criteria criteria = example.createCriteria();

        criteria.andEqualTo("myUserId", myUserId);
        criteria.andEqualTo("myFriendUserId", users.getId());

        MyFriends relation = myFriendsMapper.selectOneByExample(example);

        return relation;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    protected Users selectUserById(String id) {

        return usersMapper.selectByPrimaryKey(id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendRequestVO> getFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId", sendUserId);
        frc.andEqualTo("acceptUserId", acceptUserId);
        friendsRequestMapper.deleteByExample(fre);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {

        saveFriendRelation(sendUserId, acceptUserId);
        saveFriendRelation(acceptUserId, sendUserId);
        deleteFriendRequest(sendUserId, acceptUserId);

        Channel sendChannel = UserChannelRel.get(sendUserId);
        if (sendChannel != null) {
            // 使用websocket主动推送消息到请求发起者，更新他的通讯录列表为最新
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);

            sendChannel.writeAndFlush(
                    new TextWebSocketFrame(
                            JsonUtils.objectToJson(dataContent)));
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    protected void saveFriendRelation(String sendUserId, String acceptUserId) {
        MyFriends myFriends = new MyFriends();
        String recordId = sid.nextShort();
        myFriends.setId(recordId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriends.setMyUserId(sendUserId);
        myFriendsMapper.insert(myFriends);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> getMyFriends(String acceptUserId) {

        List<MyFriendsVO> myFriends = usersMapperCustom.queryMyFriends(acceptUserId);
        return myFriends;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {

        com.rafel.pojo.ChatMsg msgDB = new com.rafel.pojo.ChatMsg();
        String msgId = sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());

        chatMsgMapper.insert(msgDB);

        return msgId;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {

        usersMapperCustom.batchUpdateMsgSigned(msgIdList);

    }
}
