package com.example.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HomeService {

    public List<String> getMenuItems() {
        List<String> menuItems = new ArrayList<>();
        menuItems.add("홈");
        menuItems.add("소개");
        menuItems.add("연락처");
        return menuItems;
    }

    public String processContactMessage(String name, String email, String message) {
        // 실제 서비스에서는 이메일 전송, 데이터베이스 저장 등의 로직을 처리
        return "메시지가 처리되었습니다: " + name + "님의 문의사항";
    }

    public boolean validateEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}