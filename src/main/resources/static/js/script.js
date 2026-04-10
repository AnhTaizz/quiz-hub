document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    // ─── 1. Header scroll ───────────────────────────────────────
    const header = document.getElementById('header');
    if (header) {
        const onScroll = () => {
            if (window.scrollY > 30) {
                header.classList.add('header-scrolled');
            } else {
                header.classList.remove('header-scrolled');
            }
        };
        window.addEventListener('scroll', onScroll, { passive: true });
        onScroll();
    }

    // ─── 2. Mobile nav toggle ────────────────────────────────────
    const toggleBtn = document.getElementById('mobile-nav-toggle');
    const navMenu   = document.getElementById('nav-menu');
    if (toggleBtn && navMenu) {
        toggleBtn.addEventListener('click', () => {
            navMenu.classList.toggle('nav-open');
            const icon = toggleBtn.querySelector('i');
            icon.classList.toggle('bi-list');
            icon.classList.toggle('bi-x');
        });
        // Close on link click
        navMenu.querySelectorAll('a').forEach(a => {
            a.addEventListener('click', () => {
                navMenu.classList.remove('nav-open');
                const icon = toggleBtn.querySelector('i');
                icon.classList.add('bi-list');
                icon.classList.remove('bi-x');
            });
        });
    }

    // ─── 3. Smooth scroll ───────────────────────────────────────
    document.querySelectorAll('a.scrollto, a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const hash = this.getAttribute('href');
            if (!hash || hash === '#' || !hash.startsWith('#')) return;
            const target = document.querySelector(hash);
            if (!target) return;
            e.preventDefault();
            const offset = header ? header.offsetHeight : 68;
            window.scrollTo({
                top: target.getBoundingClientRect().top + window.pageYOffset - offset,
                behavior: 'smooth'
            });
        });
    });

    // ─── 4. Active nav link ─────────────────────────────────────
    const sections = document.querySelectorAll('section[id]');
    const navLinks  = document.querySelectorAll('.navbar a');
    window.addEventListener('scroll', () => {
        const scrollY = window.pageYOffset;
        sections.forEach(section => {
            const top    = section.offsetTop - (header ? header.offsetHeight : 68) - 60;
            const bottom = top + section.offsetHeight;
            const id     = section.getAttribute('id');
            navLinks.forEach(link => {
                if (link.getAttribute('href') === `#${id}`) {
                    link.classList.toggle('active', scrollY >= top && scrollY < bottom);
                }
            });
        });
    }, { passive: true });

    // ─── 5. Reveal on scroll (IntersectionObserver) ─────────────
    const io = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const delay = parseFloat(entry.target.style.transitionDelay || '0') * 1000;
                setTimeout(() => entry.target.classList.add('visible'), delay);
                io.unobserve(entry.target);
            }
        });
    }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });

    document.querySelectorAll('.reveal').forEach(el => io.observe(el));

    // ─── 6. Scroll-to-top button ────────────────────────────────
    const scrollTop = document.getElementById('scrolltop');
    if (scrollTop) {
        window.addEventListener('scroll', () => {
            scrollTop.style.display = window.scrollY > 400 ? 'block' : 'none';
        }, { passive: true });
    }

    // ─── 7. Khởi tạo Slider, Đồng bộ Nút bấm & Phím tắt ─────────────
    const heroCarouselElement = document.getElementById('heroCarousel');
    const slideButtons = document.querySelectorAll('.slide-btn');

    if (heroCarouselElement) {
        // Khởi tạo Bootstrap Carousel bằng Javascript để ép nó chạy NGAY LẬP TỨC
        const carouselInstance = new bootstrap.Carousel(heroCarouselElement, {
            interval: 3600,     // 4.5 giây đổi slide 1 lần
            ride: 'carousel',   // Kích hoạt auto-play
            pause: 'hover'      // Tạm dừng khi rê chuột vào ảnh (chỉnh 'false' nếu muốn nó chạy liên tục không dừng)
        });

        // Đồng bộ các nút bấm bên trái khi slide tự động chuyển
        if (slideButtons.length > 0) {
            heroCarouselElement.addEventListener('slide.bs.carousel', function (event) {
                slideButtons.forEach(btn => btn.classList.remove('active'));
                if (slideButtons[event.to]) {
                    slideButtons[event.to].classList.add('active');
                }
            });
        }

        // Lắng nghe sự kiện bấm phím Mũi tên Trái/Phải trên toàn bộ trang web
        document.addEventListener('keydown', function(event) {
            // Không trượt slide nếu người dùng đang gõ chữ trong ô input nào đó
            if (event.target.tagName.toLowerCase() === 'input' || event.target.tagName.toLowerCase() === 'textarea') {
                return;
            }

            if (event.key === 'ArrowLeft') {
                carouselInstance.prev(); // Sang trái
            } else if (event.key === 'ArrowRight') {
                carouselInstance.next(); // Sang phải
            }
        });
    }
});
