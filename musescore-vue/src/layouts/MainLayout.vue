<template>
  <div class="min-h-screen bg-gray-50">
    <Header />
    <div class="flex">
      <Sidebar class="w-64" />
      <main class="flex-1 p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Header from '@/components/common/Header.vue'
import Sidebar from '@/components/common/Sidebar.vue'
</script>