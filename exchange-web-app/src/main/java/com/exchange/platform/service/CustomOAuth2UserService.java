package com.exchange.platform.service;

import com.exchange.platform.entity.User;
import com.exchange.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 登入嘗試，提供者: {}", registrationId);
        
        // 處理用戶資料
        processOAuth2User(registrationId, oauth2User);
        
        return oauth2User;
    }

    private void processOAuth2User(String provider, OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        String oauth2Id = null;
        String email = null;
        String name = null;
        
        // 根據不同的 OAuth2 提供者解析用戶資訊
        if ("google".equals(provider)) {
            oauth2Id = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
        }
        
        if (oauth2Id == null || email == null) {
            log.error("無法從 OAuth2 提供者獲取必要資訊: {}", provider);
            throw new OAuth2AuthenticationException("無法獲取用戶資訊");
        }
        
        log.info("OAuth2 用戶資訊 - Provider: {}, OAuth2 ID: {}, Email: {}", provider, oauth2Id, email);
        
        // 查找或創建用戶
        Optional<User> existingUser = userRepository.findByOauth2ProviderAndOauth2Id(provider, oauth2Id);
        
        if (existingUser.isEmpty()) {
            // 檢查 email 是否已被註冊（傳統註冊）
            Optional<User> userByEmail = userRepository.findByEmail(email);
            
            if (userByEmail.isPresent()) {
                // 如果用戶已經用傳統方式註冊，則關聯 OAuth2 資訊
                User user = userByEmail.get();
                user.setOauth2Provider(provider);
                user.setOauth2Id(oauth2Id);
                userRepository.save(user);
                log.info("已將 OAuth2 資訊關聯到現有用戶: {}", user.getId());
            } else {
                // 創建新用戶
                User newUser = User.builder()
                    .email(email)
                    .displayName(name != null ? name : email.split("@")[0])
                    .oauth2Provider(provider)
                    .oauth2Id(oauth2Id)
                    .verified(true) // OAuth2 用戶預設已驗證
                    .roles("USER")
                    .build();
                
                userRepository.save(newUser);
                log.info("創建新的 OAuth2 用戶: {}", newUser.getId());
            }
        } else {
            log.info("找到現有的 OAuth2 用戶: {}", existingUser.get().getId());
        }
    }
}
