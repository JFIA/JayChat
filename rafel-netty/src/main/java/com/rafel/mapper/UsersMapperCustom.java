package com.rafel.mapper;

import com.rafel.pojo.Users;
import com.rafel.pojo.vo.FriendRequestVO;
import com.rafel.pojo.vo.MyFriendsVO;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface UsersMapperCustom extends Mapper<Users> {
	
	public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);
	
	public List<MyFriendsVO> queryMyFriends(String userId);
	
	public void batchUpdateMsgSigned(List<String> msgIdList);
	
}