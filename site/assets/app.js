/* XY Live - 站点交互 */
(function () {
  "use strict";

  var nav = document.querySelector(".nav");
  var toggle = document.querySelector(".nav-toggle");
  var links = document.querySelector(".nav-links");
  var toTop = document.querySelector(".totop");

  /* ---- 移动端菜单 ---- */
  if (toggle && links) {
    toggle.addEventListener("click", function () {
      var open = links.classList.toggle("open");
      toggle.setAttribute("aria-expanded", String(open));
    });
    links.addEventListener("click", function (e) {
      if (e.target.tagName === "A") {
        links.classList.remove("open");
        toggle.setAttribute("aria-expanded", "false");
      }
    });
  }

  /* ---- 滚动状态：导航背景 / 回到顶部 ---- */
  function onScroll() {
    var y = window.scrollY || document.documentElement.scrollTop;
    if (nav) nav.classList.toggle("scrolled", y > 12);
    if (toTop) toTop.classList.toggle("show", y > 620);
    highlightNav(y);
  }

  var ticking = false;
  window.addEventListener(
    "scroll",
    function () {
      if (!ticking) {
        window.requestAnimationFrame(function () {
          onScroll();
          ticking = false;
        });
        ticking = true;
      }
    },
    { passive: true }
  );

  if (toTop) {
    toTop.addEventListener("click", function () {
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
  }

  /* ---- 导航当前区块高亮 ---- */
  var sections = Array.prototype.slice.call(document.querySelectorAll("main section[id]"));
  var navAnchors = Array.prototype.slice.call(document.querySelectorAll(".nav-links a[href^='#']"));

  function highlightNav(y) {
    if (!sections.length || !navAnchors.length) return;
    var offset = y + 140;
    var current = null;
    for (var i = 0; i < sections.length; i++) {
      var top = sections[i].offsetTop;
      if (offset >= top) current = sections[i].id;
    }
    navAnchors.forEach(function (a) {
      var href = a.getAttribute("href").slice(1);
      a.classList.toggle("active", href === current);
    });
  }

  /* ---- 入场动画 ---- */
  var revealEls = Array.prototype.slice.call(document.querySelectorAll(".reveal"));
  if ("IntersectionObserver" in window) {
    var io = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add("in");
            io.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.1, rootMargin: "0px 0px -40px 0px" }
    );
    revealEls.forEach(function (el) {
      io.observe(el);
    });
  } else {
    revealEls.forEach(function (el) {
      el.classList.add("in");
    });
  }

  /* ---- 代码标签页 ---- */
  var tabs = Array.prototype.slice.call(document.querySelectorAll(".tab"));
  var panels = Array.prototype.slice.call(document.querySelectorAll(".code-block"));

  tabs.forEach(function (tab) {
    tab.addEventListener("click", function () {
      var target = tab.getAttribute("data-tab");
      tabs.forEach(function (t) {
        t.classList.toggle("active", t === tab);
      });
      panels.forEach(function (p) {
        p.classList.toggle("active", p.getAttribute("data-panel") === target);
      });
    });
  });

  onScroll();
})();
