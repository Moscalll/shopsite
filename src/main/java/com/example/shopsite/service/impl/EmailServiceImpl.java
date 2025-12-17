package com.example.shopsite.service.impl;

import com.example.shopsite.model.Order;
import com.example.shopsite.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Override
    public void sendOrderConfirmationEmail(Order order) {
        // 这里只是模拟发送邮件，实际应用中需要配置邮件服务器
        try {
            String emailContent = buildEmailContent(order);
            logger.info("发送订单确认邮件到: {}", order.getUser().getEmail());
            logger.info("邮件内容:\n{}", emailContent);
            
            // TODO: 实际应用中，这里应该调用邮件服务（如 JavaMailSender）发送邮件
            // 示例：
            // SimpleMailMessage message = new SimpleMailMessage();
            // message.setTo(order.getUser().getEmail());
            // message.setSubject("订单确认 - 订单号: " + order.getId());
            // message.setText(emailContent);
            // mailSender.send(message);
            
        } catch (Exception e) {
            logger.error("发送邮件失败", e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    private String buildEmailContent(Order order) {
        StringBuilder content = new StringBuilder();
        content.append("亲爱的 ").append(order.getUser().getUsername()).append("，\n\n");
        content.append("感谢您的购买！您的订单已确认。\n\n");
        content.append("订单信息：\n");
        content.append("订单号：").append(order.getId()).append("\n");
        content.append("订单时间：").append(order.getOrderDate()).append("\n");
        content.append("订单总额：¥").append(order.getTotalAmount()).append("\n\n");
        
        content.append("商品清单：\n");
        order.getItems().forEach(item -> {
            content.append("- ").append(item.getProduct().getName())
                   .append(" × ").append(item.getQuantity())
                   .append(" = ¥").append(item.getPriceAtOrder().multiply(
                       java.math.BigDecimal.valueOf(item.getQuantity()))).append("\n");
        });
        
        content.append("\n请确认收货信息，如有问题请联系客服。\n\n");
        content.append("祝您购物愉快！\n");
        content.append("KIKA ShopSite");
        
        return content.toString();
    }
}














