package com.admin.account;

import com.admin.domain.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static java.nio.file.Paths.get;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @MockBean
    JavaMailSender javaMailSender;

    @DisplayName("인증 메일 확인 - 입력값 오류")
    @Test
    void checkEmailToken_with_wrong_input() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/check-email-token")
                .param("token", "wrong-token-value")
                .param("email", "email@email.com")
                ).andExpect(status().isOk())
                .andExpect(unauthenticated())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("account/checked-email"));
    }

    @DisplayName("인증 메일 확인 - 입력값 정상")
    @Test
    void checkEmailToken() throws Exception {
        Account account = Account.builder()
                .email("test@email.com")
                .password("12345678")
                .nickname("홍길동")
                .build();
        Account newAccount = accountRepository.save(account);
        newAccount.generateEmailCheckToken();
        mockMvc.perform(MockMvcRequestBuilders.get("/check-email-token")
                        .param("token", newAccount.getEmailCheckToken())
                        .param("email", "test@email.com")
                ).andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeExists("nickname"))
                .andExpect(model().attributeExists("numberOfUser"))
                .andExpect(authenticated())
                .andExpect(view().name("account/checked-email"));
    }

    @Test
    @DisplayName("회원 가입을 하면 보여지는 테스트")
    void signUpForm() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/sign-up"))
                .andExpect(status().isOk())
                .andExpect(unauthenticated())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().attributeExists("signUpForm"));
    }

    @Test
    @DisplayName("회원 가입 처리 - 입력값 오류")
    void signUpSubmit_with_wrong_input() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/sign-up")
                .param("nickname", "nickname")
                .param("email", "wrong format")
                .param("password", "wrong")
                 .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(unauthenticated())
                .andExpect(view().name("account/sign-up"));
    }

    @Test
    @DisplayName("회원 가입 처리 - 입력값 정상")
    void signUpSubmit_with_correct_input() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/sign-up")
                        .param("nickname", "nickname")
                        .param("email", "correct@email.com")
                        .param("password", "correct-password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated())
                .andExpect(authenticated().withUsername("nickname"))
                .andExpect(view().name( "redirect:/"));

        Account account = accountRepository.findByEmail("correct@email.com");
        assertNotNull(account);
        assertNotEquals(account.getPassword(), "correct-password");
        assertTrue(accountRepository.existsByEmail("correct@email.com"));
        then(javaMailSender).should().send(any(SimpleMailMessage.class));
    }

}