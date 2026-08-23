/**
 * Inicializa os comportamentos específicos da Home.
 * Animações decorativas são ignoradas quando o usuário
 * prefere movimento reduzido.
 */
export function initHomePage() {
  const prefersReducedMotion = window.matchMedia(
    "(prefers-reduced-motion: reduce)",
  ).matches;

  if (prefersReducedMotion || typeof gsap === "undefined") {
    return;
  }

  initHeroAnimation();

  // O ScrollTrigger é uma melhoria progressiva:
  // se o plugin não carregar, a Home continua funcionando normalmente.
  if (typeof ScrollTrigger !== "undefined") {
    gsap.registerPlugin(ScrollTrigger);
    initSectionAnimations();
  }
}

function initHeroAnimation() {
  const hero = document.querySelector(".hero");

  if (!hero) {
    return;
  }

  const eyebrow = hero.querySelector(".hero__eyebrow");
  const title = hero.querySelector(".hero__title");
  const description = hero.querySelector(".hero__description");
  const actions = hero.querySelector(".hero__actions");
  const visual = hero.querySelector(".hero__visual");
  const image = hero.querySelector(".hero__image");

  // Impede a execução caso a estrutura esperada do Hero seja alterada.
  if (!eyebrow || !title || !description || !actions || !visual || !image) {
    return;
  }

  const timeline = gsap.timeline({
    defaults: {
      ease: "power3.out",
    },
  });

  timeline
    .from(eyebrow, {
      opacity: 0,
      y: 14,
      duration: 0.45,
    })
    .from(
      title,
      {
        opacity: 0,
        y: 24,
        duration: 0.7,
      },
      "-=0.15",
    )
    .from(
      visual,
      {
        opacity: 0,
        x: 30,
        y: 12,
        scale: 0.97,
        duration: 0.8,
      },
      "<",
    )
    .from(
      description,
      {
        opacity: 0,
        y: 18,
        duration: 0.55,
      },
      "-=0.4",
    )
    .from(
      actions,
      {
        opacity: 0,
        y: 16,
        duration: 0.5,
      },
      "-=0.3",
    );

  // Depois da entrada, mantém apenas um movimento muito sutil na ilustração.
  timeline.eventCallback("onComplete", () => {
    gsap.to(image, {
      y: -6,
      duration: 3,
      repeat: -1,
      yoyo: true,
      ease: "sine.inOut",
    });
  });
}

function initSectionAnimations() {
  animateSection({
    sectionSelector: ".how-it-works",
    headingSelector: ".how-it-works__heading",
    cardSelector: ".step-card",
  });

  animateSection({
    sectionSelector: ".benefits",
    headingSelector: ".benefits__heading",
    cardSelector: ".benefit-card",
  });
}

/**
 * Reutiliza o mesmo padrão de entrada em seções compostas
 * por título e uma sequência de cards.
 */
function animateSection({ sectionSelector, headingSelector, cardSelector }) {
  const section = document.querySelector(sectionSelector);

  if (!section) {
    return;
  }

  const heading = section.querySelector(headingSelector);
  const cards = section.querySelectorAll(cardSelector);

  if (!heading || cards.length === 0) {
    return;
  }

  const timeline = gsap.timeline({
    scrollTrigger: {
      trigger: section,
      start: "top 80%",
      toggleActions: "play none none none",
    },
  });

  timeline
    .from(heading, {
      opacity: 0,
      y: 20,
      duration: 0.5,
      ease: "power2.out",
    })
    .from(
      cards,
      {
        opacity: 0,
        y: 24,
        duration: 0.55,
        stagger: 0.12,
        ease: "power2.out",
      },
      "-=0.2",
    );
}
