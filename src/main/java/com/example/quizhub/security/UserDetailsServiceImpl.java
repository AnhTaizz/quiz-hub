package com.example.quizhub.security;

// UserDetailsService được định nghĩa trực tiếp trong ApplicationConfig dưới dạng @Bean.
// File này không cần thiết vì logic load user đã được inline trong ApplicationConfig.java
// để giữ code gọn và dễ maintain.
//
// Xem: com.example.quizhub.config.ApplicationConfig#userDetailsService()
