package com.sebatmal.devflow.api.user.service;

import com.sebatmal.devflow.api.user.dto.MeResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.db.user.entity.User;
import com.sebatmal.devflow.db.user.repository.UserRepository;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public MeResponse getMe(final Long userId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_USER));
        return MeResponse.from(user);
    }
}
