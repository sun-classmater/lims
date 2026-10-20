package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lims.common.BusinessException;
import com.lims.common.ResultCode;
import com.lims.dto.LoginRequest;
import com.lims.entity.User;
import com.lims.mapper.UserMapper;
import com.lims.security.JwtUtil;
import com.lims.vo.LoginResponse;
import com.lims.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Value("${lims.jwt.expiration}")
    private long expiration;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getGh, request.getUsername()));

        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getGh());
        return new LoginResponse(token, "Bearer", expiration);
    }

    public UserInfoVO getCurrentUser(String userId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getGh, userId));

        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        return new UserInfoVO(user.getGh(), user.getName(), user.getUserRoleId(), user.getDeptId());
    }
}
