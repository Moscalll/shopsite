package com.example.shopsite.interceptor;

import com.example.shopsite.model.User;
import com.example.shopsite.repository.MessageRepository;
import com.example.shopsite.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@Component
public class MessageInterceptor implements HandlerInterceptor {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageInterceptor(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                         Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                try {
                    Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
                    if (userOpt.isPresent()) {
                        Long unreadCount = messageRepository.countByUserAndIsReadFalse(userOpt.get());
                        modelAndView.addObject("unreadMessageCount", unreadCount);
                    }
                } catch (Exception e) {
                    // 忽略错误，不影响页面渲染
                }
            } else {
                modelAndView.addObject("unreadMessageCount", 0L);
            }
        }
    }
}