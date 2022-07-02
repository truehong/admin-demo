package com.admin.account;

import com.admin.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final JavaMailSender javaMailSender;

    public void processNewAccount(SignUpForm signUpForm) {
        Account newAccount = this.saveNewAccount(signUpForm);
        newAccount.generateEmailCheckToken();
        this.sendSignupConfirmEmail(newAccount);
    }

    public Account saveNewAccount(SignUpForm signUpForm) {
        Account account = Account
                .builder()
                .email(signUpForm.getEmail())
                .nickname(signUpForm.getNickname())
                .password(signUpForm.getPassword()) // TODO encoding 해야함
                .build();

        Account newAccount = accountRepository.save(account);
        return newAccount;
    }

    public void sendSignupConfirmEmail(Account newAccount) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("어드민, 회원 가입 인증");
        mailMessage.setTo(newAccount.getEmail());
        mailMessage.setText("/check-email-token?token="
                + newAccount.getEmailCheckToken()
                +"&email="
                + newAccount.getEmail());

        javaMailSender.send(mailMessage);
    }

}
