package com.kdt.crawler;

import com.microsoft.playwright.CLI;

public class PlaywrightInstaller {
    public static void main(String[] args) {
        try {
            System.out.println("Playwright 브라우저를 설치합니다...");
            CLI.main(new String[]{"install"});
            System.out.println("설치 완료!");
        } catch (Exception e) {
            System.err.println("Playwright 설치 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
