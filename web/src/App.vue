<template>
  <a class="skip-link" href="#page-content">Aller au contenu</a>
  <header class="app-header">
    <nav class="app-nav" aria-label="Navigation principale">
      <RouterLink to="/" class="brand" aria-label="Recallr, accueil"
        ><span class="brand-mark" aria-hidden="true">r</span>recallr<span
          aria-hidden="true"
          style="color: var(--accent)"
          >.</span
        ></RouterLink
      >
      <RouterLink to="/" class="nav-courses">Mes cours ↗</RouterLink>
    </nav>
  </header>
  <div id="page-content" tabindex="-1">
    <RouterView />
  </div>
</template>

<style scoped>
.skip-link {
  position: fixed;
  top: -100px;
  left: 1rem;
  z-index: 50;
  background: white;
  padding: 1rem;
  border-radius: 8px;
}
.skip-link:focus {
  top: 1rem;
}
</style>
