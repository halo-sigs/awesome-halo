<script setup lang="ts">
import confetti from 'canvas-confetti'
import { onMounted } from 'vue'
import RiShareCircleLine from '~icons/ri/share-circle-line'
import RiCodeBoxLine from '~icons/ri/code-box-line'
import RiBookReadLine from '~icons/ri/book-read-line'
import RiComputerLine from '~icons/ri/computer-line'
import RiArrowRightSLine from '~icons/ri/arrow-right-s-line'

onMounted(() => {
  confetti({
    particleCount: 100,
    spread: 70,
    origin: { y: 0.6, x: 0.58 },
  })
})
</script>

<template>
  <section id="plugin-starter">
    <div class="wrapper">
      <span class="title"> 你已经成功运行起了插件！ </span>
      <span class="message">你可以点击下方文档继续下一步</span>
      <div class="docs">
        <a
          href="https://docs.halo.run/developer-guide/plugin/publish"
          class="docs__box"
          target="_blank"
        >
          <h2 class="docs__box-title"><RiShareCircleLine />发布一个插件</h2>
          <span class="docs__box-message"> 了解如何与我们的社区分享您的扩展。 </span>
          <span class="docs__box-arrow">
            <RiArrowRightSLine />
          </span>
        </a>
        <a
          href="https://docs.halo.run/category/%E5%9F%BA%E7%A1%80"
          class="docs__box"
          target="_blank"
        >
          <h2 class="docs__box-title"><RiComputerLine />基础概览</h2>
          <span class="docs__box-message"> 了解插件的项目结构、生命周期、资源配置等。 </span>
          <span class="docs__box-arrow">
            <RiArrowRightSLine />
          </span>
        </a>
        <a
          href="https://docs.halo.run/developer-guide/plugin/examples/todolist"
          class="docs__box group"
          target="_blank"
        >
          <h2 class="docs__box-title"><RiBookReadLine />示例插件</h2>
          <span class="docs__box-message">帮助你从 0 到 1 完成一个插件。</span>
          <span class="docs__box-arrow">
            <RiArrowRightSLine />
          </span>
        </a>
        <a
          href="https://docs.halo.run/category/api-%E5%8F%82%E8%80%83"
          class="docs__box"
          target="_blank"
        >
          <h2 class="docs__box-title"><RiCodeBoxLine />API 参考</h2>
          <span class="docs__box-message">插件中的 API 列表。</span>
          <span class="docs__box-arrow">
            <RiArrowRightSLine />
          </span>
        </a>
      </div>
    </div>
  </section>
</template>

<style lang="scss" scoped>
#plugin-starter {
  height: 100vh;
  background-color: #f8fafc;
}

.wrapper {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100vh;
  gap: 1.5rem;

  .title {
    font-weight: 700;
    font-size: 1.25rem;
    line-height: 1.75rem;
  }

  .message {
    font-size: 0.875rem;
    line-height: 1.25rem;
    color: #4b5563;
  }

  .docs {
    display: grid;
    grid-template-columns: repeat(1, minmax(0, 1fr));
    gap: 1rem;
    max-width: 48rem;

    .docs__box {
      background-color: #fff;
      border-radius: 0.375rem;
      padding: 0.75rem;
      transition-property: all;
      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
      transition-duration: 300ms;
      cursor: pointer;
      filter: drop-shadow(0 1px 2px rgb(0 0 0 / 0.1)) drop-shadow(0 1px 1px rgb(0 0 0 / 0.06));

      &:hover {
        box-shadow:
          0 0 0 0px #fff,
          0 0 0 1px rgb(59 130 246 / 0.5),
          0 0 #0000;
      }

      .docs__box-title {
        display: flex;
        flex-direction: row;
        font-size: 1.125rem;
        line-height: 1.75rem;
        font-weight: 700;
        margin-bottom: 2rem;
        gap: 0.5rem;
        align-items: center;
      }

      .docs__box-message {
        font-size: 0.875rem;
        line-height: 1.25rem;
        color: #4b5563;
      }

      .docs__box-arrow {
        pointer-events: none;
        position: absolute;
        top: 1rem;
        right: 1rem;
        transition-property: all;
        transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
        transition-duration: 150ms;
        color: #d1d5db;
      }

      &:hover {
        .docs__box-arrow {
          color: #9ca3af;
          transform: translate(00.375rem, 0) rotate(0) skewX(0) skewY(0) scaleX(1) scaleY(1);
        }
      }
    }
  }

  @media (min-width: 640px) {
    .docs {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
}
</style>
