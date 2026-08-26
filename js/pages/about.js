export function initAboutPage() {
  const page = document.querySelector(".about-hero");

  if (!page) return;

  if (typeof gsap === "undefined" || typeof ScrollTrigger === "undefined") {
    return;
  }

  const prefersReducedMotion = window.matchMedia(
    "(prefers-reduced-motion: reduce)",
  ).matches;

  if (prefersReducedMotion) return;

  gsap.registerPlugin(ScrollTrigger);

  initAboutHeroAnimation();
  initAboutStoryAnimation();
  initProblemAnimation();
  initAcademicAnimation();
  initPeopleAnimation();
  initPrinciplesAnimation();

  window.addEventListener(
    "load",
    () => {
      ScrollTrigger.refresh();
    },
    { once: true },
  );
}

/* =========================================================
   Hero
   ========================================================= */

function initAboutHeroAnimation() {
  const content = document.querySelector(".about-hero__content");
  const visual = document.querySelector(".about-hero__visual");

  if (content) {
    gsap.fromTo(
      content.children,
      {
        autoAlpha: 0,
        y: 24,
        filter: "blur(8px)",
      },
      {
        autoAlpha: 1,
        y: 0,
        filter: "blur(0px)",
        duration: 0.75,
        stagger: 0.1,
        ease: "power3.out",
      },
    );
  }

  if (visual) {
    gsap.fromTo(
      visual,
      {
        autoAlpha: 0,
        x: 30,
        y: 15,
        scale: 0.97,
        filter: "blur(10px)",
      },
      {
        autoAlpha: 1,
        x: 0,
        y: 0,
        scale: 1,
        filter: "blur(0px)",
        duration: 0.9,
        delay: 0.2,
        ease: "power3.out",
      },
    );
  }
}

/* =========================================================
   Nossa história
   ========================================================= */

function initAboutStoryAnimation() {
  const section = document.querySelector(".about-story");

  if (!section) return;

  const content = section.querySelector(".about-story__content");
  const art = section.querySelector(".about-story__art");

  if (content) {
    createRevealAnimation(content);
  }

  if (art) {
    gsap.fromTo(
      art,
      {
        autoAlpha: 0,
        y: 20,
        scale: 0.97,
        filter: "blur(8px)",
      },
      {
        autoAlpha: 1,
        y: 0,
        scale: 1,
        filter: "blur(0px)",
        duration: 0.8,
        ease: "power3.out",

        scrollTrigger: {
          trigger: art,
          start: "top 86%",
          toggleActions: "play none none reverse",
        },
      },
    );
  }
}

/* =========================================================
   Problema que virou propósito
   ========================================================= */

function initProblemAnimation() {
  const section = document.querySelector(".about-problem");

  if (!section) return;

  const content = section.querySelector(".about-problem__content");
  const visual = section.querySelector(".about-problem__visual");

  if (content) {
    createRevealAnimation(content);
  }

  if (visual) {
    gsap.fromTo(
      visual,
      {
        autoAlpha: 0,
        x: 26,
        y: 14,
        scale: 0.98,
        filter: "blur(8px)",
      },
      {
        autoAlpha: 1,
        x: 0,
        y: 0,
        scale: 1,
        filter: "blur(0px)",
        duration: 0.8,
        ease: "power3.out",

        scrollTrigger: {
          trigger: visual,
          start: "top 86%",
          toggleActions: "play none none reverse",
        },
      },
    );
  }
}

/* =========================================================
   Projeto acadêmico
   ========================================================= */

function initAcademicAnimation() {
  const section = document.querySelector(".about-academic");

  if (!section) return;

  createRevealAnimation(section);
}

/* =========================================================
   Pessoas
   ========================================================= */

function initPeopleAnimation() {
  const section = document.querySelector(".about-people");

  if (!section) return;

  const header = section.querySelector(".about-people__header");
  const author = section.querySelector(".about-person-card--author");

  const collaborators = section.querySelector(".about-collaborators");

  const collaboratorGrid = section.querySelector(".about-collaborators__grid");

  const cards = section.querySelectorAll(".about-collaborator-card");

  if (header) {
    createRevealAnimation(header);
  }

  if (author) {
    gsap.fromTo(
      author,
      {
        autoAlpha: 0,
        y: 22,
        filter: "blur(8px)",
      },
      {
        autoAlpha: 1,
        y: 0,
        filter: "blur(0px)",
        duration: 0.75,
        ease: "power3.out",

        scrollTrigger: {
          trigger: author,
          start: "top 86%",
          toggleActions: "play none none reverse",
        },
      },
    );
  }

  if (collaborators) {
    gsap.fromTo(
      collaborators,
      {
        autoAlpha: 0,
        y: 22,
        filter: "blur(7px)",
      },
      {
        autoAlpha: 1,
        y: 0,
        filter: "blur(0px)",
        duration: 0.7,
        ease: "power3.out",

        scrollTrigger: {
          trigger: collaborators,
          start: "top 86%",
          toggleActions: "play none none reverse",
        },
      },
    );
  }

  if (collaboratorGrid && cards.length) {
    gsap.fromTo(
      cards,
      {
        autoAlpha: 0,
        y: 12,
      },
      {
        autoAlpha: 1,
        y: 0,
        duration: 0.45,
        stagger: 0.07,
        ease: "power2.out",

        scrollTrigger: {
          trigger: collaboratorGrid,
          start: "top 90%",
          toggleActions: "play none none reverse",
        },
      },
    );
  }
}

/* =========================================================
   Princípios
   ========================================================= */

function initPrinciplesAnimation() {
  const section = document.querySelector(".about-structure");

  if (!section) return;

  const intro = section.querySelector(".about-structure__intro");
  const principles = section.querySelectorAll(".about-feature");

  if (intro) {
    createRevealAnimation(intro);
  }

  if (!principles.length) return;

  gsap.fromTo(
    principles,
    {
      autoAlpha: 0,
      y: 16,
      filter: "blur(5px)",
    },
    {
      autoAlpha: 1,
      y: 0,
      filter: "blur(0px)",
      duration: 0.5,
      stagger: 0.06,
      ease: "power3.out",

      scrollTrigger: {
        trigger: section,
        start: "top 78%",
        toggleActions: "play none none reverse",
      },
    },
  );
}

/* =========================================================
   Revelação compartilhada
   ========================================================= */

function createRevealAnimation(element) {
  gsap.fromTo(
    element,
    {
      autoAlpha: 0,
      y: 22,
      filter: "blur(7px)",
    },
    {
      autoAlpha: 1,
      y: 0,
      filter: "blur(0px)",
      duration: 0.7,
      ease: "power3.out",

      scrollTrigger: {
        trigger: element,
        start: "top 86%",
        toggleActions: "play none none reverse",
      },
    },
  );
}
