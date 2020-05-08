package com.rafel.service;

import com.rafel.netty.websocket.ChatMsg;
import com.rafel.pojo.Users;
import com.rafel.pojo.vo.FriendRequestVO;
import com.rafel.pojo.vo.MyFriendsVO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserService {

    Users getUserName(String username);

    Users userLogin(Users user) throws Exception;

    Users userRegist(Users user) throws Exception;

    Users updateUserInfo(Users user);

    int searchFriendByName(String myUserId, String friendName);

    void sendFriendRequest(String myUserId, String friendUsername);

    @Transactional(propagation = Propagation.SUPPORTS)
    List<FriendRequestVO> getFriendRequestList(String acceptUserId);

    @Transactional(propagation = Propagation.REQUIRED)
    void deleteFriendRequest(String sendUserId, String acceptUserId);

    void passFriendRequest(String sendUserId, String acceptUserId);

    List<MyFriendsVO> getMyFriends(String acceptUserId);

    String saveMsg(ChatMsg chatMsg);

    void updateMsgSigned(List<String> msgIdList);
}
