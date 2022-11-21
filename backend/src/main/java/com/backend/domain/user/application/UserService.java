package com.backend.domain.user.application;

import com.backend.domain.refreshToken.dao.RefreshTokenRepository;
import com.backend.domain.user.dao.UserRepository;
import com.backend.domain.user.domain.Address;
import com.backend.domain.user.domain.User;
import com.backend.domain.user.dto.TestUserResponseDto;
import com.backend.domain.user.dto.UserLoginResponseDto;
import com.backend.global.config.auth.userdetails.CustomUserDetails;
import com.backend.global.error.BusinessLogicException;
import com.backend.global.error.ExceptionCode;
import com.backend.global.utils.jwt.JwtTokenizer;
import com.google.gson.JsonObject;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenizer jwtTokenizer;
    private final RefreshTokenRepository refreshTokenRepository;

    private Long guestId = 1L;
    private Long adminId = 1L;

    public User getLoginUser() { //로그인된 유저가 옳바른 지 확인하고 정보 가져옴
        return findUser(getUserByToken());
    }

    private User findUser(User user) {// 아래 getUserByToken 쓸거임
        return findVerifiedUser(user.getUserId());
    }

    public User findVerifiedUser(long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        User findUser = optionalUser.orElseThrow(() ->
                new BusinessLogicException(ExceptionCode.USER_NOT_FOUND));

        if (findUser.getUserStatus() == User.UserStatus.USER_NOT_EXIST) {
            throw new BusinessLogicException(ExceptionCode.USER_NOT_FOUND);
        }
        return findUser;
    }

    @Transactional
    public User createUser(User user) {
        // 현재 활동중인 일반 회원가입으로 가입한 유저의 이미 등록된 이메일인지 확인
        verifyExistsEmailByOriginal(user.getEmail());

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);

    }

    private void verifyExistsEmailByOriginal(String email) { // 현재 활동중인 일반 회원가입으로 가입한 유저의 이미 등록된 이메일인지 확인
        Optional<User> user = userRepository.findByEmailAndUserStatusAndSocialLogin(email, User.UserStatus.USER_EXIST, "original");
        if (user.isPresent()) {
            throw new BusinessLogicException(ExceptionCode.USER_NOT_FOUND);
        }
    }

    public void verifyExistUserByEmailAndOriginal(String email) { //현재 활동중인 일반 회원가입으로 가입한 유저중 email 파라미터로 조회
        Optional<User> user = userRepository.findByEmailAndUserStatusAndSocialLogin(email, User.UserStatus.USER_EXIST, "original");
        if (user.isEmpty()) {//DB에 없는 유저거나 이전에 탈퇴한 유저면 예외처리함
            throw new BusinessLogicException(ExceptionCode.USER_NOT_FOUND);
        }
    }

    public User getUserByToken() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CustomUserDetails memberDetails = (CustomUserDetails) principal;

        return memberDetails.getUser();
    }

    @Transactional
    public User updateUser(User user) {
        User findUser = findVerifiedUser(user.getUserId());
//        Optional.ofNullable(user.getUpdatedAt())//업데이트 날짜 수정
//                .ifPresent(userUpdatedAt ->findUser.setUpdatedAt(userUpdatedAt));

        Optional.ofNullable(user.getProfileImage())//유저 프로필이미지 수정
                .ifPresent(findUser::setProfileImage);

        Optional.ofNullable(user.getNickname())//유저 닉네임 수정
                .ifPresent(findUser::setNickname);

        Optional.ofNullable(user.getEmail())//유저 이메일 수정
                .ifPresent(findUser::setEmail);

        Optional.ofNullable(user.getUserStatus())//유저 탈퇴
                .ifPresent(findUser::setUserStatus);

        Optional.ofNullable(user.getUserRole())//유저 권한 수정
                .ifPresent(findUser::setUserRole);

        return findUser;
    }

    public UserLoginResponseDto createAccessToken(String refreshToken, HttpServletResponse response) {
        Map<String, Object> claims = null;
        User user = null;

        // todo 액세스 토큰 발급 로직 수정

        user = getUser(refreshToken);

        String subject = user.getUserId().toString();
        Date expiration = jwtTokenizer.getTokenExpiration(jwtTokenizer.getAccessTokenExpirationMillisecond());
        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getAccessSecretKey());
        String accessToken = "Bearer " + jwtTokenizer.generateAccessToken(claims, subject, expiration, base64EncodedSecretKey);

        response.setHeader("Authorization", accessToken);

        return UserLoginResponseDto.toResponse(user);
    }

    @Transactional
    public void logout(HttpServletResponse response,
                       Long userId) {


        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .path("/")
                .secure(true)
                .sameSite("None")
                .httpOnly(true)
                .build();
        response.setHeader("Set-Cookie", cookie.toString());

        refreshTokenRepository.deleteByKey(userId);
    }

    private User getUser(String refreshToken) {
        Map<String, Object> claims;
        User user;
        try {
            String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getRefreshSecretKey());
            claims = jwtTokenizer.getClaims(refreshToken, base64EncodedSecretKey).getBody();
            Long userId = Long.parseLong(claims.get("userId").toString());
            user = userRepository.findById(userId).get();
        } catch (SignatureException se) {
            throw new JwtException("사용자 인증 실패");
        } catch (ExpiredJwtException ee) {
            throw new JwtException("토큰 기한 만료");
        } catch (Exception e) {
            throw e;
        }
        return user;
    }

    @Transactional
    public TestUserResponseDto signupTestAccount(String userRole) {

//        String randomEmail = createTestAccountEmail();
//        String randomUsername = createTestAccountUsername();

        String testRole = userRole;
        String testUserId = "";

        if (Objects.equals(userRole, "ROLE_USER_TEST")) {
            testRole = "guest";
            testUserId = String.valueOf(guestId);
            hitGuest();
        } else {
            testRole = "admin";
            testUserId = String.valueOf(adminId);
            hitAdmin();
        }

        String guestEmail = testRole + testUserId + "@test.com";
        String guestNickname = testRole + testUserId;

        String randomPassword = createTestAccountPassword();
        List<Address> testAddress = List.of(Address.builder()
                .address("서울특별시 강남구 테헤란로 427")
                .zipCode("16164")
                .build());

        User testUser = User.builder()
                .email(guestEmail)
                .nickname(guestNickname)
                .password(randomPassword)
                .about("안녕하세요. 테스트 계정입니다.")
                .userRole(userRole)
                .profileImage("https://i.ibb.co/7bQQYkX/kisspng-computer-icons-user-profile-avatar-5abcbc2a1f4f51-20180201102408184.png")
                .socialLogin("original")
                .build();

        testUser.encodePassword(passwordEncoder);
        testUser.addAddress(testAddress.get(0));

        userRepository.save(testUser);

        return TestUserResponseDto.builder()
                .email(testUser.getEmail())
                .password(randomPassword)
                .build();
    }

    // 테스트용 계정 비밀번호 생성
    private String createTestAccountPassword() {
        StringBuffer key = new StringBuffer();
        Random rnd = new Random();

        for (int i = 0; i < 8; i++) {
            int index = rnd.nextInt(3); // 0~2 까지 랜덤, rnd 값에 따라서 아래 switch 문이 실행됨

            switch (index) {
                case 0:
                    key.append((char) (rnd.nextInt(26)) + 97);
                    // a~z (ex. 1+97=98 => (char)98 = 'b')
                    break;
                case 1:
                    key.append((char) (rnd.nextInt(26)) + 65);
                    // A~Z
                    break;
                case 2:
                    key.append((rnd.nextInt(10)));
                    // 0~9
                    break;
            }
        }
        return key.toString();
    }

    private void hitGuest() {
        guestId++;
    }

    private void hitAdmin() {
        adminId++;
    }

    // refresh Token parsing
    public String headerTokenGetClaimTest(String refreshToken) {
        JsonObject jsonObject = new JsonObject();

        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getAccessSecretKey());

        Map<String, Object> claims = jwtTokenizer.getClaims(refreshToken, base64EncodedSecretKey).getBody();

        jsonObject.addProperty("email", claims.get("email").toString());
        jsonObject.addProperty("nickname", claims.get("nickname").toString());
        jsonObject.addProperty("imageUrl", claims.get("profileImage").toString());

        return jsonObject.toString();
    }

    public String atkUserInfo(User user) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("email", user.getEmail());
        jsonObject.addProperty("nickname", user.getNickname());
        jsonObject.addProperty("imageUrl", user.getProfileImage());

        return jsonObject.toString();
    }
}
