<template>
  <div class="app-shell">
    <Header />
    <div class="app-body">
      <Sidebar class="app-sidebar" />
      <main class="app-main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Header from '@/components/common/Header.vue'
import Sidebar from '@/components/common/Sidebar.vue'
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 0%, rgba(191, 219, 254, 0.42), transparent 32rem),
    linear-gradient(135deg, #f8fafc 0%, #eff6ff 52%, #ecfeff 100%);
}

.app-body {
  display: flex;
  align-items: stretch;
}

.app-sidebar {
  width: 256px;
  flex-shrink: 0;
}

.app-main {
  flex: 1;
  min-width: 0;
  padding: 24px;
}

@media (max-width: 960px) {
  .app-body {
    display: block;
  }

  .app-sidebar {
    display: none;
  }

  .app-main {
    padding: 14px;
  }
}
</style>
