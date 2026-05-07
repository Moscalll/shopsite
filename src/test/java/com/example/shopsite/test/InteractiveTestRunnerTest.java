// package com.example.shopsite.test;

// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ActiveProfiles;

// /**
//  * 交互式测试工具运行器
//  * 使用方法：右键此类 -> Run 'InteractiveTestRunnerTest'
//  */
// @SpringBootTest
// @ActiveProfiles("test-cli")
// public class InteractiveTestRunnerTest {
    
//     @Autowired
//     private InteractiveTestRunner runner;
    
//     @Test
//     public void runInteractiveTest() {
//         // 直接运行
//         runner.run();
        
//         // 保持运行（测试会一直运行直到手动停止）
//         try {
//             Thread.sleep(Long.MAX_VALUE);
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//         }
//     }
// }