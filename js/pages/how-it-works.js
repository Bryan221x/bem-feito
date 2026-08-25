export function initHowItWorksPage() {
  const page = document.querySelector(".how-hero");

  // Este módulo só executa dentro da página "Como funciona".
  if (!page) return;

  if (typeof gsap === "undefined" || typeof ScrollTrigger === "undefined") {
    return;
  }

  // Respeita usuários que preferem reduzir movimentos na interface.
  const prefersReducedMotion = window.matchMedia(
    "(prefers-reduced-motion: reduce)",
  ).matches;

  if (prefersReducedMotion) return;

  gsap.registerPlugin(ScrollTrigger);

  initHeroAnimation();
  initJourneyAnimations();

  /*
   * Atualiza as posições do ScrollTrigger depois que todos os recursos
   * terminarem de carregar, evitando cálculos incorretos de layout.
   */
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

function initHeroAnimation() {
  const content = document.querySelector(".how-hero__content");
  const visual = document.querySelector(".how-hero__visual");

  if (!content) return;

  const contentElements = content.children;

  gsap.fromTo(
    contentElements,
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
   Jornada
   ========================================================= */

function initJourneyAnimations() {
  const journeyItems = document.querySelectorAll(".journey__item");

  if (!journeyItems.length) return;

  const isMobile = window.matchMedia("(max-width: 56rem)").matches;

  journeyItems.forEach((item) => {
    const card = item.querySelector(".journey-card");

    if (!card) return;

    let horizontalMovement = 0;

    if (!isMobile) {
      horizontalMovement = item.classList.contains("journey__item--left")
        ? -30
        : 30;
    }

    gsap.fromTo(
      card,
      {
        autoAlpha: 0,

        x: horizontalMovement,
        y: 26,

        scale: 0.985,

        filter: "blur(9px)",
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
          trigger: card,

          start: "top 86%",

          /*
           * Ao entrar descendo: aparece.
           * Ao subir e sair novamente pelo topo: desaparece suavemente.
           */
          toggleActions: "play none none reverse",
        },
      },
    );
  });
}
