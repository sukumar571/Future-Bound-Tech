/* ========================================
   Future Bound Tech — Main JavaScript
   ======================================== */

document.addEventListener('DOMContentLoaded', function () {
    initNavbarScroll();
    initBackToTop();
    initScrollAnimations();
    initCounterAnimation();
    initThemeToggle();
});

/* ---- Dark / Light Theme Toggle (Bootstrap 5.3) ---- */
function initThemeToggle() {
    const btn = document.getElementById('themeToggle');
    if (!btn) return;

    const syncIcon = function () {
        const isDark = document.documentElement.getAttribute('data-bs-theme') === 'dark';
        btn.innerHTML = isDark
            ? '<i class="bi bi-sun-fill"></i>'
            : '<i class="bi bi-moon-stars-fill"></i>';
    };
    syncIcon();

    btn.addEventListener('click', function () {
        const current = document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'dark' : 'light';
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-bs-theme', next);
        try { localStorage.setItem('fbt-theme', next); } catch (e) { /* ignore */ }
        syncIcon();
    });
}

/* ---- Navbar Scroll Effect ---- */
function initNavbarScroll() {
    const navbar = document.querySelector('.navbar-fbt');
    if (!navbar) return;

    window.addEventListener('scroll', function () {
        if (window.scrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    });
}

/* ---- Back to Top Button ---- */
function initBackToTop() {
    const btn = document.getElementById('backToTop');
    if (!btn) return;

    window.addEventListener('scroll', function () {
        if (window.scrollY > 400) {
            btn.classList.add('visible');
        } else {
            btn.classList.remove('visible');
        }
    });

    btn.addEventListener('click', function () {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });
}

/* ---- Scroll Reveal Animations ---- */
function initScrollAnimations() {
    const elements = document.querySelectorAll('.fade-in-up');
    if (elements.length === 0) return;

    const revealAll = function () {
        elements.forEach(function (el) { el.classList.add('visible'); });
    };

    // Respect users who prefer reduced motion, or browsers without IO support:
    // show everything immediately rather than hiding content.
    const prefersReduced = window.matchMedia
        && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReduced || typeof IntersectionObserver === 'undefined') {
        revealAll();
        return;
    }

    const observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                observer.unobserve(entry.target);
            }
        });
    }, {
        threshold: 0.05,
        rootMargin: '0px 0px -30px 0px'
    });

    elements.forEach(function (el) {
        // Phase 24: anything already inside the initial viewport must not stay
        // hidden waiting for a scroll that may never happen.
        const rect = el.getBoundingClientRect();
        if (rect.top < window.innerHeight && rect.bottom > 0) {
            el.classList.add('visible');
        } else {
            observer.observe(el);
        }
    });

    // Safety net: reveal anything still hidden shortly after load.
    window.addEventListener('load', function () {
        setTimeout(function () {
            elements.forEach(function (el) {
                const r = el.getBoundingClientRect();
                if (r.top < window.innerHeight && r.bottom > 0) {
                    el.classList.add('visible');
                }
            });
        }, 300);
    });
}

/* ---- Counter Animation ---- */
function initCounterAnimation() {
    const counters = document.querySelectorAll('[data-counter]');
    if (counters.length === 0) return;

    const observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                animateCounter(entry.target);
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.5 });

    counters.forEach(function (counter) {
        observer.observe(counter);
    });
}

function animateCounter(element) {
    const target = parseInt(element.getAttribute('data-counter'));
    const suffix = element.getAttribute('data-suffix') || '';
    const duration = 2000;
    const step = Math.ceil(target / (duration / 16));
    let current = 0;

    function update() {
        current += step;
        if (current >= target) {
            current = target;
            element.textContent = current.toLocaleString() + suffix;
            return;
        }
        element.textContent = current.toLocaleString() + suffix;
        requestAnimationFrame(update);
    }

    requestAnimationFrame(update);
}

/* ---- Smooth scroll for anchor links ---- */
document.querySelectorAll('a[href^="#"]').forEach(function (anchor) {
    anchor.addEventListener('click', function (e) {
        const targetId = this.getAttribute('href');
        if (targetId === '#') return;

        const targetEl = document.querySelector(targetId);
        if (targetEl) {
            e.preventDefault();
            targetEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    });
});
