<script>
  import { quadIn, cubicIn } from "svelte/easing";
  import { getContext } from "svelte";

  export let centered = false;

  function updateTransitionDirection(newTransitionDirection) {
    if (newTransitionDirection > 0) {
      isFade = false;
      x = 500;
    } else if (newTransitionDirection < 0) {
      isFade = false;
      x = -500;
    } else {
      isFade = true;
    }
  }

  const transitionDirection = getContext("transitionDirection");
  let it,
    x = 0,
    isFade = true;

  const absoluteTransition = (node, { duration = 300, reverse = false }) => {
    updateTransitionDirection($transitionDirection);

    return {
      duration,
      css: (t) => {
        if (isFade) {
          return `
            transform: none;
            opacity: ${cubicIn(t)};
            position: absolute;
          `;
        } else {
          const eased = reverse ? -quadIn(1 - t) : quadIn(1 - t);

          return `
            transform: translateX(${eased * x}px);
            opacity: ${cubicIn(t)};
            position: absolute;
          `;
        }
      },
    };
  };
</script>

<style lang="sass">
    div
      position: relative
      width: 100%

      &.centered
        display: flex
        justify-content: center
        align-items: center
        padding: 0 1rem
</style>

<div
  class:centered
  bind:this={it}
  in:absoluteTransition
  out:absoluteTransition={{ reverse: true }}>
  <slot />
</div>
